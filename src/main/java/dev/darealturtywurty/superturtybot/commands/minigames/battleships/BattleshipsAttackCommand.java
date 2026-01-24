package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BattleshipsAttackCommand extends BattleshipsSubcommand {
    public BattleshipsAttackCommand() {
        super("attack", "Attack a position on your opponent's board", false);
        addOption(OptionType.STRING, BattleshipsCommand.OPTION_GRID_POSITION, "The grid position to attack (e.g., A5)", true);
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
            replyBattleships(event, "❌ You can only attack in the game thread: <#" + game.getThreadId() + ">.").queue();
            return;
        }

        if (!game.isPlayer(user.getIdLong())) {
            replyBattleships(event, "❌ You are not a player in this game!").queue();
            return;
        }

        if (!game.hasPlacedAllShips(user.getIdLong())) {
            replyBattleships(event, "❌ You must place all your ships before attacking.").queue();
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

        String gridPosition = event.getOption(BattleshipsCommand.OPTION_GRID_POSITION, null, OptionMapping::getAsString);
        if (gridPosition == null || gridPosition.isBlank()) {
            replyBattleships(event, "❌ You must specify a valid grid position to attack.").queue();
            return;
        }

        String normalizedGridPosition = gridPosition.toUpperCase(Locale.ROOT);
        if (!normalizedGridPosition.matches("^[A-J](10|[1-9])$")) {
            replyBattleships(event, "❌ The grid position must be between A1 and J10.").queue();
            return;
        }

        int x = normalizedGridPosition.charAt(0) - 'A';
        int y = Integer.parseInt(normalizedGridPosition.substring(1)) - 1;

        BattleshipsCommand.AttackResult attackResult = game.attack(user.getIdLong(), x, y);
        if (!attackResult.success()) {
            replyBattleships(event, "❌ " + attackResult.message()).queue();
            return;
        }

        var response = new StringBuilder();
        if (attackResult.hit()) {
            response.append("💥 ").append(user.getAsMention())
                    .append(" hit a ship at ").append(normalizedGridPosition).append('!');
            if (attackResult.sunk()) {
                response.append(" They sunk a ")
                        .append(attackResult.sunkType().name().toLowerCase(Locale.ROOT).replace('_', ' '))
                        .append('!');
                if(game.isPowerUpsEnabled()) {
                    BattleshipsCommand.PowerUp powerUp = game.grantRandomPowerUp(user.getIdLong());
                    if (powerUp != null) {
                        response.append(" ").append(user.getAsMention())
                                .append(" received a power-up: **").append(powerUp.getDisplayName()).append("**!");
                    }
                }
            }
        } else {
            response.append("🌊 ").append(user.getAsMention())
                    .append(" missed at ").append(normalizedGridPosition).append('.');
        }

        if (attackResult.gameOver()) {
            response.append("\n🏆 ").append(user.getAsMention()).append(" wins! Game over.");
            BattleshipsCommand.GAMES.remove(game.getThreadId(), game);
            event.getChannel().asThreadChannel().getManager().setLocked(true).setArchived(true).queueAfter(5, TimeUnit.SECONDS);
            try {
                String[] names = BattleshipsCommand.buildNames(event, game);
                FileUpload upload = BattleshipsImageRenderer.createUpload(
                        game, names, game.getPlayer1().getUserId(), game.getPlayer2().getUserId());
                replyBattleships(event, response.toString()).setFiles(upload).queue();
            } catch (Exception exception) {
                replyBattleships(event, response.toString()).queue();
            }

            return;
        }

        response.append("\nIt's now <@").append(attackResult.nextTurn()).append(">'s turn to attack.");
        try {
            String[] names = BattleshipsCommand.buildNames(event, game);
            FileUpload upload = BattleshipsImageRenderer.createUpload(game, names);
            replyBattleships(event, response.toString()).setFiles(upload).queue();
        } catch (Exception exception) {
            replyBattleships(event, response.toString()).queue();
        }
    }
}
