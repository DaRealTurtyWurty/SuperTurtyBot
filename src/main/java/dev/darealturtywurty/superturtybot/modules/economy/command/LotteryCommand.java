package dev.darealturtywurty.superturtybot.modules.economy.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class LotteryCommand extends EconomyCommand {
    @Override
    public String getDescription() {
        return "Enter the lottery!";
    }

    @Override
    public String getName() {
        return "lottery";
    }

    @Override
    public String getRichName() {
        return "Lottery";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if(!event.isFromGuild() || guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }


    }
}
