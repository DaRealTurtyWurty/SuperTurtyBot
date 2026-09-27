package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record GeographyFirstTryConfig(String gameType) {
    public GeographyFirstTryConfig {
        if (gameType == null || gameType.isBlank())
            throw new IllegalArgumentException("Game type is required");
    }
}
