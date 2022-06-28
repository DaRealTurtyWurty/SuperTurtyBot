package io.github.darealturtywurty.superturtybot.commands.image;

public class SquirrelCommand extends PexelsImageCommand {
    public SquirrelCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }

    @Override
    public String getName() {
        return "squirrel";
    }

    @Override
    public String getRichName() {
        return "Squirrel Image";
    }

    @Override
    String getSearchTerm() {
        return "squirrel";
    }

    @Override
    int maxPages() {
        return 3;
    }
}
