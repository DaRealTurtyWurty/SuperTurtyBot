package io.github.darealturtywurty.superturtybot.modules;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoModerator extends ListenerAdapter {
    private static final Pattern INVITE_REGEX = Pattern
        .compile("(https?:\\/\\/)?(www\\.)?(discord\\.(gg|io|me|li)|discordapp\\.com\\/invite)\\/[^\s\\/]+?(?=\b)");
    public static final AutoModerator INSTANCE = new AutoModerator();
    public static final Set<String> SCAM_DOMAINS = new HashSet<>();
    
    private AutoModerator() {
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage()
            || event.getAuthor().isSystem())
            return;
        
        // amongusDetection(event.getMessage());
        discordInvites(event.getMessage());
        // scamDetection(event.getMessage());
    }
    
    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage()
            || event.getAuthor().isSystem() || event.getMember().isOwner())
            return;
        
        // amongusDetection(event.getMessage());
        discordInvites(event.getMessage());
        // scamDetection(event.getMessage());
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        /*
         * try { final URLConnection connection = new URL("https://phish.sinking.yachts/v2/all").openConnection();
         * connection.addRequestProperty("X-Identity", "TurtyBot#8108"); final InputStream stream =
         * connection.getInputStream(); final JsonArray response = Constants.GSON.fromJson(new
         * InputStreamReader(stream), JsonArray.class); response.forEach(link -> { final String domain =
         * link.getAsString(); SCAM_DOMAINS.add(domain); Constants.LOGGER.info("Scam link added: {}", domain); }); }
         * catch (final IOException exception) { Constants.LOGGER.
         * error("There has been an error accessing: {}\nError Message: {}", "https://phish.sinking.yachts/v2/all",
         * exception.getMessage()); }
         */
    }
    
    // TODO: Fix dumpy URL and also cooldown it
    private void amongusDetection(final Message message) {
        final User user = message.getAuthor();
        final String shortenedText = message.getContentRaw().toLowerCase().trim();
        if (shortenedText.contains(" sus ") || "sus".equals(shortenedText) || shortenedText.contains("amogus")
            || shortenedText.contains("amongus") && !shortenedText.startsWith(Environment.INSTANCE.defaultPrefix())
                && !user.isBot() && !message.isWebhookMessage()) {
            message.reply(Constants.BEAN_DUMPY_URL).queue();
        }
    }
    
    private void discordInvites(Message message) {
        if (INVITE_REGEX.matcher(message.getContentRaw()).find()) {
            message.delete()
                .queue(success -> message.getChannel()
                    .sendMessage(message.getAuthor().getAsMention() + " No invite links allowed!")
                    .queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS)));
        }
    }

    private void scamDetection(Message message) {
        final String content = message.getContentRaw().toLowerCase().trim().replace("https://", "")
            .replace("http://", "").replace("www.", "").replace("/", "");
        final String[] parts = content.split(" ");
        if (parts.length <= 1) {
            for (final String domain : SCAM_DOMAINS) {
                if (content.equals(domain.trim().toLowerCase())) {
                    message.delete()
                        .queue(success -> message.getChannel().sendMessage(message.getAuthor().getAsMention()
                            + ", do NOT send scam links! If this was not you, then your account has been comprimised. "
                            + "Please make sure to reset your login details to get your token changed. "
                            + "For future reference, do NOT click free nitro links, doesn't matter who it is from!")
                            .queue());
                    return;
                }
            }
        }

        for (final String part : parts) {
            if (SCAM_DOMAINS.contains(part)) {
                message.delete().queue(success -> message.getChannel().sendMessage(message.getAuthor().getAsMention()
                    + ", please do not send scam links! If this was not you, then your account has been comprimised. Please make sure to reset your login details to get your token changed, and do not click free nitro links!")
                    .queue());
                return;
            }
        }
    }
}
