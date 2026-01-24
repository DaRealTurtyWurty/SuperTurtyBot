package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class BattleshipsPlaceCommand extends BattleshipsSubcommand {
    public BattleshipsPlaceCommand() {
        super("place", "Place your ships on the battleships board", true);
        addOption(OptionType.STRING, BattleshipsCommand.OPTION_GRID_POSITION, "The grid position to place your ship (e.g., A5)", true, true);
        addOption(OptionType.STRING, BattleshipsCommand.OPTION_ORIENTATION, "The orientation of the ship (horizontal or vertical)", true, true);
        addOption(OptionType.STRING, BattleshipsCommand.OPTION_SHIP_TYPE, "The type of ship to place (e.g., destroyer, submarine)", true, true);
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
            replyBattleships(event, "❌ You can only place ships in the game thread: <#" + game.getThreadId() + ">").queue();
            return;
        }

        if (!game.isPlayer(user.getIdLong())) {
            replyBattleships(event, "❌ You are not a player in this game!").queue();
            return;
        }

        if (game.hasPlacedAllShips(user.getIdLong())) {
            replyBattleships(event, "❌ You have already placed all your ships!").queue();
            return;
        }

        String gridPosition = event.getOption(BattleshipsCommand.OPTION_GRID_POSITION, null, OptionMapping::getAsString);
        String orientation = event.getOption(BattleshipsCommand.OPTION_ORIENTATION, null, OptionMapping::getAsString);
        String shipType = event.getOption(BattleshipsCommand.OPTION_SHIP_TYPE, null, OptionMapping::getAsString);

        if (gridPosition == null || gridPosition.isBlank()) {
            replyBattleships(event, "❌ You must specify a valid grid position to place your ship.").queue();
            return;
        }

        String normalizedGridPosition = gridPosition.toUpperCase(Locale.ROOT);
        if (!normalizedGridPosition.matches("^[A-J](10|[1-9])$")) {
            var highlight = tryParseGridPosition(normalizedGridPosition);
            if (highlight != null) {
                String[] names = buildNames(event, game);
                try {
                    FileUpload upload = BattleshipsImageRenderer.createUploadWithHighlights(
                            game, names, user.getIdLong(), List.of(highlight), new Color(255, 120, 120, 160));
                    replyBattleships(event, "❌ The grid position must be between A1 and J10.").setFiles(upload).queue();
                } catch (IOException exception) {
                    replyBattleships(event, "❌ The grid position must be between A1 and J10.").queue();
                }
            } else {
                replyBattleships(event, "❌ The grid position must be between A1 and J10.").queue();
            }
            return;
        }

        if (orientation == null || orientation.isBlank()) {
            replyBattleships(event, "❌ You must specify a valid orientation (horizontal or vertical) to place your ship.").queue();
            return;
        }

        if (!orientation.equalsIgnoreCase("horizontal") && !orientation.equalsIgnoreCase("vertical")) {
            replyBattleships(event, "❌ The orientation must be either 'horizontal' or 'vertical'.").queue();
            return;
        }

        if (shipType == null || shipType.isBlank()) {
            replyBattleships(event, "❌ You must specify a valid ship type to place your ship.").queue();
            return;
        }

        BattleshipsCommand.ShipType type;
        try {
            type = BattleshipsCommand.ShipType.valueOf(shipType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            var joiner = new StringJoiner("`, `", "`", "`");
            for (BattleshipsCommand.ShipType ship : BattleshipsCommand.ShipType.values()) {
                joiner.add(ship.name().toLowerCase(Locale.ROOT));
            }

            replyBattleships(event, "❌ The ship type must be one of the following: %s".formatted(joiner.toString())).queue();
            return;
        }

        if (!game.canPlaceShip(user.getIdLong(), type)) {
            replyBattleships(event, "❌ You have already placed all your ships of type `%s`.".formatted(type.name().toLowerCase(Locale.ROOT))).queue();
            return;
        }

        int x = normalizedGridPosition.charAt(0) - 'A';
        int y = Integer.parseInt(normalizedGridPosition.substring(1)) - 1;
        BattleshipsCommand.Orientation orient = orientation.equalsIgnoreCase("horizontal")
                ? BattleshipsCommand.Orientation.HORIZONTAL
                : BattleshipsCommand.Orientation.VERTICAL;
        BattleshipsCommand.PlacementResult placementResult = game.placeShip(user.getIdLong(), type, orient, x, y);
        if (!placementResult.success()) {
            if ("That ship placement is out of bounds.".equals(placementResult.message())
                    || "That ship overlaps with one of your other ships.".equals(placementResult.message())) {
                List<Point> highlights = buildPlacementHighlights(type, orient, x, y);
                try {
                    String[] names = buildNames(event, game);
                    FileUpload upload = BattleshipsImageRenderer.createUploadWithHighlights(
                            game, names, user.getIdLong(), highlights, new Color(255, 120, 120, 160));
                    replyBattleships(event, "❌ " + placementResult.message()).setFiles(upload).queue();
                } catch (IOException exception) {
                    replyBattleships(event, "❌ " + placementResult.message()).queue();
                }
                return;
            }

            replyBattleships(event, "❌ " + placementResult.message()).queue();
            return;
        }

        try {
            String[] names = buildNames(event, game);
            FileUpload upload = BattleshipsImageRenderer.createUpload(game, names, user.getIdLong());
            replyBattleships(event, "✅ Successfully placed your `%s` at %s facing %s.".formatted(
                    type.name().toLowerCase(Locale.ROOT),
                    gridPosition.toUpperCase(Locale.ROOT),
                    orient.name().toLowerCase(Locale.ROOT)
            )).setFiles(upload).queue();
        } catch (IOException exception) {
            replyBattleships(event, "❌ An error occurred while generating the game board image.").queue();
            return;
        }

        BattleshipsCommand.checkAllShipsPlaced(event.getChannel(), game, user);
    }

    private static Point tryParseGridPosition(String value) {
        if (value == null || value.isBlank())
            return null;

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 2)
            return null;

        char letter = normalized.charAt(0);
        if (letter < 'A' || letter > 'Z')
            return null;

        String numberPart = normalized.substring(1);
        if (!numberPart.matches("\\d{1,2}"))
            return null;

        int row = Integer.parseInt(numberPart) - 1;
        int col = letter - 'A';
        if (col >= BattleshipsCommand.BOARD_SIZE || row < 0 || row >= BattleshipsCommand.BOARD_SIZE)
            return null;

        return new Point(col, row);
    }

    private static List<Point> buildPlacementHighlights(BattleshipsCommand.ShipType type,
                                                        BattleshipsCommand.Orientation orientation, int x, int y) {
        List<Point> points = new ArrayList<>();
        int[][] positions = BattleshipsCommand.Battleship.getPositions(type, orientation, x, y);
        for (int[] position : positions) {
            int px = position[0];
            int py = position[1];
            if (px < 0 || px >= BattleshipsCommand.BOARD_SIZE || py < 0 || py >= BattleshipsCommand.BOARD_SIZE)
                continue;

            points.add(new Point(px, py));
        }

        return points;
    }

    private static String[] buildNames(SlashCommandInteractionEvent event, BattleshipsCommand.Game game) {
        String player1 = resolveDisplayName(event, game.getPlayer1Id(), "Player 1");
        String player2 = resolveDisplayName(event, game.getPlayer2Id(), "Player 2");
        return new String[]{player1, player2};
    }
}
