package dev.darealturtywurty.superturtybot.dashboard.service.discord.channels;

public record DashboardGuildChannelInfo(
        String id,
        String name,
        String type,
        String parentCategoryId,
        int position
) {
}
