package dev.darealturtywurty.superturtybot.modules.privacy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ExternalSharingConfirmation extends ListenerAdapter {
    private static final String APPROVE_PREFIX = "privacy:share:approve:";
    private static final String CANCEL_PREFIX = "privacy:share:cancel:";
    private static final long LIFETIME_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final String BLOCKED_TRANSFER = "This transfer is unavailable because an account involved "
        + "has opted out of optional data processing.";
    public static final ExternalSharingConfirmation INSTANCE = new ExternalSharingConfirmation(Clock.systemUTC());

    private final Cache<String, Pending> pending = CacheBuilder.newBuilder()
        .maximumSize(1_000).expireAfterWrite(5, TimeUnit.MINUTES).build();
    private final Clock clock;

    public ExternalSharingConfirmation(Clock clock) {
        this.clock = clock;
    }

    public MessageCreateData create(long userId, String notice, Consumer<InteractionHook> action) {
        return create(userId, userId, notice, action);
    }

    public MessageCreateData create(long userId, long sourceAuthorId, String notice, Consumer<InteractionHook> action) {
        PrivacyPreferences.requireOptionalData(userId);
        PrivacyPreferences.requireOptionalData(sourceAuthorId);
        String token = UUID.randomUUID().toString();
        pending.put(token, new Pending(userId, sourceAuthorId, clock.millis() + LIFETIME_MILLIS, action));
        return new MessageCreateBuilder()
            .setContent(notice + "\n\nOnly the requester can approve. Approve only content you are allowed "
                + "to share. Do not upload secrets or other people's private information. "
                + "This approval is single-use and expires in five minutes. Cancel to send nothing.")
            .setAllowedMentions(List.of())
            .setComponents(ActionRow.of(Button.primary(APPROVE_PREFIX + token, "Approve this transfer"),
                Button.secondary(CANCEL_PREFIX + token, "Cancel")))
            .build();
    }

    public void forgetUser(long userId) {
        pending.asMap().entrySet().removeIf(entry -> entry.getValue().userId() == userId
            || entry.getValue().sourceAuthorId() == userId);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String component = event.getComponentId();
        boolean approving = component.startsWith(APPROVE_PREFIX);
        if (!approving && !component.startsWith(CANCEL_PREFIX))
            return;
        String token = component.substring((approving ? APPROVE_PREFIX : CANCEL_PREFIX).length());
        Pending request = pending.getIfPresent(token);
        if (request == null || request.expiresAt() <= clock.millis()) {
            pending.invalidate(token);
            event.reply("This approval expired or was already used. Start the request again.")
                .setEphemeral(true).queue();
            return;
        }
        if (request.userId() != event.getUser().getIdLong()) {
            event.reply("Only the requester can approve or cancel this transfer.")
                .setEphemeral(true).queue();
            return;
        }
        if (!pending.asMap().remove(token, request)) {
            event.reply("This approval was already used.").setEphemeral(true).queue();
            return;
        }
        if (!approving || PrivacyPreferences.isOptedOut(request.userId())
            || PrivacyPreferences.isOptedOut(request.sourceAuthorId())) {
            event.editMessage(approving ? BLOCKED_TRANSFER : "Cancelled. Nothing was sent.")
                .setComponents(List.of()).setAllowedMentions(List.of()).queue();
            return;
        }
        event.editMessage("Transfer approved. Processing your request...").setComponents(List.of())
            .setAllowedMentions(List.of()).queue(_ -> Thread.ofVirtual().name("external-sharing").start(() -> {
                try {
                    PrivacyPreferences.requireOptionalData(request.userId());
                    PrivacyPreferences.requireOptionalData(request.sourceAuthorId());
                    request.action().accept(event.getHook());
                } catch (PrivacyOptOutException exception) {
                    event.getHook().editOriginal(BLOCKED_TRANSFER).queue();
                } catch (RuntimeException exception) {
                    Constants.LOGGER.warn("An approved external-sharing request failed ({})",
                        exception.getClass().getSimpleName());
                    event.getHook().editOriginal("The transfer could not finish. Check the source and try again. "
                        + "If an upload already started, the provider may have received it.").queue();
                }
            }));
    }

    public static boolean isUnchanged(Message original, Message current) {
        return original.getIdLong() == current.getIdLong()
            && original.getChannel().getIdLong() == current.getChannel().getIdLong()
            && original.getAuthor().getIdLong() == current.getAuthor().getIdLong()
            && original.getContentRaw().equals(current.getContentRaw())
            && original.getAttachments().stream().map(ExternalSharingConfirmation::attachmentIdentity).toList()
                .equals(
                    current.getAttachments().stream().map(ExternalSharingConfirmation::attachmentIdentity).toList());
    }

    private static List<Object> attachmentIdentity(Message.Attachment attachment) {
        return List.of(attachment.getIdLong(), attachment.getFileName(), attachment.getSize());
    }

    private record Pending(long userId, long sourceAuthorId, long expiresAt, Consumer<InteractionHook> action) {
    }
}
