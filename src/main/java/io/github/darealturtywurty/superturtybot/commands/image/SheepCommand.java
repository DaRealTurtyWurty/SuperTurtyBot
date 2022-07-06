package io.github.darealturtywurty.superturtybot.commands.image;

public class SheepCommand extends PexelsImageCommand {
    public SheepCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "sheep";
    }
    
    @Override
    public String getRichName() {
        return "Sheep Image";
    }
    
    @Override
    String getSearchTerm() {
        return "sheep";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
