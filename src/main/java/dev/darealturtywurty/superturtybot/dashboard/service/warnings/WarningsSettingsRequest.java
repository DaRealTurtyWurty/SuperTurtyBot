package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

import java.util.List;

public record WarningsSettingsRequest(
        boolean warningsModeratorOnly,
        int warningExpiryDays,
        float warningXpPercentage,
        float warningEconomyPercentage,
        List<WarningSanctionPayload> sanctions
) {
}
