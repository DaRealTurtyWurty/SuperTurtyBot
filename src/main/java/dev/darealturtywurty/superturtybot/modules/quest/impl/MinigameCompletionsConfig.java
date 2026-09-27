package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.Map;

public record MinigameCompletionsConfig(Map<String, Integer> requiredCompletions) {
    public MinigameCompletionsConfig {
        requiredCompletions = Map.copyOf(requiredCompletions);
        if (requiredCompletions.isEmpty()
                || requiredCompletions.values().stream().anyMatch(required -> required <= 0))
            throw new IllegalArgumentException("Minigame completion requirements must be positive.");
    }
}
