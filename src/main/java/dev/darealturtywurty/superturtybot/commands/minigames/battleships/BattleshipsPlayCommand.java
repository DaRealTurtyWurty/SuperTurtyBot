package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BattleshipsPlayCommand extends BattleshipsSubcommand {
    public BattleshipsPlayCommand() {
        super("play", "Start a game of battleships against another player or the AI", false);
        addOption(OptionType.USER, "opponent", "The user you want to play against", true);
    }

    @Override
    public void executeSubcommand(SlashCommandInteractionEvent event) {
        if (event.getChannel() instanceof ThreadChannel) {
            replyBattleships(event, "❌ This command cannot be used in a thread.").queue();
            return;
        }

        Guild guild = event.getGuild();
        User user = event.getUser();
        BattleshipsCommand.Game existingGame = BattleshipsCommand.getGame(guild.getIdLong(), user.getIdLong())
                .orElse(null);
        if (existingGame != null) {
            replyBattleships(event, "❌ You are already in a game! Please finish it at <#" + existingGame.getThreadId() + ">.").queue();
            return;
        }

        User opponent = event.getOption("opponent", null, OptionMapping::getAsUser);
        if (opponent == null) {
            replyBattleships(event, "❌ Invalid opponent specified!").queue();
            return;
        }

        if (opponent.getIdLong() == user.getIdLong()) {
            replyBattleships(event, "❌ You cannot play against yourself!").queue();
            return;
        }

        replyBattleships(event, "✅ Starting a game of Battleships between " + user.getAsMention() + " and " + opponent.getAsMention() + "!").queue(message -> {
            message.createThreadChannel("Battleships: " + user.getName() + " vs " + opponent.getName())
                    .queue(threadChannel -> {
                        var game = new BattleshipsCommand.Game(guild.getIdLong(),
                                event.getChannel().getIdLong(), threadChannel.getIdLong(),
                                user.getIdLong(), opponent.getIdLong(), true);
                        BattleshipsCommand.GAMES.put(threadChannel.getIdLong(), game);

                        threadChannel.sendMessage("Game started between " + user.getAsMention() + " and " + opponent.getAsMention() + "!").queue();
                        try {
                            String[] names = {
                                    resolveDisplayName(event, user.getIdLong(), "Player 1"),
                                    resolveDisplayName(event, opponent.getIdLong(), "Player 2")
                            };
                            FileUpload upload = BattleshipsImageRenderer.createUpload(game, names);
                            threadChannel.sendMessage("Here is the initial game board. Please use the `/battleships place` command to place your ships.").addFiles(upload).queue();
                        } catch (IOException exception) {
                            Constants.LOGGER.error("Failed to render battleships image for game in thread {}", threadChannel.getId(), exception);
                            threadChannel.sendMessage("❌ An error occurred while generating the game board image.").queue();
                            BattleshipsCommand.GAMES.remove(threadChannel.getIdLong());
                            threadChannel.getManager().setLocked(true).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
                        }
                    });
        });
    }
}
