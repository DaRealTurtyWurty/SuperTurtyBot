package dev.darealturtywurty.superturtybot.dashboard.service.ai;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AiSettingsRequest {
    private boolean aiEnabled;
    private List<String> aiChannelWhitelist;
    private List<String> aiUserBlacklist;
}
