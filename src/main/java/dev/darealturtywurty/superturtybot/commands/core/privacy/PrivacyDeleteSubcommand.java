package dev.darealturtywurty.superturtybot.commands.core.privacy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.AIMessageResponder;
import dev.darealturtywurty.superturtybot.modules.LoggingManager;
import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyConfirmation;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyDataService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.UUID;
import java.util.function.LongConsumer;

public final class PrivacyDeleteSubcommand extends SubcommandCommand {
    private static final String DELETE_PREFIX = "privacy:delete:";
    private static final String CANCEL_PREFIX = "privacy:cancel:";
    private final PrivacyConfirmation confirmations = new PrivacyConfirmation();
    private final PrivacyOperations operations;

    public PrivacyDeleteSubcommand(PrivacyOperations operations) {
        super("delete", "Confirm automatic cleanup and request operator review of remaining copies");
        this.operations = operations;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String token = confirmations.create(event.getUser().getIdLong());
        event.reply("**Delete your stored TurtyBot data across all servers?**\n\n"
            + "This permanently removes supported profiles, birthdays, reminders, balances, inventories, "
            + "game progress and related database records. You may lose virtual items and progress. "
            + "Consider using `/privacy export` first.\n\n"
            + "Other people's shared records will have your structured references removed where supported. "
            + "A request containing your user ID is kept while the operator reviews remaining copies, "
            + "including Discord posts, Gists, free-text mentions and active restrictions. "
            + "This does not remove Discord bans or stop future activity creating new records.\n\n"
            + "The confirmation expires in five minutes.")
            .setEphemeral(true).setAllowedMentions(List.of())
            .setComponents(ActionRow.of(Button.danger(DELETE_PREFIX + token, "Delete my data"),
                Button.secondary(CANCEL_PREFIX + token, "Cancel")))
            .queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        boolean deleting = id.startsWith(DELETE_PREFIX);
        if (!deleting && !id.startsWith(CANCEL_PREFIX))
            return;
        String token = id.substring((deleting ? DELETE_PREFIX : CANCEL_PREFIX).length());
        long userId = event.getUser().getIdLong();
        if (!confirmations.consume(token, userId)) {
            event.reply("This confirmation expired, was already used, or belongs to another account. "
                + "Run `/privacy delete` to start again.").setEphemeral(true).queue();
            return;
        }
        if (!deleting) {
            event.editMessage("Deletion cancelled. Your data has not been changed.").setComponents(List.of()).queue();
            return;
        }
        if (!operations.tryBegin(userId)) {
            event.editMessage("A privacy operation is already operations.running. Wait for it to finish, then run "
                + "`/privacy delete` again.").setComponents(List.of()).queue();
            return;
        }
        String requestId = UUID.randomUUID().toString();
        event.editMessage("Processing your deletion request. Reference: `" + requestId + "`.")
            .setComponents(List.of()).queue(message -> Thread.ofVirtual().name("privacy-delete").start(() -> {
                delete(event.getHook(), event.getJDA(), userId, requestId);
            }), error -> operations.finish(userId));
    }

    private void delete(InteractionHook hook, JDA jda, long userId, String requestId) {
        try {
            PrivacyDataService service = PrivacyOperations.service();
            PrivacyDataService.DeletionResult result = service.deleteUser(userId, requestId,
                PrivacyDeleteSubcommand::clearRuntime);
            boolean success = result.failedSteps().isEmpty();
            hook.editOriginal(
                (success ? "Automatic database cleanup completed." : "Some automatic cleanup steps failed.")
                    + "\nChanged or removed **" + result.recordsChanged() + "** database records.\n\n"
                    + "Your request is saved for operator review of remaining data, including Discord/Gist/provider "
                    + "copies, free-text references and active restrictions. Your user ID is retained with the request "
                    + "until it is resolved. This is not confirmation that every copy has been erased.\n\n"
                    + "Reference: `" + requestId + "`\nContact: **turtybot@gmail.com**. "
                    + "Future activity can create new records.")
                .setAllowedMentions(List.of()).queue();
            notifyOperator(jda, userId, requestId, success ? "manual_review" : "partial_failure");
        } catch (Exception exception) {
            Constants.LOGGER.warn("Privacy deletion {} did not finish ({})", requestId,
                exception.getClass().getSimpleName());
            hook.editOriginal("The deletion could not finish. Some data may already have been removed. "
                + "Email **turtybot@gmail.com** with reference `" + requestId + "` for help.")
                .setAllowedMentions(List.of()).queue();
            notifyOperator(jda, userId, requestId, "failed_or_incomplete");
        } finally {
            operations.finish(userId);
        }
    }

    private static void notifyOperator(JDA jda, long userId, String requestId, String status) {
        Constants.LOGGER.info("Privacy request {} requires operator review ({})", requestId, status);
        try {
            Environment.INSTANCE.ownerId().ifPresent(ownerId -> jda.retrieveUserById(ownerId)
                .flatMap(user -> user.openPrivateChannel())
                .flatMap(channel -> channel.sendMessage("Privacy deletion request `" + requestId
                    + "` for Discord user `"
                    + userId + "` needs operator review. Status: `" + status + "`. "
                    + "Review the privacyRequests database collection and the privacy handling instructions in README.md.")
                    .setAllowedMentions(List.of()))
                .queue(message -> {
                }, error -> Constants.LOGGER.warn("Could not deliver privacy request {} notification", requestId)));
        } catch (RuntimeException exception) {
            Constants.LOGGER.warn("Could not start privacy request {} notification", requestId);
        }
    }

    private static void clearRuntime(long userId) {
        boolean failed = false;
        List<LongConsumer> steps = List.of(AIMessageResponder.INSTANCE::forgetUser,
            LoggingManager.INSTANCE::forgetUser, ReminderManager::clearReminders);
        for (LongConsumer step : steps) {
            try {
                step.accept(userId);
            } catch (RuntimeException exception) {
                failed = true;
            }
        }
        if (failed)
            throw new IllegalStateException("A runtime privacy cleanup step failed");
    }
}
