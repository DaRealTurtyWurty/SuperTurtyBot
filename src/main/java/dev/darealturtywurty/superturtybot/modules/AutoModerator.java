package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage() || event.getAuthor().isSystem())
            return;

        this.inviteGuard.handleMessage(event.getMessage());
        this.imageSpamAutoBanManager.handleMessage(event.getMessage());
        if (this.scamDetectionEnabled) {
            this.scamDomainDetector.handleMessage(event.getMessage());
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.getMessage().isWebhookMessage() ||
                event.getAuthor().isSystem() || event.getMember() == null || event.getMember().isOwner())
            return;

        this.inviteGuard.handleMessage(event.getMessage());
        if (this.scamDetectionEnabled) {
            this.scamDomainDetector.handleMessage(event.getMessage());
        }
    }
}
