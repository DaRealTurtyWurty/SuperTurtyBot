package io.github.darealturtywurty.superturtybot.commands.image;

public class ForestCommand extends PexelsImageCommand {
    public ForestCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.SCENERY;
    }
    
    @Override
    public String getName() {
        return "forest";
    }
    
    @Override
    public String getRichName() {
        return "Forest Image";
    }
    
    @Override
    String getSearchTerm() {
        return "forest";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
