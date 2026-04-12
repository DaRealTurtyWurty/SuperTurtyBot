package dev.darealturtywurty.superturtybot.dashboard.service.welcome;

public record WelcomeSettingsResponse(
        String welcomeChannelId,
        boolean shouldAnnounceJoins,
        boolean shouldAnnounceLeaves
) {
}
