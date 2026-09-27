package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.util.HashSet;
import java.util.Set;

public final class ReminderCompletionState {
    public final Set<String> createdReminderIds = new HashSet<>();
    public final Set<String> firedReminderIds = new HashSet<>();
}
