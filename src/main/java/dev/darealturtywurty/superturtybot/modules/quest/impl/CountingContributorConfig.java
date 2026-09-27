package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record CountingContributorConfig(int requiredCounts, int requiredDays) {
    public CountingContributorConfig {
        if (requiredCounts < 1)
            throw new IllegalArgumentException("Required counts must be greater than 0");

        if (requiredDays < 1)
            throw new IllegalArgumentException("Required days must be greater than 0");
    }
}
