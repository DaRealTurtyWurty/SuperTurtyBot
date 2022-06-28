package io.github.darealturtywurty.superturtybot.database;

import io.github.darealturtywurty.superturtybot.database.impl.CountingDatabaseHandler;
import io.github.darealturtywurty.superturtybot.database.impl.LevellingDatabaseHandler;

public class TurtyBotDatabase {
    public static final LevellingDatabaseHandler LEVELS = new LevellingDatabaseHandler();
    public static final CountingDatabaseHandler COUNTING = new CountingDatabaseHandler();
}
