package dev.darealturtywurty.superturtybot.dashboard.service.discord.roles;

import java.util.List;

public record DashboardGuildRolesResponse(
        List<DashboardGuildRoleInfo> roles
) {
}
