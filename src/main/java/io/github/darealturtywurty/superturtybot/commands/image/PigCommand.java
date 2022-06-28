package io.github.darealturtywurty.superturtybot.commands.image;

public class PigCommand extends PexelsImageCommand {
    public PigCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "pig";
    }
    
    @Override
    public String getRichName() {
        return "Pig Image";
    }
    
    @Override
    String getSearchTerm() {
        return "pig";
    }
    
    @Override
    int maxPages() {
        return 2;
    }
}
