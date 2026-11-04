package dev.darealturtywurty.superturtybot.dashboard.service.nsfw;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NsfwSettingsRequest {
    private boolean artistNsfwFilterEnabled;
}
