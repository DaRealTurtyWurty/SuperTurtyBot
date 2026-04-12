package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SuggestionActionRequest {
    private String actorUserId;
    private String action;
    private String reason;
}
