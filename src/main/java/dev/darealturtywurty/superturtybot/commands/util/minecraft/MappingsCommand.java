package dev.darealturtywurty.superturtybot.commands.util.minecraft;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.commands.SearchCommand;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.commands.TranslateCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;

public class MappingsCommand extends CoreCommand {
    public MappingsCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(new TranslateCommand(), new SearchCommand());
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Search and translate Minecraft classes, methods, and fields.";
    }

    @Override
    public String getName() {
        return "mappings";
    }

    @Override
    public String getRichName() {
        return "Mappings";
    }
}
