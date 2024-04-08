package dev.darealturtywurty.superturtybot.modules.rpg.command;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;

public abstract class RPGCommand extends CoreCommand {
    public RPGCommand(boolean slash, boolean message, boolean user) {
        super(new Types(slash, false, message, user));
    }

    public RPGCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.RPG;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected final void runSlash(SlashCommandInteractionEvent event) {
        if(event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!");
            return;
        }

        handleSlash(event);
    }

    @Override
    protected final void runMessageCtx(MessageContextInteractionEvent event) {
        if(event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!");
            return;
        }

        handleMessageCtx(event);
    }

    @Override
    protected final void runUserCtx(UserContextInteractionEvent event) {
        if (event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!");
            return;
        }

        handleUserCtx(event);
    }

    protected void handleSlash(SlashCommandInteractionEvent event) {}
    protected void handleMessageCtx(MessageContextInteractionEvent event) {}
    protected void handleUserCtx(UserContextInteractionEvent event) {}
}
