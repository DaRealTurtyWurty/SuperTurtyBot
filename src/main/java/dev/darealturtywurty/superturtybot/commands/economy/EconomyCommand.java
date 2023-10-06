package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public abstract class EconomyCommand extends CoreCommand {
    public EconomyCommand() {
        super(new Types(true, false, false, false));
    }

    protected EconomyCommand(Types types) {
        super(types);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.ECONOMY;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 10L);
    }
}