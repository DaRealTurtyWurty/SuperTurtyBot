package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MinigameCompletionsState {
    public final Map<String, Set<String>> completionIdsByGameType = new HashMap<>();

    public void add(String gameType, String sourceId) {
        completionIdsByGameType.computeIfAbsent(gameType, ignored -> new HashSet<>()).add(sourceId);
    }
}
