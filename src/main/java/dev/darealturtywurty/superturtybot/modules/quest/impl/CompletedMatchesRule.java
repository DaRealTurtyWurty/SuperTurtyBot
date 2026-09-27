package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class CompletedMatchesRule implements QuestRule<CompletedMatchesConfig, CompletedMatchesState> {
    @Override
    public CompletedMatchesState createState() {
        return new CompletedMatchesState();
    }

    @Override
    public void apply(CompletedMatchesConfig config, CompletedMatchesState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.MultiplayerMatchCompleted match))
            return;

        if (config.gameType() == null || config.gameType().equals(match.gameType())) {
            state.completedMatchIds.add(match.sourceId());
        }
    }

    @Override
    public QuestStatus status(CompletedMatchesConfig config, CompletedMatchesState state) {
        int target = config.requiredMatches();
        int progress = Math.min(state.completedMatchIds.size(), target);

        return new QuestStatus(
                progress,
                target,
                progress >= target
        );
    }
}
