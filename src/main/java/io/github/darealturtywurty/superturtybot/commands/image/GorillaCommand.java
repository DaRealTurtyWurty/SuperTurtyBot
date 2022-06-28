package io.github.darealturtywurty.superturtybot.commands.image;

public class GorillaCommand extends PexelsImageCommand {
    public GorillaCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "gorilla";
    }
    
    @Override
    public String getRichName() {
        return "Gorilla Image";
    }
    
    @Override
    String getSearchTerm() {
        return "gorilla";
    }
    
    @Override
    int maxPages() {
        return 1;
    }
}
