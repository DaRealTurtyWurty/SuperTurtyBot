package dev.darealturtywurty.superturtybot.dashboard.service.notifiers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardNotifierMutationRequest {
    private String originalTarget;
    private String target;
    private String discordChannelId;
    private String mention;
}
