package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class GeographyAnswersRule implements QuestRule<GeographyAnswersConfig, GeographyAnswersState> {
    @Override
    public GeographyAnswersState createState() {
        return new GeographyAnswersState();
    }

    @Override
    public void apply(GeographyAnswersConfig config, GeographyAnswersState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.GeographyAnswered(String sourceId, String gameType, boolean correct))
                || !correct)
            return;

        state.correctAnswerIds.add(sourceId);
        state.correctGameTypes.add(gameType);
    }

    @Override
    public QuestStatus status(GeographyAnswersConfig config, GeographyAnswersState state) {
        int answerProgress = Math.min(state.correctAnswerIds.size(), config.requiredCorrectAnswers());
        int typeProgress = Math.min(state.correctGameTypes.size(), config.requiredGameTypes());
        boolean answersComplete = answerProgress >= config.requiredCorrectAnswers();
        boolean typesComplete = typeProgress >= config.requiredGameTypes();

        if (config.requiredCorrectAnswers() == 0)
            return new QuestStatus(typeProgress, config.requiredGameTypes(), typesComplete);

        if (config.requiredGameTypes() == 0)
            return new QuestStatus(answerProgress, config.requiredCorrectAnswers(), answersComplete);

        String progressText = answerProgress + "/" + config.requiredCorrectAnswers() + " answers • "
                + typeProgress + "/" + config.requiredGameTypes() + " modes";
        return new QuestStatus(
                answerProgress + typeProgress,
                config.requiredCorrectAnswers() + config.requiredGameTypes(),
                answersComplete && typesComplete,
                progressText
        );
    }
}
