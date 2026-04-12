package dev.darealturtywurty.superturtybot.dashboard.service.nsfw;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class NsfwSettingsRequest {
    private List<String> nsfwChannelIds = new ArrayList<>();
    private boolean artistNsfwFilterEnabled;
}
