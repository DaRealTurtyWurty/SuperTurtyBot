package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSession {
    private String sessionId;
    private UserSummary user;
    private List<GuildSummary> guilds = new ArrayList<>();
    private Date createdAt;
    private Date expiresAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String id;
        private String username;
        private String globalName;
        private String avatar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuildSummary {
        private String id;
        private String name;
        private String icon;
        private boolean owner;
        private String permissions;
    }
}
