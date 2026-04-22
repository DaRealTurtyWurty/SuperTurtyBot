package dev.darealturtywurty.superturtybot.dashboard.service.voice_notifiers;

import java.util.List;

public record DashboardVoiceChannelNotifierEntry(
        String voiceChannelId,
        String voiceChannelName,
        String sendToChannelId,
        String sendToChannelName,
        List<String> mentionRoleIds,
        String message,
        boolean enabled,
        boolean announcePerJoin,
        boolean notifyLeaves,
        long cooldownMs
) {
}
