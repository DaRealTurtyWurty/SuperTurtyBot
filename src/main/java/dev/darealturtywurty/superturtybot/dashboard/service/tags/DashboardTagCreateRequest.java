package dev.darealturtywurty.superturtybot.dashboard.service.tags;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DashboardTagCreateRequest {
    private String name;
    private String contentType;
    private String content;
    private String actorUserId;
}
