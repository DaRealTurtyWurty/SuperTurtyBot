package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class GoFishHowToPlaySubcommand extends GoFishSubcommand {
    protected GoFishHowToPlaySubcommand() {
        super("howtoplay", "Learn how to play Go Fish");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String message = """
                **Go Fish — How to Play**
                Goal: collect the most books (four of a kind).
                
                **Commands**
                - `/gofish create <bet>`: create a lobby (players join via buttons)
                - `/gofish hand`: view your cards (ephemeral)
                - `/gofish ask <player> <rank>`: ask another player for a rank
                - `/gofish status`: show current game status
                
                **Rules**
                - You must have at least one card of a rank to ask for it.
                - If the target has any, they give you all of that rank and you go again.
                - Otherwise you draw from the deck (“Go fish”). If you draw the asked rank, you go again.
                - Any time you complete a book (four of a rank), it is scored.
                - Game ends when all 13 books are made or no one has cards and the deck is empty.
                """;
        event.getHook().editOriginal(message).queue();
    }
}
