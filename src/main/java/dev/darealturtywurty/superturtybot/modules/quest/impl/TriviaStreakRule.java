package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class TriviaStreakRule implements QuestRule<TriviaStreakConfig, TriviaStreakState> {
    @Override
    public TriviaStreakState createState() {
        return new TriviaStreakState();
    }

    @Override
    public void apply(TriviaStreakConfig config, TriviaStreakState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.TriviaAnswered(_, boolean correct)))
            return;

        if (correct) {
            state.current++;
            state.best = Math.max(state.best, state.current);
        } else if (config.consecutive()) {
            state.current = 0;
        }
    }

    @Override
    public QuestStatus status(TriviaStreakConfig config, TriviaStreakState state) {
        int target = config.requiredAnswers();
        return new QuestStatus(
            Math.min(state.best, target),
            target,
            state.best >= target);
    }
}
