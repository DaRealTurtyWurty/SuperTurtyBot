package io.github.darealturtywurty.superturtybot.commands.image;

public class HorseCommand extends PexelsImageCommand {
    public HorseCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }

    @Override
    public String getName() {
        return "horse";
    }

    @Override
    public String getRichName() {
        return "Horse Image";
    }

    @Override
    String getSearchTerm() {
        return "horse";
    }

    @Override
    int maxPages() {
        return 3;
    }
}
