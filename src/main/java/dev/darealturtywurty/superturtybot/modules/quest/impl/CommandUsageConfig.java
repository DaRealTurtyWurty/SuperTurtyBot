package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.Set;

public record CommandUsageConfig(
    Set<String> acceptedCommandTypes,
    String requiredCategory,
    int requiredDistinctCommandTypes,
    int requiredDays
) {
    public CommandUsageConfig {
        acceptedCommandTypes = Set.copyOf(acceptedCommandTypes);

        if (requiredDistinctCommandTypes < 0 || requiredDays < 0)
            throw new IllegalArgumentException("Command usage requirements cannot be negative");

        if (requiredDistinctCommandTypes + requiredDays == 0)
            throw new IllegalArgumentException("At least one command usage requirement is required");

        if (!acceptedCommandTypes.isEmpty() && requiredDistinctCommandTypes > acceptedCommandTypes.size())
            throw new IllegalArgumentException("Required command types cannot exceed the accepted command types");
    }
}
