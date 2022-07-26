package io.github.darealturtywurty.superturtybot.commands.core;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ShutdownCommand extends CoreCommand {
    public ShutdownCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Shuts down the bot.";
    }

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getRichName() {
        return "Shutdown";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId())
            return;

        event.getMessage().reply("😩 Shutting down! 😩").mentionRepliedUser(false).queue();
        ShutdownHooks.shutdown(event.getJDA());
    }
}
