package io.github.darealturtywurty.superturtybot.core.command;

import java.util.List;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public interface BotCommand {
    default List<OptionData> createOptions() {
        return List.of();
    }

    default List<SubcommandData> createSubcommands() {
        return List.of();
    }

    CommandCategory getCategory();

    String getDescription();
    
    String getName();
    
    default String getRichName() {
        return getName();
    }
}
