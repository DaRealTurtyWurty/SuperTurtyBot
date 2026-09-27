package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record GeographyGameSizeConfig(String gameType, int requiredSize) {
    public GeographyGameSizeConfig {
        if (gameType == null || gameType.isBlank())
            throw new IllegalArgumentException("Game type is required");

        if (requiredSize < 1)
            throw new IllegalArgumentException("Required game size must be greater than 0");
    }
}
