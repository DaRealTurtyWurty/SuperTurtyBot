package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class GeographyFirstTryRule implements QuestRule<GeographyFirstTryConfig, GeographyCompletionsState> {
    @Override
    public GeographyCompletionsState createState() {
        return new GeographyCompletionsState();
    }

    @Override
    public void apply(GeographyFirstTryConfig config, GeographyCompletionsState state, QuestEvent event) {
        if (event instanceof QuestEvent.GeographyGameCompleted(
                String sourceId, String gameType, _, int attempts
        ) && config.gameType().equals(gameType) && attempts == 1) {
            state.qualifyingGameIds.add(sourceId);
        }
    }

    @Override
    public QuestStatus status(GeographyFirstTryConfig config, GeographyCompletionsState state) {
        int progress = Math.min(state.qualifyingGameIds.size(), 1);
        return new QuestStatus(progress, 1, progress == 1);
    }
}
