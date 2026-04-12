package dev.darealturtywurty.superturtybot.dashboard.service.discord;

public record DashboardGuildInfo(
        String id,
        String name,
        String iconUrl,
        int memberCount,
        boolean connected
) {
}
