package io.github.darealturtywurty.superturtybot.commands.image;

public class TigerCommand extends PexelsImageCommand {
    public TigerCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "tiger";
    }
    
    @Override
    public String getRichName() {
        return "Tiger Image";
    }
    
    @Override
    String getSearchTerm() {
        return "tiger";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
