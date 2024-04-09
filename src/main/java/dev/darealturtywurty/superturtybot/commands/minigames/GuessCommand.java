package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

// TODO: add how to use
public class GuessCommand extends CoreCommand {
    public GuessCommand() {
        super(new Types(true, false, false, false));

        addSubcommands(new GeoGuesserCommand(),
                new GuessCombinedFlagsCommand(),
                new GuessRegionBorderCommand(),
                new GuessSongCommand());
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a variety of guessing games!";
    }

    @Override
    public String getName() {
        return "guess";
    }

    @Override
    public String getRichName() {
        return "Guess";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
