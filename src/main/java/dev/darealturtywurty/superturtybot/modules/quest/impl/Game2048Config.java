package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record Game2048Config(int requiredTile, boolean mustFinish) {
    public Game2048Config {
        if (requiredTile <= 0)
            throw new IllegalArgumentException("Required tile must be positive.");
    }
}
