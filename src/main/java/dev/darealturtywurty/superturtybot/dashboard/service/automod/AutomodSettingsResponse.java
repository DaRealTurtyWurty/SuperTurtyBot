package dev.darealturtywurty.superturtybot.dashboard.service.automod;

import java.util.List;

public record AutomodSettingsResponse(
        boolean inviteGuardEnabled,
        List<String> inviteGuardWhitelistChannelIds,
        boolean scamDetectionEnabled,
        boolean imageSpamAutoBanEnabled,
        int imageSpamWindowSeconds,
        int imageSpamMinImages,
        int imageSpamNewMemberThresholdHours
) {
}