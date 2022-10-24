package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class BalanceCommand extends CoreCommand {
    public BalanceCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getRichName() {
        return "Balance";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.ECONOMY;
    }

    @Override
    public String getDescription() {
        return "Gets your economy balance for this server (both wallet and bank).";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if(!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }


    }
}
