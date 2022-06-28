package io.github.darealturtywurty.superturtybot.commands.image;

public class LionCommand extends PexelsImageCommand {
    public LionCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }

    @Override
    public String getName() {
        return "lion";
    }

    @Override
    public String getRichName() {
        return "Lion Image";
    }

    @Override
    String getSearchTerm() {
        return "lion";
    }

    @Override
    int maxPages() {
        return 3;
    }
}
