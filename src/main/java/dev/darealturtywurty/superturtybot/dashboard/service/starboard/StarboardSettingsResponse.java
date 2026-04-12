package dev.darealturtywurty.superturtybot.dashboard.service.starboard;

import java.util.List;

public record StarboardSettingsResponse(
        boolean starboardEnabled,
        String starboardChannelId,
        int minimumStars,
        boolean botStarsCount,
        List<String> showcaseChannelIds,
        boolean starboardMediaOnly,
        String starEmoji
) {
}
