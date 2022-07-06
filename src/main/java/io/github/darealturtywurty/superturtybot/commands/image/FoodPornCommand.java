package io.github.darealturtywurty.superturtybot.commands.image;

public class FoodPornCommand extends PexelsImageCommand {
    public FoodPornCommand() {
        super(new Types(true, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.MISC;
    }
    
    @Override
    public String getName() {
        return "foodporn";
    }
    
    @Override
    public String getRichName() {
        return "Food Porn";
    }
    
    @Override
    String getSearchTerm() {
        return "food";
    }
    
    @Override
    int maxPages() {
        return 3;
    }
}
