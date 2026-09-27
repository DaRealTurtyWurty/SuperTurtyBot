package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record MultiplayerVarietyConfig(
        int requiredGameTypes,
        int requiredOpponents,
        int requiredMatchesAgainstOneOpponent,
        int requiredWonGameTypes
) {
    public MultiplayerVarietyConfig {
        if (requiredGameTypes < 0 || requiredOpponents < 0
                || requiredMatchesAgainstOneOpponent < 0 || requiredWonGameTypes < 0)
            throw new IllegalArgumentException("Multiplayer requirements cannot be negative.");
        if (requiredGameTypes + requiredOpponents + requiredMatchesAgainstOneOpponent + requiredWonGameTypes == 0)
            throw new IllegalArgumentException("At least one multiplayer requirement is required.");
    }
}
