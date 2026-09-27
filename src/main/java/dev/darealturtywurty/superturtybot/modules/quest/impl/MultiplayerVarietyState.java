package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MultiplayerVarietyState {
    public final Set<String> gameTypes = new HashSet<>();
    public final Set<String> opponentIds = new HashSet<>();
    public final Set<String> wonGameTypes = new HashSet<>();
    public final Map<String, Set<String>> matchIdsByOpponent = new HashMap<>();
}
