package dev.darealturtywurty.superturtybot.dashboard.service.levelling;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class LevellingSettingsRequest {
    private boolean levellingEnabled;
    private long levelCooldown;
    private int minXp;
    private int maxXp;
    private int levellingItemChance;
    private List<String> disabledLevellingChannelIds = new ArrayList<>();
    private boolean disableLevelUpMessages;
    private boolean hasLevelUpChannel;
    private String levelUpMessageChannelId;
    private boolean shouldEmbedLevelUpMessage;
    private boolean levelDepletionEnabled;
    private List<String> levelRoleMappings = new ArrayList<>();
    private List<String> xpBoostedChannelIds = new ArrayList<>();
    private List<String> xpBoostedRoleIds = new ArrayList<>();
    private int xpBoostPercentage;
    private boolean doServerBoostsAffectXP;
}
