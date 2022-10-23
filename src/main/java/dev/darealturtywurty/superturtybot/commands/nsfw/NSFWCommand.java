package dev.darealturtywurty.superturtybot.commands.nsfw;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class NSFWCommand extends CoreCommand {
    public final NSFWCategory category;

    protected NSFWCommand(NSFWCategory category) {
        super(new Types(false, true, false, false));
        this.category = category;
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.NSFW;
    }

    /**
     * Make sure to call this super method! <br>
     * <br>
     * The message will be deleted, so do NOT try to access it!
     */
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        // TODO: Check user config to see if they have denied NSFW usage
        // TODO: Check server config to see if the server has disabled this command
        if (event.isFromGuild() && !event.getChannel().asTextChannel().isNSFW())
            return;

        // TODO: Check user settings first
        event.getMessage().delete().queue();
        event.getChannel().sendTyping().queue();
    }

    public enum NSFWCategory {
        REAL, FAKE, MISC
    }
}
