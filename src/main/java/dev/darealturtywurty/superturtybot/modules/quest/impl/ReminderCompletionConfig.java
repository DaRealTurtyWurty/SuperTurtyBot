package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record ReminderCompletionConfig(int requiredReminders) {
    public ReminderCompletionConfig {
        if (requiredReminders < 1)
            throw new IllegalArgumentException("Required reminders must be greater than 0.");
    }
}
