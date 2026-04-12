package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BattleshipsPowerUpCommand extends BattleshipsSubcommand {
    public BattleshipsPowerUpCommand() {
        super("power-up", "Use a power-up in your battleships game", true);
        var powerUpTypeOption = new OptionData(OptionType.STRING, "power-up-type", "The type of power-up to use", true);
        for (BattleshipsCommand.PowerUp powerUp : BattleshipsCommand.PowerUp.values()) {
            powerUpTypeOption.addChoice(powerUp.getDisplayName(), powerUp.name());
        }
        addOption(powerUpTypeOption);

        addOption(OptionType.STRING, BattleshipsCommand.OPTION_GRID_POSITION, "The grid position to target", true);
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
            replyBattleships(event, "❌ You can only use power-ups in the game thread: <#" + game.getThreadId() + ">.").queue();
            return;
        }

        if (!game.isPlayer(user.getIdLong())) {
            replyBattleships(event, "❌ You are not a player in this game!").queue();
            return;
        }

        if (!game.hasPlacedAllShips(user.getIdLong())) {
            replyBattleships(event, "❌ You must place all your ships before using a power-up.").queue();
            return;
        }

        if (!game.isReady()) {
            replyBattleships(event, "❌ The other player has not finished placing their ships yet.").queue();
            return;
        }

        if (!game.isTurn(user.getIdLong())) {
            replyBattleships(event, "❌ It's not your turn! It's currently <@" + game.getCurrentTurn() + ">'s turn.").queue();
            return;
        }

        String powerUpTypeStr = event.getOption("power-up-type", null, OptionMapping::getAsString);
        if (powerUpTypeStr == null || powerUpTypeStr.isBlank()) {
            replyBattleships(event, "❌ You must specify a valid power-up type.").queue();
            return;
        }

        BattleshipsCommand.PowerUp powerUpType;
        try {
            powerUpType = BattleshipsCommand.PowerUp.valueOf(powerUpTypeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            replyBattleships(event, "❌ Invalid power-up type specified.").queue();
            return;
        }

        if (!game.hasPowerUp(user.getIdLong(), powerUpType)) {
            replyBattleships(event, "❌ You do not have any " + powerUpType.getDisplayName() + " power-ups available.").queue();
            return;
        }

        String gridPosition = event.getOption(BattleshipsCommand.OPTION_GRID_POSITION, null, OptionMapping::getAsString);
        if (gridPosition == null || gridPosition.isBlank()) {
            replyBattleships(event, "❌ You must specify a valid grid position to use your power-up on.").queue();
            return;
        }

        String normalizedGridPosition = gridPosition.toUpperCase(Locale.ROOT);
        if (!normalizedGridPosition.matches("^[A-J](10|[1-9])$")) {
            replyBattleships(event, "❌ The grid position must be between A1 and J10.").queue();
            return;
        }

        int x = normalizedGridPosition.charAt(0) - 'A';
        int y = Integer.parseInt(normalizedGridPosition.substring(1)) - 1;
        BattleshipsCommand.PowerUpResult powerUpResult = game.usePowerUp(user.getIdLong(), powerUpType, x, y);
        if (!powerUpResult.success()) {
            replyBattleships(event, "❌ " + powerUpResult.message()).queue();
            return;
        }

        String[] names = BattleshipsCommand.buildNames(event, game);
        String response = "✅ You used the " + powerUpType.getDisplayName() + " power-up at "
                + normalizedGridPosition + "!";

        if (game.isGameOver(user.getIdLong())) {
            response += "\n🏆 " + user.getAsMention() + " wins! Game over.";
            BattleshipsCommand.GAMES.remove(game.getThreadId(), game);
            event.getChannel().asThreadChannel().getManager().setLocked(true).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
            try {
                FileUpload upload = BattleshipsImageRenderer.createUpload(
                        game, names, game.getPlayer1().getUserId(), game.getPlayer2().getUserId());
                replyBattleships(event, response).setFiles(upload).queue();
            } catch (IOException exception) {
                replyBattleships(event, response).queue();
            }
            return;
        }

        game.endTurn();

        boolean shouldShowOtherPlayerPosition = powerUpType.isShouldShowOtherPlayerPosition();
        response += "\nIt's now <@" + game.getCurrentTurn() + ">'s turn to attack.";
        try {
            FileUpload upload = shouldShowOtherPlayerPosition
                    ? BattleshipsImageRenderer.createUpload(game, names)
                    : BattleshipsImageRenderer.createUpload(game, names, user.getIdLong());
            replyBattleships(event, response).setFiles(upload).queue();
        } catch (IOException exception) {
            replyBattleships(event, response).queue();
        }
    }
}
