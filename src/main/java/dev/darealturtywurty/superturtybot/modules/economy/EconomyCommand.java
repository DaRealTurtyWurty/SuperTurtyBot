package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;

public abstract class EconomyCommand extends CoreCommand {
    protected EconomyCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.ECONOMY;
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
}
