package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.Set;

public record EconomyActionsConfig(
        Set<String> actionTypes,
        int requiredActions,
        int requiredDays,
        String actionLabel
) {
    public EconomyActionsConfig(Set<String> actionTypes, int requiredActions, int requiredDays) {
        this(actionTypes, requiredActions, requiredDays, "actions");
    }

    public EconomyActionsConfig {
        actionTypes = Set.copyOf(actionTypes);
        if (actionTypes.isEmpty())
            throw new IllegalArgumentException("At least one action type is required");

        if (requiredActions < 1)
            throw new IllegalArgumentException("Required actions must be greater than 0");

        if (requiredDays < 1)
            throw new IllegalArgumentException("Required days must be greater than 0");

        if (actionLabel.isBlank())
            throw new IllegalArgumentException("Action label cannot be blank");
    }
}
