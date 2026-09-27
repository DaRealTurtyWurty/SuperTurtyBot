package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record CollectablesEarnedConfig(
        int requiredCollectables,
        int requiredCollections,
        int requiredDays,
        String collectionType,
        int minimumRarityOrdinal,
        long maximumResponseTimeMillis
) {
    public CollectablesEarnedConfig(int requiredCollectables) {
        this(requiredCollectables, 0, 0, null, -1, 0);
    }

    public CollectablesEarnedConfig {
        if (requiredCollectables < 0 || requiredCollections < 0 || requiredDays < 0)
            throw new IllegalArgumentException("Collectable requirements cannot be negative");

        if (requiredCollectables + requiredCollections + requiredDays == 0)
            throw new IllegalArgumentException("At least one collectable requirement is required");

        if (maximumResponseTimeMillis < 0)
            throw new IllegalArgumentException("Maximum response time cannot be negative");
    }
}
