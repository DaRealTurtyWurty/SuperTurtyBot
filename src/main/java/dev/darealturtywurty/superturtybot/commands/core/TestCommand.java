package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class TestCommand extends CoreCommand {
    public TestCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "A test command!";
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getRichName() {
        return "Test";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {

    }
}
