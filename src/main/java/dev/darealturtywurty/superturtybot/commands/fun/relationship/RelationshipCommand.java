package dev.darealturtywurty.superturtybot.commands.fun.relationship;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;

public class RelationshipCommand extends CoreCommand {
    public RelationshipCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(new RelationshipDateCommand());
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Manage your relationship between other members!";
    }

    @Override
    public String getName() {
        return "relationship";
    }

    @Override
    public String getRichName() {
        return "Relationship";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
