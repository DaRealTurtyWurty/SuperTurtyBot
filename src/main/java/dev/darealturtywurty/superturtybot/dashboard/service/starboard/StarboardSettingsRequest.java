package dev.darealturtywurty.superturtybot.dashboard.service.starboard;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class StarboardSettingsRequest {
    private boolean starboardEnabled;
    private String starboardChannelId;
    private int minimumStars;
    private boolean botStarsCount;
    private List<String> showcaseChannelIds = new ArrayList<>();
    private boolean starboardMediaOnly;
    private String starEmoji;
}
