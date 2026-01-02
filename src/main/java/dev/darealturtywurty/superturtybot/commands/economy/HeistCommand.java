package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class HeistCommand extends EconomyCommand {
    public HeistCommand() {
        addSubcommands(new HeistStartSubcommand(), new HeistProfileSubcommand());
    }

    @Override
    public String getDescription() {
        return "Heist actions and progress.";
    }

    @Override
    public String getName() {
        return "heist";
    }

    @Override
    public String getHowToUse() {
        return """
                To start a heist:
                `/heist start`
                To view your heist profile:
                `/heist profile`
                """;
    }

    @Override
    public String getRichName() {
        return "Heist";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 2L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        if (event.getSubcommandName() == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
        }
    }
}
