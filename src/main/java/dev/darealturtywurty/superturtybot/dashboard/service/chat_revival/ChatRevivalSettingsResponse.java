package dev.darealturtywurty.superturtybot.dashboard.service.chat_revival;

import java.util.List;

public record ChatRevivalSettingsResponse(
        boolean chatRevivalEnabled,
        String chatRevivalChannelId,
        int chatRevivalTime,
        List<String> chatRevivalTypes,
        boolean chatRevivalAllowNsfw
) {
}
