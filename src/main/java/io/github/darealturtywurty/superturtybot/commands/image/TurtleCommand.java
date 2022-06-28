package io.github.darealturtywurty.superturtybot.commands.image;

public class TurtleCommand extends PexelsImageCommand {
    public TurtleCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "turtle";
    }
    
    @Override
    public String getRichName() {
        return "Turtle Image";
    }
    
    @Override
    String getSearchTerm() {
        return "turtle";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
