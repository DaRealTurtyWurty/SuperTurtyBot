package dev.darealturtywurty.superturtybot.commands.economy.poker;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PokerHowToPlayCommand extends PokerSubcommand {
    protected PokerHowToPlayCommand() {
        super("howtoplay", "Learn how to play Poker");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String message = """
                **Poker (Texas Hold'em) — How to Play**
                Goal: make the best 5-card hand using your 2 hole cards + 5 community cards.

                **Commands**
                - `/poker play <bet>`: start a hand (creates a thread)
                - `/poker check`: reveal the next community cards
                - `/poker bet <amount>`: increase the pot before the next reveal
                - `/poker fold`: fold and end the hand

                **Rounds**
                - Pre-Flop → Flop → Turn → River → Showdown
                - At showdown, your best 5-card hand is compared to the dealer.
                """;
        event.getHook().editOriginal(message).queue();
    }
}
