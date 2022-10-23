package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;

public abstract class AbstractImageCommand extends CoreCommand {
    protected AbstractImageCommand(Types types) {
        super(types);
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    public abstract ImageCategory getImageCategory();
    
    public enum ImageCategory {
        ANIMAL, SCENERY, FUN, GENERATION, MISC
    }
}
