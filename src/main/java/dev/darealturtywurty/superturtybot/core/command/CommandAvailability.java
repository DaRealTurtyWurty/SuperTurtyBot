package dev.darealturtywurty.superturtybot.core.command;

import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;

public final class CommandAvailability {
    private CommandAvailability() {
    }

    public static boolean isDisabled(long guildId, String commandName) {
        return Database.getDatabase().guildData.countDocuments(
            Filters.and(Filters.eq("guild", guildId), Filters.eq("disabledCommands", commandName)),
            new CountOptions().limit(1)) > 0;
    }
}
