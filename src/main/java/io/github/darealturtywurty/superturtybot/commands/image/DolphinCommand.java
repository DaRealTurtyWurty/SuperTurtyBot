package io.github.darealturtywurty.superturtybot.commands.image;

public class DolphinCommand extends PexelsImageCommand {
    public DolphinCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "dolphin";
    }
    
    @Override
    public String getRichName() {
        return "Dolphin Image";
    }
    
    @Override
    String getSearchTerm() {
        return "dolphin";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
