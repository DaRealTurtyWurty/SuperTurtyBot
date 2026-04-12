package dev.darealturtywurty.superturtybot.dashboard.service.levelling;

import java.util.List;

public record LevellingSettingsResponse(
        boolean levellingEnabled,
        long levelCooldown,
        int minXp,
        int maxXp,
        int levellingItemChance,
        List<String> disabledLevellingChannelIds,
        boolean disableLevelUpMessages,
        boolean hasLevelUpChannel,
        String levelUpMessageChannelId,
        boolean shouldEmbedLevelUpMessage,
        boolean levelDepletionEnabled,
        List<String> levelRoleMappings,
        List<String> xpBoostedChannelIds,
        List<String> xpBoostedRoleIds,
        int xpBoostPercentage,
        boolean doServerBoostsAffectXP
) {
}
