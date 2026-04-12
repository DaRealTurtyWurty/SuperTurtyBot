package dev.darealturtywurty.superturtybot.dashboard.http;

import dev.darealturtywurty.superturtybot.database.pojos.collections.DashboardSession;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DashboardSessionUpsertRequest {
    private String sessionId;
    private DashboardSession.UserSummary user;
    private List<DashboardSession.GuildSummary> guilds = new ArrayList<>();
    private long createdAtMs;
    private long expiresAtMs;
}
