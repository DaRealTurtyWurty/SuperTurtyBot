package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class CommunityExplorerRule implements QuestRule<CommunityExplorerConfig, CommunityExplorerState> {
    @Override
    public CommunityExplorerState createState() {
        return new CommunityExplorerState();
    }

    @Override
    public void apply(CommunityExplorerConfig config, CommunityExplorerState state, QuestEvent event) {
        switch (event) {
            case QuestEvent.TriviaAnswered(String sourceId, _) -> state.triviaQuestionIds.add(sourceId);
            case QuestEvent.MultiplayerMatchCompleted(String sourceId, _, _, _) -> state.minigameIds.add(sourceId);
            case QuestEvent.MinigameCompleted(String sourceId, _) -> state.minigameIds.add(sourceId);
            case QuestEvent.WordleCompleted(var date) -> state.minigameIds.add("wordle:" + date);
            case QuestEvent.ValidCount(String messageId, _) -> state.countMessageIds.add(messageId);
            default -> {
            }
        }
    }

    @Override
    public QuestStatus status(CommunityExplorerConfig config, CommunityExplorerState state) {
        int triviaProgress = Math.min(state.triviaQuestionIds.size(), config.triviaQuestions());
        int minigameProgress = Math.min(state.minigameIds.size(), config.minigames());
        int countProgress = Math.min(state.countMessageIds.size(), config.validCounts());
        int progress = triviaProgress + minigameProgress + countProgress;
        int target = config.triviaQuestions() + config.minigames() + config.validCounts();
        return new QuestStatus(progress, target, progress >= target);
    }
}
