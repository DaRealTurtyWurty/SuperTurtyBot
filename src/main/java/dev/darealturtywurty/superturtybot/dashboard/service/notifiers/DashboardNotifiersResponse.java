package dev.darealturtywurty.superturtybot.dashboard.service.notifiers;

import dev.darealturtywurty.superturtybot.dashboard.service.discord.DashboardGuildInfo;

import java.util.List;

public record DashboardNotifiersResponse(
        DashboardGuildInfo guild,
        int totalCount,
        List<DashboardNotifierSection> sections
) {
}
