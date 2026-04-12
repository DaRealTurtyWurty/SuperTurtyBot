package dev.darealturtywurty.superturtybot.dashboard.service.automod;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AutomodSettingsRequest {
    private boolean inviteGuardEnabled;
    private List<String> inviteGuardWhitelistChannelIds = new ArrayList<>();
    private boolean scamDetectionEnabled;
    private boolean imageSpamAutoBanEnabled;
    private int imageSpamWindowSeconds;
    private int imageSpamMinImages;
    private int imageSpamNewMemberThresholdHours;
}
