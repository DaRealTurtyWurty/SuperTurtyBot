package dev.darealturtywurty.superturtybot.modules.quest.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public final class CollectablesEarnedState {
    public final Set<String> questionIds = new HashSet<>();
    public final Set<String> collectionTypes = new HashSet<>();
    public final Set<LocalDate> dates = new HashSet<>();
}
