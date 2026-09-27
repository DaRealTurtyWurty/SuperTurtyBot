package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

import java.time.LocalDate;

public final class WordleCompletionStreakRule
        implements QuestRule<WordleCompletionStreakConfig, WordleCompletionStreakState> {
    @Override
    public WordleCompletionStreakState createState() {
        return new WordleCompletionStreakState();
    }

    @Override
    public void apply(WordleCompletionStreakConfig config, WordleCompletionStreakState state, QuestEvent event) {
        if (event instanceof QuestEvent.WordleCompleted(LocalDate date)) {
            state.completedDays.add(date);
        }
    }

    @Override
    public QuestStatus status(WordleCompletionStreakConfig config, WordleCompletionStreakState state) {
        int currentStreak = 0;
        int bestStreak = 0;
        LocalDate previous = null;

        for (LocalDate date : state.completedDays.stream().sorted().toList()) {
            currentStreak = previous != null && date.equals(previous.plusDays(1))
                    ? currentStreak + 1
                    : 1;
            bestStreak = Math.max(bestStreak, currentStreak);
            previous = date;
        }

        int target = config.requiredDays();
        int progress = Math.min(bestStreak, target);
        return new QuestStatus(progress, target, progress >= target);
    }
}
