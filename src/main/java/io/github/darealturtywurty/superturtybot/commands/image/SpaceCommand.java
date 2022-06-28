package io.github.darealturtywurty.superturtybot.commands.image;

public class SpaceCommand extends PexelsImageCommand {
    public SpaceCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.SCENERY;
    }
    
    @Override
    public String getName() {
        return "space";
    }
    
    @Override
    public String getRichName() {
        return "Space Image";
    }
    
    @Override
    String getSearchTerm() {
        return "space";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
