package io.github.darealturtywurty.superturtybot.commands.image;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;

public abstract class ImageCommand extends CoreCommand {
    protected ImageCommand(Types types) {
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
