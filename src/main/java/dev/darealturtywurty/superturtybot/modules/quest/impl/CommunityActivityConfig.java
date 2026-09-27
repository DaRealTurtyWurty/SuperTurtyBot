package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record CommunityActivityConfig(String activityType, int requiredActions, int requiredDays) {
    public CommunityActivityConfig {
        if (activityType == null || activityType.isBlank())
            throw new IllegalArgumentException("Activity type cannot be blank.");

        if (requiredActions < 1)
            throw new IllegalArgumentException("Required actions must be greater than 0.");

        if (requiredDays < 1)
            throw new IllegalArgumentException("Required days must be greater than 0.");
    }
}
