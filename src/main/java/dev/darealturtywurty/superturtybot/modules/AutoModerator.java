package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AutoModerator extends ListenerAdapter {
    private static final Pattern INVITE_REGEX = Pattern.compile("(https?://)?(www\\.)?(discord\\.(gg|io|me|li)|discordapp\\.com/invite)/[^ /]+?(?=\b)");
    public static final AutoModerator INSTANCE = new AutoModerator();
    public static final Set<String> SCAM_DOMAINS = ConcurrentHashMap.newKeySet();

    static {
        loadScamDomains();
    }

    private AutoModerator() {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage() || event.getAuthor().isSystem())
            return;

        discordInvites(event.getMessage());
        scamDetection(event.getMessage());
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage() ||
                event.getAuthor().isSystem() || event.getMember() == null || event.getMember().isOwner())
            return;

        discordInvites(event.getMessage());
        scamDetection(event.getMessage());
    }

    private static void loadScamDomains() {
        new Thread(() -> {
            try {
                final URLConnection connection = new URI("https://phish.sinking.yachts/v2/all").toURL().openConnection();
                connection.addRequestProperty("X-Identity", "TurtyBot#8108");
                final InputStream stream = connection.getInputStream();
                final String[] response = Constants.GSON.fromJson(new InputStreamReader(stream), String[].class);
                for (String domain : response) {
                    SCAM_DOMAINS.add(domain);
                    Constants.LOGGER.debug("Scam link added: {}", domain);
                }
            } catch (final IOException | URISyntaxException exception) {
                Constants.LOGGER.error("Failed to initialize scam links!", exception);
            }
        }).start();
    }

    private void discordInvites(Message message) {
        if (INVITE_REGEX.matcher(message.getContentRaw()).find()) {
            message.delete()
                    .queue(success -> message.getChannel().sendMessage(message.getAuthor().getAsMention() + " No invite links allowed!")
                            .queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS)));
        }
    }

    private void scamDetection(Message message) {
        final String content = message.getContentRaw();
        for (final String domain : SCAM_DOMAINS) {
            if (content.contains(domain) && (!content.contains("." + domain) && !content.contains("/" + domain))) {
                message.delete().flatMap(success ->
                        message.getChannel().sendMessage(message.getAuthor().getAsMention() + ", do NOT send scam links! " +
                                "If this was not you, then your account has been compromised. " +
                                "Please make sure to reset your login details to get your token changed. " +
                                "In the future, be more careful what URLs you are opening, always check that its the real one.")).queue();
                break;
            }
        }
    }
}
