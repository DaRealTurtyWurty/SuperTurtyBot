package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class EconomyActionsRule implements QuestRule<EconomyActionsConfig, EconomyActionsState> {
    @Override
    public EconomyActionsState createState() {
        return new EconomyActionsState();
    }

    @Override
    public void apply(EconomyActionsConfig config, EconomyActionsState state, QuestEvent event) {
        if (event instanceof QuestEvent.EconomyAction(String sourceId, String actionType, var date)
                && config.actionTypes().contains(actionType)) {
            state.actions.putIfAbsent(sourceId, date);
        }
    }

    @Override
    public QuestStatus status(EconomyActionsConfig config, EconomyActionsState state) {
        int actionProgress = Math.min(state.actions.size(), config.requiredActions());
        int dayProgress = Math.min(
                Math.toIntExact(state.actions.values().stream().distinct().count()),
                config.requiredDays()
        );
        boolean complete = actionProgress >= config.requiredActions() && dayProgress >= config.requiredDays();
        if (config.requiredDays() == 1)
            return new QuestStatus(actionProgress, config.requiredActions(), complete);

        String progressText = actionProgress + "/" + config.requiredActions() + " " + config.actionLabel() + " • "
                + dayProgress + "/" + config.requiredDays() + " days";
        return new QuestStatus(actionProgress, config.requiredActions(), complete, progressText);
    }
}
