package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SpeakCommand extends CoreCommand {
    public SpeakCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Makes the bot speak!";
    }

    @Override
    public String getName() {
        return "speak";
    }

    @Override
    public String getRichName() {
        return "Speak";
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if(event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId().orElseThrow(() -> new IllegalStateException("Owner ID is not set!")))
            return;

        event.getMessage().delete().queue();
        int commandIndex = event.getMessage().getContentRaw().indexOf(getName());

        String content = event.getMessage().getContentRaw().substring(commandIndex + getName().length());
        event.getChannel().sendMessage(content).queue();
    }
}
