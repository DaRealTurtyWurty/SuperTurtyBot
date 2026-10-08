package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class GeographyGameSizeRule implements QuestRule<GeographyGameSizeConfig, GeographyCompletionsState> {
    @Override
    public GeographyCompletionsState createState() {
        return new GeographyCompletionsState();
    }

    @Override
    public void apply(GeographyGameSizeConfig config, GeographyCompletionsState state, QuestEvent event) {
        if (event instanceof QuestEvent.GeographyGameCompleted(String sourceId, String gameType, int gameSize, _)
            && config.gameType().equals(gameType) && gameSize >= config.requiredSize()) {
            state.qualifyingGameIds.add(sourceId);
        }
    }

    @Override
    public QuestStatus status(GeographyGameSizeConfig config, GeographyCompletionsState state) {
        int progress = Math.min(state.qualifyingGameIds.size(), 1);
        return new QuestStatus(progress, 1, progress == 1);
    }
}
