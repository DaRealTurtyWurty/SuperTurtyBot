package dev.darealturtywurty.superturtybot.dashboard.service.guild_config;

import dev.darealturtywurty.superturtybot.dashboard.service.discord.DashboardGuildInfo;

import java.util.Map;

public record DashboardGuildConfigSnapshot(
        DashboardGuildInfo guild,
        boolean persisted,
        Map<String, Object> config
) {
}
