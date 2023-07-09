package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 2L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.getJDA().getRestPing().queue(ping -> event.replyFormat("Rest Ping: %sms\nWebsocket Ping: %sms", ping,
                event.getJDA().getGatewayPing()).mentionRepliedUser(false).queue());
    }
}
