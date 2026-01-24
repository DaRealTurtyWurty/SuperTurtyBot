package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class BattleshipsHowToPlayCommand extends BattleshipsSubcommand {
    public BattleshipsHowToPlayCommand() {
        super("howtoplay", "Learn how to play Battleships", false);
    }

    @Override
    protected void executeSubcommand(SlashCommandInteractionEvent event) {
        String message = """
                **Battleships — How to Play**
                Goal: sink all of your opponent's ships.

                **Commands**
                - `/battleships play <opponent>`: start a game (creates a thread)
                - `/battleships place <grid-position> <orientation> <ship-type>`: place a ship
                - `/battleships attack <grid-position>`: fire at a grid position
                - `/battleships reveal`: show your board with your ships (ephemeral)
                - `/battleships power-up <power-up-type> <grid-position>`: use a power-up

                **Rules**
                - Each player places 5 ships: carrier (5), battleship (4), destroyer (3), submarine (3), patrol boat (2).
                - Place ships on the 10x10 grid (A1–J10) horizontally or vertically.
                - Take turns attacking; hits and misses are marked on the board.
                - When all of a ship's cells are hit, it is sunk.
                - First player to sink all opponent ships wins.
                """;
        replyBattleships(event, message).queue();
    }
}
