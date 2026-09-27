package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.Set;

public record GeographyStreakConfig(int requiredAnswers, Set<String> gameTypes) {
    public GeographyStreakConfig {
        if (requiredAnswers < 1)
            throw new IllegalArgumentException("Required answers must be greater than 0");

        if (gameTypes == null || gameTypes.isEmpty())
            throw new IllegalArgumentException("At least one game type is required");

        gameTypes = Set.copyOf(gameTypes);
    }
}
