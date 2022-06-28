package io.github.darealturtywurty.superturtybot.commands.image;

public class MonkeyCommand extends PexelsImageCommand {
    public MonkeyCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "monkey";
    }

    @Override
    public String getRichName() {
        return "Monkey Image";
    }

    @Override
    String getSearchTerm() {
        return "monkey";
    }

    @Override
    int maxPages() {
        return 3;
    }
}
