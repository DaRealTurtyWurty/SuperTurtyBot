package io.github.darealturtywurty.superturtybot.commands.image;

public class ZebraCommand extends PexelsImageCommand {
    public ZebraCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "zebra";
    }
    
    @Override
    public String getRichName() {
        return "Zebra Image";
    }
    
    @Override
    String getSearchTerm() {
        return "zebra";
    }
    
    @Override
    int maxPages() {
        return 2;
    }
}
