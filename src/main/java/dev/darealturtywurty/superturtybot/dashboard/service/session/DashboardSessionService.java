package dev.darealturtywurty.superturtybot.dashboard.service.session;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardSessionResponse;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardSessionUpsertRequest;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.DashboardSession;

import java.util.ArrayList;
import java.util.Date;

public final class DashboardSessionService {
    public DashboardSessionResponse createOrReplaceSession(DashboardSessionUpsertRequest request) {
        DashboardSession session = new DashboardSession(
                request.getSessionId(),
                request.getUser(),
                request.getGuilds() == null ? new ArrayList<>() : new ArrayList<>(request.getGuilds()),
                new Date(request.getCreatedAtMs()),
                new Date(request.getExpiresAtMs())
        );

        Database.getDatabase().dashboardSessions.replaceOne(
                Filters.eq("sessionId", session.getSessionId()),
                session,
                new ReplaceOptions().upsert(true)
        );

        return toResponse(session);
    }

    public DashboardSessionResponse getSession(String sessionId) {
        DashboardSession session = Database.getDatabase().dashboardSessions.find(Filters.and(
                Filters.eq("sessionId", sessionId),
                Filters.gt("expiresAt", new Date())
        )).first();

        if (session == null)
            return null;

        return toResponse(session);
    }

    public void deleteSession(String sessionId) {
        Database.getDatabase().dashboardSessions.deleteOne(Filters.eq("sessionId", sessionId));
    }

    private static DashboardSessionResponse toResponse(DashboardSession session) {
        return new DashboardSessionResponse(
                session.getSessionId(),
                session.getUser(),
                session.getGuilds(),
                session.getCreatedAt() == null ? 0L : session.getCreatedAt().getTime(),
                session.getExpiresAt() == null ? 0L : session.getExpiresAt().getTime()
        );
    }
}
