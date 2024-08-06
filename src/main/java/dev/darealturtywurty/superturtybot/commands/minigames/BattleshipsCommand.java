package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BattleshipsCommand extends CoreCommand {
    private static final int BOARD_SIZE = 10;

    public BattleshipsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "opponent", "The user you want to play against", false));
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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    public static class Game {
        @Getter
        private final long guildId, channelId, threadId, player1Id, player2Id;
        private final boolean isPvP;

        private final Battleship[] player1Ships = new Battleship[5];
        private final Battleship[] player2Ships = new Battleship[5];

        private final boolean[] player1Board = new boolean[BOARD_SIZE * BOARD_SIZE];
        private final boolean[] player2Board = new boolean[BOARD_SIZE * BOARD_SIZE];

        private long currentTurn;
        private boolean isPlayer1Ready = false, isPlayer2Ready = false;

        public Game(long guildId, long channelId, long threadId, long player1Id, long player2Id, boolean isPvP) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.threadId = threadId;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.isPvP = isPvP;

            this.currentTurn = player1Id;
        }

        public boolean isTurn(long userId) {
            return this.currentTurn == userId;
        }

        public void endTurn() {
            this.currentTurn = this.currentTurn == this.player1Id ? this.player2Id : this.player1Id;
        }

        public boolean isReady() {
            return this.isPlayer1Ready && this.isPlayer2Ready;
        }

        public void setReady(long userId) {
            if (userId == this.player1Id) {
                this.isPlayer1Ready = true;
            } else if (userId == this.player2Id) {
                this.isPlayer2Ready = true;
            }
        }

        public void setShips(long userId, Battleship[] ships) {
            if (userId == this.player1Id) {
                System.arraycopy(ships, 0, this.player1Ships, 0, 5);
            } else if (userId == this.player2Id) {
                System.arraycopy(ships, 0, this.player2Ships, 0, 5);
            }
        }

        public boolean isPlayer1(long userId) {
            return this.player1Id == userId;
        }

        public boolean isPlayer2(long userId) {
            return this.player2Id == userId;
        }

        public boolean isPvP() {
            return this.isPvP;
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
            return switch (orientation) {
                case LEFT_TO_RIGHT -> x + type.getSize() <= BOARD_SIZE;
                case RIGHT_TO_LEFT -> x - type.getSize() >= 0;
                case TOP_TO_BOTTOM -> y + type.getSize() <= BOARD_SIZE;
                case BOTTOM_TO_TOP -> y - type.getSize() >= 0;
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
        LEFT_TO_RIGHT, TOP_TO_BOTTOM, RIGHT_TO_LEFT, BOTTOM_TO_TOP;

        public Orientation getNext() {
            return Orientation.values()[(this.ordinal() + 1) % Orientation.values().length];
        }

        public Orientation getPrevious() {
            return Orientation.values()[(this.ordinal() - 1 + Orientation.values().length) % Orientation.values().length];
        }

        public int getX(int x, int size) {
            return switch (this) {
                case LEFT_TO_RIGHT -> x + size;
                case RIGHT_TO_LEFT -> x - size;
                default -> x;
            };
        }

        public int getY(int y, int size) {
            return switch (this) {
                case TOP_TO_BOTTOM -> y + size;
                case BOTTOM_TO_TOP -> y - size;
                default -> y;
            };
        }
    }
}
