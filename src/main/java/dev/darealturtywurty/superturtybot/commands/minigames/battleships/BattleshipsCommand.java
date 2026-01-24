package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import lombok.Getter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BattleshipsCommand extends CoreCommand {
    public static final Map<Long, Game> GAMES = new ConcurrentHashMap<>();
    public static final int BOARD_SIZE = 10;
    public static final String OPTION_ORIENTATION = "orientation";
    public static final String OPTION_SHIP_TYPE = "ship-type";
    public static final String OPTION_GRID_POSITION = "grid-position";

    public BattleshipsCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new BattleshipsHowToPlayCommand(),
                new BattleshipsPlayCommand(),
                new BattleshipsPlaceCommand(),
                new BattleshipsAttackCommand(),
                new BattleshipsRevealCommand()
        );
    }

    public static void checkAllShipsPlaced(MessageChannelUnion channel, Game game, User user) {
        if (game.hasPlacedAllShips(user.getIdLong())) {
            game.setReady(user.getIdLong(), true);
            channel.sendMessage("✅ " + user.getAsMention() + " has placed all their ships!").queue();

            if (game.isReady()) {
                channel.sendMessage("🚢 Both players have placed all their ships! Let the battle begin!\nIt's " +
                        (game.isPlayer1(game.getCurrentTurn()) ? "<@" + game.getPlayer1Id() + ">" : "<@" + game.getPlayer2Id() + ">") +
                        "'s turn to attack! Use the `/battleships attack` command to make your move.").queue();
            }
        }
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of battleships!";
    }

    @Override
    public String getName() {
        return "battleships";
    }

    @Override
    public String getRichName() {
        return "Battleships";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getSubcommandName() != null)
            return;

        reply(event, "❌ Please specify a valid subcommand.", false, true);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName()))
            return;

        if (!"place".equals(event.getSubcommandName())) {
            event.replyChoices().queue();
            return;
        }

        AutoCompleteQuery focused = event.getFocusedOption();
        String value = focused.getValue().toLowerCase(Locale.ROOT);

        switch (focused.getName()) {
            case OPTION_ORIENTATION -> event.replyChoiceStrings(filterSuggestions(List.of("horizontal", "vertical"), value)).queue();
            case OPTION_SHIP_TYPE -> {
                List<String> ships = new ArrayList<>();
                if (!event.isFromGuild() || event.getGuild() == null) {
                    for (ShipType type : ShipType.values()) {
                        ships.add(type.name().toLowerCase(Locale.ROOT));
                    }
                    event.replyChoiceStrings(filterSuggestions(ships, value)).queue();
                    return;
                }

                long userId = event.getUser().getIdLong();
                Game game = getGame(event.getGuild().getIdLong(), userId).orElse(null);
                for (ShipType type : ShipType.values()) {
                    if (game == null || game.canPlaceShip(userId, type)) {
                        ships.add(type.name().toLowerCase(Locale.ROOT));
                    }
                }
                event.replyChoiceStrings(filterSuggestions(ships, value)).queue();
            }
            case OPTION_GRID_POSITION -> {
                if (!event.isFromGuild() || event.getGuild() == null) {
                    event.replyChoices().queue();
                    return;
                }

                long userId = event.getUser().getIdLong();
                Game game = getGame(event.getGuild().getIdLong(), userId).orElse(null);
                if (game == null) {
                    event.replyChoices().queue();
                    return;
                }

                ShipType type = parseShipType(event.getOption(OPTION_SHIP_TYPE, null, OptionMapping::getAsString));
                Orientation orientation = parseOrientation(event.getOption(OPTION_ORIENTATION, null, OptionMapping::getAsString));

                List<ShipType> candidateTypes = new ArrayList<>();
                if (type != null) {
                    if (game.canPlaceShip(userId, type))
                        candidateTypes.add(type);
                } else {
                    for (ShipType shipType : ShipType.values()) {
                        if (game.canPlaceShip(userId, shipType))
                            candidateTypes.add(shipType);
                    }
                }

                List<Orientation> candidateOrientations = new ArrayList<>();
                if (orientation != null) {
                    candidateOrientations.add(orientation);
                } else {
                    candidateOrientations.add(Orientation.HORIZONTAL);
                    candidateOrientations.add(Orientation.VERTICAL);
                }

                boolean[][] valid = new boolean[BOARD_SIZE][BOARD_SIZE];
                for (ShipType shipType : candidateTypes) {
                    for (Orientation candidateOrientation : candidateOrientations) {
                        for (int row = 0; row < BOARD_SIZE; row++) {
                            for (int col = 0; col < BOARD_SIZE; col++) {
                                if (valid[col][row])
                                    continue;
                                PlacementResult result = game.canPlaceShipAt(userId, shipType, candidateOrientation, col, row);
                                if (result.success())
                                    valid[col][row] = true;
                            }
                        }
                    }
                }

                List<String> positions = new ArrayList<>();
                for (int row = 0; row < BOARD_SIZE; row++) {
                    for (int col = 0; col < BOARD_SIZE; col++) {
                        if (valid[col][row])
                            positions.add("%c%d".formatted('A' + col, row + 1));
                    }
                }

                event.replyChoiceStrings(filterSuggestions(positions, value.toUpperCase(Locale.ROOT))).queue();
            }
            default -> event.replyChoices().queue();
        }
    }

    private static List<String> filterSuggestions(List<String> options, String query) {
        List<String> suggestions = new ArrayList<>(25);
        String normalized = query == null ? "" : query.trim();
        for (String option : options) {
            if (normalized.isEmpty() || option.toLowerCase(Locale.ROOT).startsWith(normalized.toLowerCase(Locale.ROOT))) {
                suggestions.add(option);
                if (suggestions.size() >= 25)
                    break;
            }
        }

        return suggestions;
    }

    private static ShipType parseShipType(String value) {
        if (value == null || value.isBlank())
            return null;

        try {
            return ShipType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Orientation parseOrientation(String value) {
        if (value == null || value.isBlank())
            return null;

        if ("horizontal".equalsIgnoreCase(value))
            return Orientation.HORIZONTAL;
        if ("vertical".equalsIgnoreCase(value))
            return Orientation.VERTICAL;
        return null;
    }

    public static Optional<Game> getGame(long guildId, long userId) {
        return GAMES.values().stream()
                .filter(game -> game.getGuildId() == guildId && game.isPlayer(userId))
                .findFirst();
    }

    public static class Game {
        @Getter
        private final long guildId, channelId, threadId, player1Id, player2Id;
        @Getter
        private final boolean isPvP;

        private final Battleship[] player1Ships = new Battleship[ShipType.values().length];
        private final Battleship[] player2Ships = new Battleship[ShipType.values().length];

        private final boolean[] player1Board = new boolean[BOARD_SIZE * BOARD_SIZE];
        private final boolean[] player2Board = new boolean[BOARD_SIZE * BOARD_SIZE];

        private final AtomicLong currentTurn;
        private final AtomicBoolean isPlayer1Ready = new AtomicBoolean(false);
        private final AtomicBoolean isPlayer2Ready = new AtomicBoolean(false);

        public Game(long guildId, long channelId, long threadId, long player1Id, long player2Id, boolean isPvP) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.threadId = threadId;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.isPvP = isPvP;

            this.currentTurn = new AtomicLong(player1Id);
        }

        public long getCurrentTurn() {
            return this.currentTurn.get();
        }

        public boolean hasPlacedAllShips(long userId) {
            if (userId == this.player1Id) {
                for (Battleship ship : this.player1Ships) {
                    if (ship == null)
                        return false;
                }

                return true;
            } else if (userId == this.player2Id) {
                for (Battleship ship : this.player2Ships) {
                    if (ship == null)
                        return false;
                }

                return true;
            }

            return false;
        }

        public boolean isTurn(long userId) {
            return this.currentTurn.get() == userId;
        }

        public void endTurn() {
            this.currentTurn.set(this.currentTurn.get() == this.player1Id ? this.player2Id : this.player1Id);
        }

        public boolean isReady() {
            return this.isPlayer1Ready.get() && this.isPlayer2Ready.get();
        }

        public synchronized void setReady(long userId, boolean ready) {
            if (userId == this.player1Id) {
                this.isPlayer1Ready.set(ready);
            } else if (userId == this.player2Id) {
                this.isPlayer2Ready.set(ready);
            }
        }

        private int index(int x, int y) {
            return (y * BOARD_SIZE) + x;
        }

        public void markHit(long userId, int x, int y) {
            if (userId == this.player1Id) {
                this.player2Board[index(x, y)] = true;
            } else {
                this.player1Board[index(x, y)] = true;
            }
        }

        public boolean wasHit(long userId, int x, int y) {
            return userId == this.player1Id
                    ? this.player2Board[index(x, y)]
                    : this.player1Board[index(x, y)];
        }

        public boolean isPlayer1(long userId) {
            return this.player1Id == userId;
        }

        public boolean isPlayer2(long userId) {
            return this.player2Id == userId;
        }

        public boolean isPlayer(long userId) {
            return isPlayer1(userId) || isPlayer2(userId);
        }

        public long getOpponentId(long userId) {
            return isPlayer1(userId) ? this.player2Id : this.player1Id;
        }

        public Battleship[] getShips(long userId) {
            if (userId == this.player1Id)
                return Arrays.copyOf(this.player1Ships, this.player1Ships.length);

            if (userId == this.player2Id)
                return Arrays.copyOf(this.player2Ships, this.player2Ships.length);

            return new Battleship[0];
        }

        public boolean canPlaceShip(long userId, ShipType type) {
            Battleship[] ships = isPlayer1(userId) ? this.player1Ships : this.player2Ships;
            for (Battleship ship : ships) {
                if (ship != null && ship.getType() == type)
                    return false;
            }

            return true;
        }

        public PlacementResult canPlaceShipAt(long userId, ShipType type, Orientation orientation, int x, int y) {
            if (!isPlayer(userId))
                return PlacementResult.failure("You are not a player in this game.");

            if (!canPlaceShip(userId, type))
                return PlacementResult.failure("You have already placed that ship.");

            if (!Battleship.isWithinBounds(x, y, type, orientation))
                return PlacementResult.failure("That ship placement is out of bounds.");

            Battleship[] ships = isPlayer1(userId) ? this.player1Ships : this.player2Ships;
            int[][] newPositions = Battleship.getPositions(type, orientation, x, y);
            for (Battleship ship : ships) {
                if (ship == null)
                    continue;

                int[][] existingPositions = Battleship.getPositions(ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
                for (int[] newPosition : newPositions) {
                    for (int[] existingPosition : existingPositions) {
                        if (newPosition[0] == existingPosition[0] && newPosition[1] == existingPosition[1])
                            return PlacementResult.failure("That ship overlaps with one of your other ships.");
                    }
                }
            }

            return PlacementResult.SUCCESS;
        }

        public synchronized PlacementResult placeShip(long userId, ShipType type, Orientation orientation, int x, int y) {
            PlacementResult placementResult = canPlaceShipAt(userId, type, orientation, x, y);
            if (!placementResult.success())
                return placementResult;

            Battleship[] ships = isPlayer1(userId) ? this.player1Ships : this.player2Ships;
            int insertIndex = -1;
            for (int index = 0; index < ships.length; index++) {
                if (ships[index] == null) {
                    insertIndex = index;
                    break;
                }
            }

            if (insertIndex == -1)
                return PlacementResult.failure("You have already placed all of your ships.");

            ships[insertIndex] = new Battleship(type, orientation, x, y);
            return PlacementResult.SUCCESS;
        }

        public synchronized AttackResult attack(long attackerId, int x, int y) {
            AttackResult validation = isAttackValid(attackerId, x, y);
            if (!validation.success())
                return validation;

            Battleship[] targetShips = isPlayer1(attackerId) ? this.player2Ships : this.player1Ships;
            Battleship hitShip = findHitShip(targetShips, x, y);

            markHit(attackerId, x, y);

            boolean hit = hitShip != null;
            boolean sunk = false;
            ShipType sunkType = null;
            if (hit) {
                sunk = checkIfShipSunk(hitShip, attackerId);
                if (sunk) {
                    sunkType = hitShip.getType();
                }
            }

            boolean gameOver = false;
            if (hit) {
                gameOver = isGameOver(targetShips);
            }

            long nextTurn = this.currentTurn.get();
            if (!hit && !gameOver) {
                endTurn();
                nextTurn = this.currentTurn.get();
            }

            return new AttackResult(true, "Attack processed.", hit, sunk, sunkType, gameOver, nextTurn);
        }

        private AttackResult isAttackValid(long attackerId, int x, int y) {
            if (!isPlayer(attackerId))
                return AttackResult.failure("You are not a player in this game.");

            if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE)
                return AttackResult.failure("That attack position is out of bounds.");

            if (wasHit(attackerId, x, y))
                return AttackResult.failure("You have already attacked that position.");

            return new AttackResult(true, "", false, false, null, false, 0L);
        }

        private Battleship findHitShip(Battleship[] targetShips, int x, int y) {
            for (Battleship ship : targetShips) {
                if (ship == null)
                    continue;

                int[][] positions = Battleship.getPositions(ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
                for (int[] position : positions) {
                    if (position[0] == x && position[1] == y)
                        return ship;
                }
            }

            return null;
        }

        private boolean checkIfShipSunk(Battleship ship, long attackerId) {
            if (ship.isSunk())
                return true;

            int[][] positions = Battleship.getPositions(ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
            for (int[] position : positions) {
                if (!wasHit(attackerId, position[0], position[1]))
                    return false;
            }

            ship.sink();
            return true;
        }

        private boolean isGameOver(Battleship[] targetShips) {
            for (Battleship ship : targetShips) {
                if (ship != null && !ship.isSunk())
                    return false;
            }

            return true;
        }
    }

    @Getter
    public static class Battleship {
        private final ShipType type;
        private final Orientation orientation;
        private final int x, y;

        private boolean isSunk = false;

        public Battleship(ShipType type, Orientation orientation, int x, int y) {
            this.type = type;
            this.orientation = orientation;
            this.x = x;
            this.y = y;
        }

        public static int[][] getPositions(ShipType type, Orientation orientation, int x, int y) {
            final int[][] positions = new int[type.getSize()][2];
            for (int index = 0; index < type.getSize(); index++) {
                positions[index][0] = orientation.getX(x, index);
                positions[index][1] = orientation.getY(y, index);
            }

            return positions;
        }

        public static boolean isWithinBounds(int x, int y, ShipType type, Orientation orientation) {
            final int length = type.getSize() - 1;
            return switch (orientation) {
                case HORIZONTAL -> x + length < BOARD_SIZE;
                case VERTICAL -> y + length < BOARD_SIZE;
            };
        }

        public void sink() {
            this.isSunk = true;
        }
    }

    @Getter
    public enum ShipType {
        CARRIER(5), BATTLESHIP(4), DESTROYER(3), SUBMARINE(3), PATROL_BOAT(2);

        private final int size;

        ShipType(int size) {
            this.size = size;
        }
    }

    public enum Orientation {
        HORIZONTAL, VERTICAL;

        public Orientation getNext() {
            return Orientation.values()[(this.ordinal() + 1) % Orientation.values().length];
        }

        public Orientation getPrevious() {
            return Orientation.values()[(this.ordinal() - 1 + Orientation.values().length) % Orientation.values().length];
        }

        public int getX(int x, int size) {
            return this == Orientation.HORIZONTAL ? x + size : x;
        }

        public int getY(int y, int size) {
            return this == Orientation.VERTICAL ? y + size : y;
        }
    }

    public record PlacementResult(boolean success, String message) {
        public static final PlacementResult SUCCESS = new PlacementResult(true, "Ship placed successfully.");

        public static PlacementResult failure(String message) {
            return new PlacementResult(false, message);
        }
    }

    public record AttackResult(boolean success, String message, boolean hit, boolean sunk, ShipType sunkType,
                               boolean gameOver, long nextTurn) {
        public static AttackResult failure(String message) {
            return new AttackResult(false, message, false, false, null, false, 0L);
        }
    }
}
