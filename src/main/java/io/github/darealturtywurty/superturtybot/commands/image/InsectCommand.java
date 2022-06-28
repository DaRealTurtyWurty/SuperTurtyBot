package io.github.darealturtywurty.superturtybot.commands.image;

public class InsectCommand extends PexelsImageCommand {
    public InsectCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "insect";
    }
    
    @Override
    public String getRichName() {
        return "Insect Image";
    }
    
    @Override
    String getSearchTerm() {
        return "insect";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
