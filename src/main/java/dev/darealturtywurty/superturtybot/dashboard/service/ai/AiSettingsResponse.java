package dev.darealturtywurty.superturtybot.dashboard.service.ai;

import java.util.List;

public record AiSettingsResponse(
        boolean aiEnabled,
        List<String> aiChannelWhitelist,
        List<String> aiUserBlacklist
) {
}
