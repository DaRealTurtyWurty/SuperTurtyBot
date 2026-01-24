package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;

public class BattleshipsRevealCommand extends BattleshipsSubcommand {
    public BattleshipsRevealCommand() {
        super("reveal", "Reveal your own ships on the current board", true);
    }

    @Override
    protected void executeSubcommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();
        BattleshipsCommand.Game game = BattleshipsCommand.getGame(guild.getIdLong(), user.getIdLong()).orElse(null);
        if (game == null) {
            replyBattleships(event, "❌ You are not currently in a game! Start a new game with `/battleships play`.").queue();
            return;
        }

        if (game.getThreadId() != event.getChannel().getIdLong()) {
            replyBattleships(event, "❌ You can only reveal your board in the game thread: <#" + game.getThreadId() + ">.").queue();
            return;
        }

        if (!game.isPlayer(user.getIdLong())) {
            replyBattleships(event, "❌ You are not a player in this game!").queue();
            return;
        }

        try {
            String[] names = BattleshipsCommand.buildNames(event, game);
            FileUpload upload = BattleshipsImageRenderer.createUpload(game, names, user.getIdLong());
            replyBattleships(event, "✅ Here's your board with your ships revealed.").setFiles(upload).queue();
        } catch (IOException exception) {
            replyBattleships(event, "❌ An error occurred while generating the game board image.").queue();
        }
    }
}
