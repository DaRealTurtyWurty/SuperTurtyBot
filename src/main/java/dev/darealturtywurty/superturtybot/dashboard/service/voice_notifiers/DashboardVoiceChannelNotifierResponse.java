package dev.darealturtywurty.superturtybot.dashboard.service.voice_notifiers;

import dev.darealturtywurty.superturtybot.dashboard.service.discord.DashboardGuildInfo;

import java.util.List;

public record DashboardVoiceChannelNotifierResponse(
        DashboardGuildInfo guild,
        List<DashboardVoiceChannelNotifierEntry> entries
) {
}
