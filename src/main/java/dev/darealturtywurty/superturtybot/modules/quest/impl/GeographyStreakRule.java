package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class GeographyStreakRule implements QuestRule<GeographyStreakConfig, GeographyStreakState> {
    @Override
    public GeographyStreakState createState() {
        return new GeographyStreakState();
    }

    @Override
    public void apply(GeographyStreakConfig config, GeographyStreakState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.GeographyAnswered(_, String gameType, boolean correct))
                || !config.gameTypes().contains(gameType))
            return;

        if (correct) {
            state.current++;
            state.best = Math.max(state.best, state.current);
        } else {
            state.current = 0;
        }
    }

    @Override
    public QuestStatus status(GeographyStreakConfig config, GeographyStreakState state) {
        int progress = Math.min(state.best, config.requiredAnswers());
        return new QuestStatus(progress, config.requiredAnswers(), progress >= config.requiredAnswers());
    }
}
