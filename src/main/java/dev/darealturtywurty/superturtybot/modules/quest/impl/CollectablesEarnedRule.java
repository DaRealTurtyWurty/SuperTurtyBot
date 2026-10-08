package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

import java.time.LocalDate;

public final class CollectablesEarnedRule implements QuestRule<CollectablesEarnedConfig, CollectablesEarnedState> {
    @Override
    public CollectablesEarnedState createState() {
        return new CollectablesEarnedState();
    }

    @Override
    public void apply(CollectablesEarnedConfig config, CollectablesEarnedState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.CollectableEarned(String questionId, String collectionType, int rarityOrdinal, long responseTimeMillis, LocalDate date)))
            return;

        if (config.collectionType() != null && !config.collectionType().equals(collectionType))
            return;

        if (config.minimumRarityOrdinal() >= 0
            && rarityOrdinal < config.minimumRarityOrdinal())
            return;

        if (config.maximumResponseTimeMillis() > 0
            && responseTimeMillis > config.maximumResponseTimeMillis())
            return;

        if (state.questionIds.add(questionId)) {
            if (collectionType != null) {
                state.collectionTypes.add(collectionType);
            }
            state.dates.add(date);
        }
    }

    @Override
    public QuestStatus status(CollectablesEarnedConfig config, CollectablesEarnedState state) {
        int collectableProgress = Math.min(state.questionIds.size(), config.requiredCollectables());
        int collectionProgress = Math.min(state.collectionTypes.size(), config.requiredCollections());
        int dayProgress = Math.min(state.dates.size(), config.requiredDays());
        int progress = collectableProgress + collectionProgress + dayProgress;
        int target = config.requiredCollectables() + config.requiredCollections() + config.requiredDays();
        return new QuestStatus(progress, target, progress >= target);
    }
}
