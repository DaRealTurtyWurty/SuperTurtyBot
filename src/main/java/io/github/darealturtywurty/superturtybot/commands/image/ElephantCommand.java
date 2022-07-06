package io.github.darealturtywurty.superturtybot.commands.image;

public class ElephantCommand extends PexelsImageCommand {
    public ElephantCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "elephant";
    }
    
    @Override
    public String getRichName() {
        return "Elephant Image";
    }
    
    @Override
    String getSearchTerm() {
        return "elephant";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
