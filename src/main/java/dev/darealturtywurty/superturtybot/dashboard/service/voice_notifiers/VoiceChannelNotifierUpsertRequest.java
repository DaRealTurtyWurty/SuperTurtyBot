package dev.darealturtywurty.superturtybot.dashboard.service.voice_notifiers;

import java.util.List;

public record VoiceChannelNotifierUpsertRequest(
        String originalVoiceChannelId,
        String voiceChannelId,
        String sendToChannelId,
        List<String> mentionRoleIds,
        String message,
        boolean enabled,
        boolean announcePerJoin,
        long cooldownMs
) {
}
