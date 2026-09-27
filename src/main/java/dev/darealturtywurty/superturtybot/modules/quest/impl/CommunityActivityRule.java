package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class CommunityActivityRule implements QuestRule<CommunityActivityConfig, CommunityActivityState> {
    @Override
    public CommunityActivityState createState() {
        return new CommunityActivityState();
    }

    @Override
    public void apply(CommunityActivityConfig config, CommunityActivityState state, QuestEvent event) {
        if (event instanceof QuestEvent.CommunityActivityCompleted(
                String sourceId,
                String activityType,
                var date
        ) && config.activityType().equals(activityType)) {
            state.activities.putIfAbsent(sourceId, date);
        }
    }

    @Override
    public QuestStatus status(CommunityActivityConfig config, CommunityActivityState state) {
        int actionProgress = Math.min(state.activities.size(), config.requiredActions());
        int dayProgress = Math.min(
                Math.toIntExact(state.activities.values().stream().distinct().count()),
                config.requiredDays()
        );
        boolean complete = actionProgress >= config.requiredActions() && dayProgress >= config.requiredDays();

        if (config.requiredDays() == 1)
            return new QuestStatus(actionProgress, config.requiredActions(), complete);

        String progressText = actionProgress + "/" + config.requiredActions() + " uses • "
                + dayProgress + "/" + config.requiredDays() + " days";
        return new QuestStatus(actionProgress, config.requiredActions(), complete, progressText);
    }
}
