package dev.darealturtywurty.superturtybot.dashboard.http;

import dev.darealturtywurty.superturtybot.database.pojos.collections.DashboardSession;

import java.util.List;

public record DashboardSessionResponse(
        String sessionId,
        DashboardSession.UserSummary user,
        List<DashboardSession.GuildSummary> guilds,
        long createdAtMs,
        long expiresAtMs
) {
}
