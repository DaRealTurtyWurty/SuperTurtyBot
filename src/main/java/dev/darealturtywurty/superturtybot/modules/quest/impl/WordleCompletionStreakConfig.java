package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record WordleCompletionStreakConfig(int requiredDays) {
    public WordleCompletionStreakConfig {
        if (requiredDays < 1 || requiredDays > 7)
            throw new IllegalArgumentException("Required days must be between 1 and 7");
    }
}
