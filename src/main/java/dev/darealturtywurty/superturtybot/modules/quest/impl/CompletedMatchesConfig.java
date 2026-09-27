package dev.darealturtywurty.superturtybot.modules.quest.impl;

import org.jetbrains.annotations.Nullable;

public record CompletedMatchesConfig(int requiredMatches, @Nullable String gameType) {
    public CompletedMatchesConfig {
        if (requiredMatches < 1)
            throw new IllegalArgumentException("Required matches must be greater than 0");
    }
}
