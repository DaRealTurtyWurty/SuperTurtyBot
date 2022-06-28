package io.github.darealturtywurty.superturtybot.commands.image;

public class BeeCommand extends PexelsImageCommand {
    public BeeCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }

    @Override
    public String getName() {
        return "bee";
    }

    @Override
    public String getRichName() {
        return "Bee Image";
    }

    @Override
    String getSearchTerm() {
        return "bee";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
