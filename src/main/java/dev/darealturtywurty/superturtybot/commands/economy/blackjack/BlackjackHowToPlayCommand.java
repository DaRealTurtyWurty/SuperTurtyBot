package dev.darealturtywurty.superturtybot.commands.economy.blackjack;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class BlackjackHowToPlayCommand extends BlackjackSubcommand {
    protected BlackjackHowToPlayCommand() {
        super("howtoplay", "Learn how to play Blackjack");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String message = """
                **Blackjack — How to Play**
                Goal: get as close to 21 as possible without going over.

                **Commands**
                - `/blackjack play <bet>`: start a game (creates a thread)
                - `/blackjack hit`: draw a card
                - `/blackjack stand`: end your turn and let the dealer play

                **Rules**
                - Number cards are worth their value, face cards are 10, Aces are 1 or 11.
                - If you go over 21, you bust and lose.
                - Dealer draws until at least 17.
                - Blackjack (21 with 2 cards) pays 3:2.
                """;
        event.getHook().editOriginal(message).queue();
    }
}
