package io.github.darealturtywurty.superturtybot.commands.image;

public class CrabCommand extends PexelsImageCommand {
    public CrabCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "crab";
    }
    
    @Override
    public String getRichName() {
        return "Crab Image";
    }
    
    @Override
    String getSearchTerm() {
        return "crab";
    }
    
    @Override
    int maxPages() {
        return 1;
    }
}
