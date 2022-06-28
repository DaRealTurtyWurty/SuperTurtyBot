package io.github.darealturtywurty.superturtybot.commands.image;

public class NatureCommand extends PexelsImageCommand {
    public NatureCommand() {
        super(new Types(true, true, false, false));
    }

    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.SCENERY;
    }

    @Override
    public String getName() {
        return "nature";
    }

    @Override
    public String getRichName() {
        return "Nature Image";
    }

    @Override
    String getSearchTerm() {
        return "nature";
    }

    @Override
    int maxPages() {
        return 3;
    }
}
