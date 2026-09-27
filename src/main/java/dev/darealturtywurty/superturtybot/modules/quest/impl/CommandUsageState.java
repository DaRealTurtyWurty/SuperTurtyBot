package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public final class CommandUsageState {
    public final Set<String> commandTypes = new HashSet<>();
    public final Set<LocalDate> days = new HashSet<>();
}
