package io.github.darealturtywurty.superturtybot.commands.image;

public class SpiderCommand extends PexelsImageCommand {
    public SpiderCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "spider";
    }
    
    @Override
    public String getRichName() {
        return "Spider Image";
    }
    
    @Override
    String getSearchTerm() {
        return "spider";
    }
    
    @Override
    int maxPages() {
        return 2;
    }
}
