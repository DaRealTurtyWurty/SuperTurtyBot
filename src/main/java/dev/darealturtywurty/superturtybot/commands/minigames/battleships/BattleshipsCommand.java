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
import reactor.function.Function4;

import java.util.*;
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
                new BattleshipsRevealCommand(),
                new BattleshipsPowerUpCommand()
        );
    }

    public static void checkAllShipsPlaced(MessageChannelUnion channel, Game game, User user) {
        if (game.hasPlacedAllShips(user.getIdLong())) {
            game.setReady(user.getIdLong(), true);
            channel.sendMessage("✅ " + user.getAsMention() + " has placed all their ships!").queue();

            if (game.isReady()) {
                channel.sendMessage("🚢 Both players have placed all their ships! Let the battle begin!\nIt's " +
                        (game.isPlayer1(game.getCurrentTurn()) ? "<@" + game.player1.getUserId() + ">" : "<@" + game.player2.getUserId() + ">") +
                        "'s turn to attack! Use the `/battleships attack` command to make your move.").queue();
            }
        }
    }

    public static String[] buildNames(SlashCommandInteractionEvent event, Game game) {
        String player1 = BattleshipsSubcommand.resolveDisplayName(event, game.player1.getUserId(), "Player 1");
        String player2 = BattleshipsSubcommand.resolveDisplayName(event, game.player2.getUserId(), "Player 2");
        return new String[]{player1, player2};
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
            case OPTION_ORIENTATION ->
                    event.replyChoiceStrings(filterSuggestions(List.of("horizontal", "vertical"), value)).queue();
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

    public static class Player {
        @Getter
        private final long userId;
        @Getter
        private final Set<PowerUp> powerUps = new HashSet<>();
        private final AtomicBoolean isReady = new AtomicBoolean(false);
        private final Battleship[] ships = new Battleship[ShipType.values().length];
        private final boolean[] board = new boolean[BOARD_SIZE * BOARD_SIZE];
        private final boolean[] scanned = new boolean[BOARD_SIZE * BOARD_SIZE];

        public Player(long userId) {
            this.userId = userId;
        }

        public boolean isReady() {
            return this.isReady.get();
        }

        public boolean hasPlacedAllShips() {
            for (Battleship ship : this.ships) {
                if (ship == null)
                    return false;
            }

            return true;
        }

        public void markHit(int x, int y) {
            this.board[(y * BOARD_SIZE) + x] = true;
        }

        public void markScanned(int x, int y) {
            this.scanned[(y * BOARD_SIZE) + x] = true;
        }

        public void markRepaired(int x, int y) {
            this.board[(y * BOARD_SIZE) + x] = false;
        }

        public void repairShip(Battleship ship, int x, int y) {
            int[][] positions = Battleship.getPositions(ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
            for (int[] position : positions) {
                if (position[0] == x && position[1] == y) {
                    ship.repair();
                    markRepaired(x, y);
                    break;
                }
            }
        }

        public boolean isHit(int x, int y) {
            return this.board[(y * BOARD_SIZE) + x];
        }

        public boolean isScanned(int x, int y) {
            return this.scanned[(y * BOARD_SIZE) + x];
        }

        public boolean canPlaceShip(ShipType type) {
            for (Battleship ship : this.ships) {
                if (ship != null && ship.getType() == type)
                    return false;
            }

            return true;
        }
    }

    public static class Game {
        @Getter
        private final long guildId, channelId, threadId;
        @Getter
        private final boolean isPvP;
        @Getter
        private final Player player1, player2;
        private final AtomicLong currentTurn;

        @Getter
        private final boolean powerUpsEnabled;

        public Game(long guildId, long channelId, long threadId, long player1Id, long player2Id, boolean isPvP, boolean powerUpsEnabled) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.threadId = threadId;
            this.player1 = new Player(player1Id);
            this.player2 = new Player(player2Id);
            this.isPvP = isPvP;
            this.powerUpsEnabled = powerUpsEnabled;

            this.currentTurn = new AtomicLong(player1Id);
        }

        public long getCurrentTurn() {
            return this.currentTurn.get();
        }

        public boolean hasPlacedAllShips(long userId) {
            if (userId == this.player1.getUserId())
                return this.player1.hasPlacedAllShips();

            if (userId == this.player2.getUserId())
                return this.player2.hasPlacedAllShips();

            return false;
        }

        public boolean isTurn(long userId) {
            return this.currentTurn.get() == userId;
        }

        public void endTurn() {
            this.currentTurn.set(this.currentTurn.get() == this.player1.getUserId()
                    ? this.player2.getUserId()
                    : this.player1.getUserId());
        }

        public boolean isReady() {
            return this.player1.isReady() && this.player2.isReady();
        }

        public synchronized void setReady(long userId, boolean ready) {
            if (userId == this.player1.getUserId()) {
                this.player1.isReady.set(ready);
            } else if (userId == this.player2.getUserId()) {
                this.player2.isReady.set(ready);
            }
        }

        private int index(int x, int y) {
            return (y * BOARD_SIZE) + x;
        }

        public void markHit(long userId, int x, int y) {
            if (userId == this.player1.getUserId()) {
                this.player2.markHit(x, y);
            } else {
                this.player1.markHit(x, y);
            }
        }

        public boolean wasHit(long userId, int x, int y) {
            return userId == this.player1.getUserId()
                    ? this.player2.isHit(x, y)
                    : this.player1.isHit(x, y);
        }

        public boolean isPlayer1(long userId) {
            return this.player1.getUserId() == userId;
        }

        public boolean isPlayer2(long userId) {
            return this.player2.getUserId() == userId;
        }

        public boolean isPlayer(long userId) {
            return isPlayer1(userId) || isPlayer2(userId);
        }

        public long getOpponentId(long userId) {
            return isPlayer1(userId) ? this.player2.getUserId() : this.player1.getUserId();
        }

        public Battleship[] getShips(long userId) {
            return isPlayer1(userId) ? this.player1.ships : this.player2.ships;
        }

        public boolean canPlaceShip(long userId, ShipType type) {
            return isPlayer1(userId)
                    ? this.player1.canPlaceShip(type)
                    : this.player2.canPlaceShip(type);
        }

        public PlacementResult canPlaceShipAt(long userId, ShipType type, Orientation orientation, int x, int y) {
            if (!isPlayer(userId))
                return PlacementResult.failure("You are not a player in this game.");

            if (!canPlaceShip(userId, type))
                return PlacementResult.failure("You have already placed that ship.");

            if (!Battleship.isWithinBounds(x, y, type, orientation))
                return PlacementResult.failure("That ship placement is out of bounds.");

            Battleship[] ships = isPlayer1(userId) ? this.player1.ships : this.player2.ships;
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

            Battleship[] ships = isPlayer1(userId) ? this.player1.ships : this.player2.ships;
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
            return attackInternal(attackerId, x, y, true);
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

        public PowerUpResult usePowerUp(long userId, PowerUp powerUp, int x, int y) {
            if (!powerUpsEnabled)
                return PowerUpResult.failure("Power-ups are not enabled for this game.");

            if (!isPlayer(userId))
                return PowerUpResult.failure("You are not a player in this game.");

            Set<PowerUp> powerUps = isPlayer1(userId)
                    ? this.player1.powerUps
                    : this.player2.powerUps;
            if (!powerUps.contains(powerUp))
                return PowerUpResult.failure("You do not have that power-up available.");

            PowerUpResult result = powerUp.use(this, userId, x, y);
            if (!result.success())
                return result;

            powerUps.remove(powerUp);
            return result;
        }

        public boolean hasPowerUp(long userId, PowerUp powerUp) {
            return isPlayer1(userId)
                    ? this.player1.powerUps.contains(powerUp)
                    : this.player2.powerUps.contains(powerUp);
        }

        public boolean hasScannedPosition(long userId, int scanX, int scanY) {
            return isPlayer1(userId)
                    ? this.player2.isScanned(scanX, scanY)
                    : this.player1.isScanned(scanX, scanY);
        }

        public void markScannedPosition(long userId, int scanX, int scanY) {
            if (isPlayer1(userId)) {
                this.player2.markScanned(scanX, scanY);
            } else {
                this.player1.markScanned(scanX, scanY);
            }
        }

        public void markRepaired(long userId, int x, int y) {
            if (isPlayer1(userId)) {
                this.player1.markRepaired(x, y);
            } else if (isPlayer2(userId)) {
                this.player2.markRepaired(x, y);
            }
        }

        public PowerUp grantRandomPowerUp(long userId) {
            PowerUp[] powerUps = PowerUp.values();
            int owned = 0;
            for (PowerUp powerUp : powerUps) {
                if (hasPowerUp(userId, powerUp))
                    owned++;
            }
            if (owned >= powerUps.length) {
                return null;
            }
            PowerUp granted;
            var random = new Random();
            do {
                granted = powerUps[random.nextInt(powerUps.length)];
            } while (hasPowerUp(userId, granted));

            if (isPlayer1(userId)) {
                this.player1.powerUps.add(granted);
            } else {
                this.player2.powerUps.add(granted);
            }

            return granted;
        }

        public boolean isGameOver(long attackerId) {
            Battleship[] targetShips = isPlayer1(attackerId) ? this.player2.ships : this.player1.ships;
            return isGameOver(targetShips);
        }

        public synchronized AttackResult attackWithoutTurn(long attackerId, int x, int y) {
            return attackInternal(attackerId, x, y, false);
        }

        private synchronized AttackResult attackInternal(long attackerId, int x, int y, boolean endTurnOnMiss) {
            AttackResult validation = isAttackValid(attackerId, x, y);
            if (!validation.success())
                return validation;

            Battleship[] targetShips = isPlayer1(attackerId) ? this.player2.ships : this.player1.ships;
            Battleship hitShip = findHitShip(targetShips, x, y);
            if (hitShip != null && hitShip.isHasShield()) {
                hitShip.deactivateShield();
                markScannedPosition(attackerId, x, y); // Mark as scanned to show that the ship has been hit (but not actually hit)
                return new AttackResult(true, "Attack processed. The ship's shield absorbed the hit!", false, false, null, false, this.currentTurn.get());
            }

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
            if (!hit && endTurnOnMiss) {
                endTurn();
                nextTurn = this.currentTurn.get();
            }

            return new AttackResult(true, "Attack processed.", hit, sunk, sunkType, gameOver, nextTurn);
        }
    }

    @Getter
    public static class Battleship {
        private final ShipType type;
        private final Orientation orientation;
        private final int x, y;

        private boolean isSunk = false;
        private boolean hasShield = false;

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

        public void repair() {
            this.isSunk = false;
        }

        public void activateShield() {
            this.hasShield = true;
        }

        public void deactivateShield() {
            this.hasShield = false;
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

    public record PowerUpResult(boolean success, String message) {
        public static PowerUpResult failure(String message) {
            return new PowerUpResult(false, message);
        }

        public static final PowerUpResult SUCCESS = new PowerUpResult(true, "Power-up used successfully.");
    }

    @Getter
    public enum PowerUp {
        SONAR_SCAN("Sonar Scan", true, BattleshipsCommand.PowerUp::useSonarScan),
        AIRSTRIKE("Airstrike", true, BattleshipsCommand.PowerUp::useAirstrike),
        REPAIR_SHIP("Repair Ship", false, BattleshipsCommand.PowerUp::useRepairShip),
        SHIELD("Shield", false, BattleshipsCommand.PowerUp::useShield);

        private final String displayName;
        private final boolean shouldShowOtherPlayerPosition;
        private final Function4<Game, Long, Integer, Integer, PowerUpResult> action;

        PowerUp(String displayName, boolean shouldShowOtherPlayerPosition,
                Function4<Game, Long, Integer, Integer, PowerUpResult> action) {
            this.displayName = displayName;
            this.shouldShowOtherPlayerPosition = shouldShowOtherPlayerPosition;
            this.action = action;
        }

        private static PowerUpResult useSonarScan(Game game, long userId, int x, int y) {
            int scanned = 0;
            for (int scanX = x - 1; scanX <= x + 1; scanX++) {
                for (int scanY = y - 1; scanY <= y + 1; scanY++) {
                    if (scanX < 0 || scanX >= BOARD_SIZE || scanY < 0 || scanY >= BOARD_SIZE)
                        continue;

                    if (!game.hasScannedPosition(userId, scanX, scanY)) {
                        game.markScannedPosition(userId, scanX, scanY);
                        scanned++;
                    }
                }
            }

            return scanned == 0
                    ? PowerUpResult.failure("All positions in the scan area have already been scanned.")
                    : PowerUpResult.SUCCESS;
        }

        private static PowerUpResult useAirstrike(Game game, long userId, int x, int y) {
            int hits = 0;
            int attacked = 0;
            for (int strikeX = x - 1; strikeX <= x + 1; strikeX++) {
                for (int strikeY = y - 1; strikeY <= y + 1; strikeY++) {
                    if (strikeX < 0 || strikeX >= BOARD_SIZE || strikeY < 0 || strikeY >= BOARD_SIZE)
                        continue;

                    if (!game.wasHit(userId, strikeX, strikeY)) {
                        attacked++;
                        AttackResult attackResult = game.attackWithoutTurn(userId, strikeX, strikeY);
                        if (attackResult.success() && attackResult.hit()) {
                            hits++;
                        }
                    }
                }
            }

            return attacked == 0
                    ? PowerUpResult.failure("All positions in the airstrike area have already been attacked.")
                    : PowerUpResult.SUCCESS;
        }

        private static PowerUpResult useRepairShip(Game game, long userId, int x, int y) {
            Battleship[] ships = game.getShips(userId);
            for (Battleship ship : ships) {
                if (ship == null)
                    continue;

                int[][] positions = Battleship.getPositions(ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
                for (int[] position : positions) {
                    if (position[0] == x && position[1] == y) {
                        if (!ship.isSunk())
                            return PowerUpResult.failure("The ship at that position is not sunk.");

                        ship.repair();
                        int[][] repairPositions = Battleship.getPositions(
                                ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
                        for (int[] repairPosition : repairPositions) {
                            game.markRepaired(userId, repairPosition[0], repairPosition[1]);
                        }
                        return PowerUpResult.SUCCESS;
                    }
                }
            }

            return PowerUpResult.failure("No ship found at the specified position.");
        }

        private static PowerUpResult useShield(Game game, long userId, int x, int y) {
            return PowerUpResult.failure("Shield is not implemented yet.");
        }

        public PowerUpResult use(Game game, long userId, int x, int y) {
            return this.action.apply(game, userId, x, y);
        }
    }
}
