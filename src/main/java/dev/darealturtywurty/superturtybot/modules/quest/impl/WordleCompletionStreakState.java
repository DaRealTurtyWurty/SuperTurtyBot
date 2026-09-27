package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class WordleCompletionStreakState {
    public final Set<LocalDate> completedDays = new HashSet<>();
}
