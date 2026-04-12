package dev.darealturtywurty.superturtybot.dashboard.service.nsfw;

import java.util.List;

public record NsfwSettingsResponse(
        List<String> nsfwChannelIds,
        boolean artistNsfwFilterEnabled
) {
}
