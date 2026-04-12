package dev.darealturtywurty.superturtybot.dashboard.service.discord.member;

import java.util.List;

public record DashboardGuildMembersResponse(
        List<DashboardGuildMemberInfo> members
) {
}
