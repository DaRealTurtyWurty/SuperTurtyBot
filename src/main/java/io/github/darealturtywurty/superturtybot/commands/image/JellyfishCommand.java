package io.github.darealturtywurty.superturtybot.commands.image;

public class JellyfishCommand extends PexelsImageCommand {
    public JellyfishCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "jellyfish";
    }
    
    @Override
    public String getRichName() {
        return "Jellyfish Image";
    }
    
    @Override
    String getSearchTerm() {
        return "jellyfish";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
