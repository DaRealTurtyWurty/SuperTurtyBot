package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

public record WarningsSettingsResponse(
        boolean warningsModeratorOnly,
        float warningXpPercentage,
        float warningEconomyPercentage
) {
}
