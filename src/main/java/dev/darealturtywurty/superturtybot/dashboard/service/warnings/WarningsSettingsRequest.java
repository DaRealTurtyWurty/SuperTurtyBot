package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

public record WarningsSettingsRequest(
        boolean warningsModeratorOnly,
        float warningXpPercentage,
        float warningEconomyPercentage
) {
}
