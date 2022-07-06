package io.github.darealturtywurty.superturtybot.commands.image;

public class WhaleCommand extends PexelsImageCommand {
    public WhaleCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "whale";
    }
    
    @Override
    public String getRichName() {
        return "Whale Image";
    }
    
    @Override
    String getSearchTerm() {
        return "whale";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
