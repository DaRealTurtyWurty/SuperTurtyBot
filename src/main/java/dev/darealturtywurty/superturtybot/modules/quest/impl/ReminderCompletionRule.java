package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class ReminderCompletionRule
    implements
        QuestRule<ReminderCompletionConfig, ReminderCompletionState> {
    @Override
    public ReminderCompletionState createState() {
        return new ReminderCompletionState();
    }

    @Override
    public void apply(ReminderCompletionConfig config, ReminderCompletionState state, QuestEvent event) {
        switch (event) {
            case QuestEvent.ReminderCreated(String reminderId) -> state.createdReminderIds.add(reminderId);
            case QuestEvent.ReminderFired(String reminderId) -> state.firedReminderIds.add(reminderId);
            default -> {
            }
        }
    }

    @Override
    public QuestStatus status(ReminderCompletionConfig config, ReminderCompletionState state) {
        int completedReminders = Math.toIntExact(state.createdReminderIds.stream()
            .filter(state.firedReminderIds::contains)
            .count());
        int progress = Math.min(completedReminders, config.requiredReminders());
        return new QuestStatus(progress, config.requiredReminders(), progress >= config.requiredReminders());
    }
}
