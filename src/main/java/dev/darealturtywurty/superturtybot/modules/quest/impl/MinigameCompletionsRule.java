package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

import java.util.Set;

public final class MinigameCompletionsRule
        implements QuestRule<MinigameCompletionsConfig, MinigameCompletionsState> {
    @Override
    public MinigameCompletionsState createState() {
        return new MinigameCompletionsState();
    }

    @Override
    public void apply(MinigameCompletionsConfig config, MinigameCompletionsState state, QuestEvent event) {
        if (event instanceof QuestEvent.MinigameCompleted(String sourceId, String gameType)
                && config.requiredCompletions().containsKey(gameType)) {
            state.add(gameType, sourceId);
        }
    }

    @Override
    public QuestStatus status(MinigameCompletionsConfig config, MinigameCompletionsState state) {
        int progress = config.requiredCompletions().entrySet().stream()
                .mapToInt(entry -> Math.min(
                        state.completionIdsByGameType.getOrDefault(entry.getKey(), Set.of()).size(),
                        entry.getValue()))
                .sum();
        int target = config.requiredCompletions().values().stream().mapToInt(Integer::intValue).sum();
        return new QuestStatus(progress, target, progress >= target);
    }
}
