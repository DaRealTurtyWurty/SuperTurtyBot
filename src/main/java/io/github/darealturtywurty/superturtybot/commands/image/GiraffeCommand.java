package io.github.darealturtywurty.superturtybot.commands.image;

public class GiraffeCommand extends PexelsImageCommand {
    public GiraffeCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "giraffe";
    }
    
    @Override
    public String getRichName() {
        return "Giraffe Image";
    }
    
    @Override
    String getSearchTerm() {
        return "giraffe";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
