package io.github.darealturtywurty.superturtybot.commands.core;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PingCommand extends CoreCommand {
    public PingCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }
    
    @Override
    public String getDescription() {
        return "Gets the ping of the bot";
    }
    
    @Override
    public String getName() {
        return "ping";
    }
    
    @Override
    public String getRichName() {
        return "Ping";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.getJDA().getRestPing()
            .queue(ping -> event
                .replyFormat("Rest Ping: %sms\nWebsocket Ping: %sms", ping, event.getJDA().getGatewayPing())
                .mentionRepliedUser(false).queue());
    }
}
