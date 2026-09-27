package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record TriviaStreakConfig(int requiredAnswers, boolean consecutive) {
    public TriviaStreakConfig {
        if (requiredAnswers < 1)
            throw new IllegalArgumentException("Required answers must be greater than 0");
    }
}
