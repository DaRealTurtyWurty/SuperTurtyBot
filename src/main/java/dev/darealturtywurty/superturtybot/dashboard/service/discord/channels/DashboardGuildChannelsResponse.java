package dev.darealturtywurty.superturtybot.dashboard.service.discord.channels;

import java.util.List;

public record DashboardGuildChannelsResponse(
        List<DashboardGuildChannelInfo> channels
) {
}
