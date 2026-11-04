package dev.darealturtywurty.superturtybot.dashboard.service.commands;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CommandMetadataGenerator;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;

import java.util.List;

public final class CommandSettingsService {
    public record Settings(List<String> disabledCommands) {
    }

    public record Change(Boolean enabled) {
    }

    public String metadata() {
        var commands = CommandHook.INSTANCE.getCommands();
        if (commands.isEmpty())
            throw new DashboardApiException(HttpStatus.SERVICE_UNAVAILABLE, "commands_not_ready",
                "Commands are still loading. Try again shortly.");
        return CommandMetadataGenerator.generate(commands).toString();
    }

    public Settings getSettings(long guildId) {
        GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
        return new Settings(
            data == null || data.getDisabledCommands() == null ? List.of() : data.getDisabledCommands());
    }

    public Settings update(long guildId, String commandName, Change change) {
        if (CommandHook.INSTANCE.getCommands().stream().noneMatch(command -> command.getName().equals(commandName)))
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "command_not_found", "That command does not exist.");
        if (change == null || change.enabled() == null)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_command_settings",
                "Choose whether to enable this command.");

        GuildData.getOrCreateGuildData(guildId);
        Database.getDatabase().guildData.updateOne(Filters.eq("guild", guildId), change.enabled()
            ? Updates.pull("disabledCommands", commandName)
            : Updates.addToSet("disabledCommands", commandName));
        return getSettings(guildId);
    }
}
