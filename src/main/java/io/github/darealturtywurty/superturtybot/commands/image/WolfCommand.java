package io.github.darealturtywurty.superturtybot.commands.image;

public class WolfCommand extends PexelsImageCommand {
    public WolfCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }

    @Override
    public String getName() {
        return "wolf";
    }

    @Override
    public String getRichName() {
        return "Wolf Image";
    }

    @Override
    String getSearchTerm() {
        return "wolf";
    }

    @Override
    int maxPages() {
        return 2;
    }
}
