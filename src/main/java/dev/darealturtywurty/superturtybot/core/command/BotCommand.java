package dev.darealturtywurty.superturtybot.core.command;

import java.util.List;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.apache.commons.lang3.tuple.Pair;

public interface BotCommand {
    default List<OptionData> createOptions() {
        return List.of();
    }
    
    default List<SubcommandData> createSubcommands() {
        return List.of();
    }

    default List<SubcommandGroupData> createSubcommandGroups() {
        return List.of();
    }
    
    CommandCategory getCategory();
    
    String getDescription();
    
    String getName();
    
    default String getRichName() {
        return getName();
    }

    default Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 0L);
    }
}
