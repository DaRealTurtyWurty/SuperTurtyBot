package io.github.darealturtywurty.superturtybot.commands.image;

public class CowCommand extends PexelsImageCommand {
    public CowCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "cow";
    }
    
    @Override
    public String getRichName() {
        return "Cow Image";
    }
    
    @Override
    String getSearchTerm() {
        return "cow";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
