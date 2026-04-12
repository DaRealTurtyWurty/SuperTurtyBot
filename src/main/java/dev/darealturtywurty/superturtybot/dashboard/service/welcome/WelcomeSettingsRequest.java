package dev.darealturtywurty.superturtybot.dashboard.service.welcome;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WelcomeSettingsRequest {
    private String welcomeChannelId;
    private boolean shouldAnnounceJoins;
    private boolean shouldAnnounceLeaves;
}
