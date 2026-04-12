package dev.darealturtywurty.superturtybot.dashboard.service.threads;

import java.util.List;

public record ThreadSettingsResponse(
        boolean shouldModeratorsJoinThreads,
        List<String> autoThreadChannelIds
) {
}
