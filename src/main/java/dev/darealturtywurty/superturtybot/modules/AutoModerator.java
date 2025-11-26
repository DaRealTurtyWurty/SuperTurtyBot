package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class AutoModerator extends ListenerAdapter {
    public static final AutoModerator INSTANCE = new AutoModerator();
    private final DiscordInviteGuard inviteGuard = new DiscordInviteGuard();
    private final ImageSpamAutoBanManager imageSpamAutoBanManager = new ImageSpamAutoBanManager();
    private final ScamDomainDetector scamDomainDetector = new ScamDomainDetector();
    private final boolean scamDetectionEnabled;

    private AutoModerator() {
        this.scamDetectionEnabled = !Environment.INSTANCE.isDevelopment();
        if (this.scamDetectionEnabled) {
            this.scamDomainDetector.start();
        } else {
            Constants.LOGGER.info("Skipping scam-domain detector in development environment.");
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (shouldIgnoreMessage(event, event.getAuthor(), message))
            return;

        this.inviteGuard.handleMessage(message);
        this.imageSpamAutoBanManager.handleMessage(message);
        if (this.scamDetectionEnabled) {
            this.scamDomainDetector.handleMessage(message);
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        Message message = event.getMessage();
        if (shouldIgnoreMessage(event, event.getAuthor(), message))
            return;

        this.inviteGuard.handleMessage(message);
        if (this.scamDetectionEnabled) {
            this.scamDomainDetector.handleMessage(message);
        }
    }

    private boolean shouldIgnoreMessage(GenericMessageEvent event, User author, Message message) {
        return !event.isFromGuild() || author.isBot() || message.isWebhookMessage() || author.isSystem();
    }
}
