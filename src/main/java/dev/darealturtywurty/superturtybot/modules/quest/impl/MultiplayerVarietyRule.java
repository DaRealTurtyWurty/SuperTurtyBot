package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

import java.util.HashSet;
import java.util.Set;

public final class MultiplayerVarietyRule
    implements
        QuestRule<MultiplayerVarietyConfig, MultiplayerVarietyState> {
    @Override
    public MultiplayerVarietyState createState() {
        return new MultiplayerVarietyState();
    }

    @Override
    public void apply(MultiplayerVarietyConfig config, MultiplayerVarietyState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.MultiplayerMatchCompleted(String sourceId, String gameType, String opponentId, boolean won)))
            return;

        state.gameTypes.add(gameType);
        if (won) {
            state.wonGameTypes.add(gameType);
        }
        if (opponentId != null) {
            state.opponentIds.add(opponentId);
            state.matchIdsByOpponent.computeIfAbsent(opponentId, _ -> new HashSet<>())
                .add(sourceId);
        }
    }

    @Override
    public QuestStatus status(MultiplayerVarietyConfig config, MultiplayerVarietyState state) {
        int gameTypes = Math.min(state.gameTypes.size(), config.requiredGameTypes());
        int opponents = Math.min(state.opponentIds.size(), config.requiredOpponents());
        int sameOpponent = Math.min(
            state.matchIdsByOpponent.values().stream().mapToInt(Set::size).max().orElse(0),
            config.requiredMatchesAgainstOneOpponent());
        int wonGameTypes = Math.min(state.wonGameTypes.size(), config.requiredWonGameTypes());
        int progress = gameTypes + opponents + sameOpponent + wonGameTypes;
        int target = config.requiredGameTypes() + config.requiredOpponents()
            + config.requiredMatchesAgainstOneOpponent() + config.requiredWonGameTypes();
        return new QuestStatus(progress, target, progress >= target);
    }
}
