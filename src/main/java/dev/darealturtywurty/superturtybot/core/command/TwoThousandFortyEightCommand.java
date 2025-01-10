package dev.darealturtywurty.superturtybot.core.command;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.TwoThousandFortyEightProfile;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class TwoThousandFortyEightCommand extends CoreCommand {
    private static final Font FONT = new Font("Arial", Font.PLAIN, 100);
    private static final Color BACKGROUND_COLOR = new Color(0xBBADA0);
    private static final Color[] COLORS = {
            new Color(0xCDC1B4), // empty
            new Color(0xEEE4DA), // 2
            new Color(0xEDE0C8), // 4
            new Color(0xF2B179), // 8
            new Color(0xF59563), // 16
            new Color(0xF67C5F), // 32
            new Color(0xF65E3B), // 64
            new Color(0xEDCF72), // 128
            new Color(0xEDCC61), // 256
            new Color(0xEDC850), // 512
            new Color(0xEDC53F), // 1024
            new Color(0xEDC22E), // 2048
            new Color(0x3C3A32), // more than 2048
    };

    public TwoThousandFortyEightCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of 2048";
    }

    @Override
    public String getName() {
        return "2048";
    }

    @Override
    public String getRichName() {
        return "2048";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        var game = new Game();
        event.reply("✅ Created a game of 2048!")
                .setComponents(createButtons(game))
                .setFiles(createFileUpload(game, event.getHook()))
                .flatMap(InteractionHook::retrieveOriginal)
                .queue(message -> createEventWaiter(event, message, game).build());
    }

    private static List<ActionRow> createButtons(Game game) {
        return List.of(
                ActionRow.of(
                        Button.secondary("2048:disabled1", "\u0000").asDisabled(),
                        Button.primary("2048:up", "▲").withDisabled(!game.canMove("up")),
                        Button.secondary("2048:disabled2", "\u0000").asDisabled()),
                ActionRow.of(
                        Button.primary("2048:left", "◀").withDisabled(!game.canMove("left")),
                        Button.secondary("2048:disabled3", "\u0000").asDisabled(),
                        Button.primary("2048:right", "▶").withDisabled(!game.canMove("right"))),
                ActionRow.of(
                        Button.secondary("2048:disabled4", "\u0000").asDisabled(),
                        Button.primary("2048:down", "▼").withDisabled(!game.canMove("down")),
                        Button.secondary("2048:disabled5", "\u0000").asDisabled()));
    }

    private EventWaiter.Builder<ButtonInteractionEvent> createEventWaiter(SlashCommandInteractionEvent event, Message message, Game game) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(buttonEvent ->
                        buttonEvent.getUser() == event.getUser() &&
                        buttonEvent.getMessageIdLong() == message.getIdLong() &&
                                buttonEvent.getChannelIdLong() == message.getChannelIdLong() &&
                                (buttonEvent.getComponentId().equals("2048:up") ||
                                buttonEvent.getComponentId().equals("2048:down") ||
                                buttonEvent.getComponentId().equals("2048:left") ||
                                buttonEvent.getComponentId().equals("2048:right")))
                .timeout(5, TimeUnit.MINUTES)
                .timeoutAction(() -> event.getHook().editOriginal("❌ Game timed out!\nScore: %d\nBest score: %d"
                                .formatted(game.currentScore, getProfile(event.getUser().getIdLong()).bestScore))
                        .setComponents()
                        .queue())
                .success(buttonEvent -> handleButtonClicked(event, buttonEvent, message, game));
    }

    private static TwoThousandFortyEightProfile getProfile(long userId) {
        TwoThousandFortyEightProfile twoThousandFortyEightProfile = Database.getDatabase().twoThousandFortyEight.find(Filters.eq("user", userId)).first();
        if (twoThousandFortyEightProfile == null) {
            twoThousandFortyEightProfile = new TwoThousandFortyEightProfile(userId, 0);
            Database.getDatabase().twoThousandFortyEight.insertOne(twoThousandFortyEightProfile);
        }

        return twoThousandFortyEightProfile;
    }

    private void handleButtonClicked(SlashCommandInteractionEvent event, ButtonInteractionEvent buttonEvent, Message message, Game game) {
        String[] split = buttonEvent.getComponentId().split(":");
        game.makeMove(split[1]);

        long userId = event.getUser().getIdLong();
        TwoThousandFortyEightProfile profile = getProfile(userId);
        if (profile.bestScore < game.currentScore) {
            profile.bestScore = game.currentScore;
            Database.getDatabase().twoThousandFortyEight.replaceOne(Filters.eq("user", userId), profile);
        }

        List<ActionRow> buttons = createButtons(game);
        boolean allArrowButtonsDisabled = buttons.stream()
                .flatMap(itemComponents -> itemComponents.getButtons().stream())
                .filter(button -> !button.getLabel().equals("\u0000"))
                .allMatch(ActionComponent::isDisabled);
        if (!allArrowButtonsDisabled) {
            createEventWaiter(event, message, game).build();
        }
        buttonEvent.editMessage("%sScore: %d\nBest score: %d".formatted(allArrowButtonsDisabled ? "The game is over!\n" : "", game.currentScore, profile.bestScore))
                .setFiles(createFileUpload(game, buttonEvent.getHook()))
                .setComponents(allArrowButtonsDisabled ? List.of() : buttons)
                .queue();
    }


    private static FileUpload createFileUpload(Game game, InteractionHook hook) {
        BufferedImage image = createImage(game);

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write image!", exception);
            hook.editOriginal("❌ Something went wrong while creating the image! The game has been cancelled!")
                    .setComponents()
                    .setFiles()
                    .queue();
            return null;
        }

        return FileUpload.fromData(baos.toByteArray(), "2048.png");
    }

    public static BufferedImage createImage(Game game) {
        BufferedImage image = new BufferedImage(990, 990, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, 990, 990);
        graphics.setFont(FONT);
        FontMetrics fontMetrics = graphics.getFontMetrics();

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                int squareValue = game.board[y][x];
                Color color = getColor(squareValue);

                graphics.setColor(color);
                graphics.fillRect(x * 242 + 22, y * 242 + 22, 220, 220);

                if (squareValue == 0) continue;

                graphics.setColor(squareValue >= 8 ?
                        new Color(0xF9F6F2) :
                        new Color(0x776E65));
                String squareValueStr = String.valueOf(squareValue);
                int width = fontMetrics.stringWidth(squareValueStr);
                AffineTransform originalTransform = graphics.getTransform();
                graphics.translate(x * 242 + 22 + 110, y * 242 + 22 + 110);
                double digits = Math.max(3.5, squareValueStr.length());
                graphics.scale(3.5 / digits, 3.5 / digits);
                graphics.drawString(squareValueStr, -width / 2, 36);
                graphics.setTransform(originalTransform);
            }
        }
        return image;
    }

    private static Color getColor(int squareValue) {
        int colorIndex = switch (squareValue) {
            case 0 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            case 8 -> 3;
            case 16 -> 4;
            case 32 -> 5;
            case 64 -> 6;
            case 128 -> 7;
            case 256 -> 8;
            case 512 -> 9;
            case 1024 -> 10;
            case 2048 -> 11;
            default -> 12;
        };
        return COLORS[colorIndex];
    }

    public static class Game {
        private final int[][] board = new int[4][4];
        public int currentScore;

        public Game() {
            initBoard();
        }

        private void initBoard() {
            spawnTileAtRandomPos(board);
            spawnTileAtRandomPos(board);
        }

        private void makeMove(String direction) {
            int[][] originalBoard = Arrays.stream(board)
                    .map(int[]::clone)
                    .toArray(int[][]::new);
            makeMove(direction, board, addition -> currentScore += addition);
            if (!Arrays.deepEquals(originalBoard, board)) {
                spawnTileAtRandomPos(board);
            }
        }

        private static void makeMove(String direction, int[][] board, Consumer<Integer> addScore) {
            compress(direction, board);
            merge(direction, board, addScore);
            compress(direction, board);
        }

        private static void spawnTileAtRandomPos(int[][] board) {
            while (true) {
                int tileIndex = ThreadLocalRandom.current().nextInt(16);
                int x = tileIndex % 4;
                int y = tileIndex / 4;
                if (board[y][x] == 0) {
                    board[y][x] = ThreadLocalRandom.current().nextInt(10) == 0 ? 4 : 2;
                    break;
                }
            }
        }

        private static void compress(String direction, int[][] board) {
            for (int i = 0; i < 4; i++) {
                int finalI = i;
                switch (direction) {
                    case "up" -> moveZerosToEnd(y -> board[y][finalI], (y, value) -> board[y][finalI] = value);
                    case "down" -> moveZerosToEnd(y -> board[3 - y][finalI], (y, value) -> board[3 - y][finalI] = value);
                    case "left" -> moveZerosToEnd(x -> board[finalI][x], (x, value) -> board[finalI][x] = value);
                    case "right" -> moveZerosToEnd(x -> board[finalI][3 - x], (x, value) -> board[finalI][3 - x] = value);
                }
            }
        }

        private static void merge(String direction, int[][] board, Consumer<Integer> addScore) {
            for (int i = 0; i < 4; i++) {
                int finalI = i;
                switch (direction) {
                    case "up" -> mergeEqualTiles(
                            y -> board[y][finalI],
                            (y, value) -> board[y][finalI] = value,
                            addScore);
                    case "down" -> mergeEqualTiles(
                            y -> board[3 - y][finalI],
                            (y, value) -> board[3 - y][finalI] = value,
                            addScore);
                    case "left" -> mergeEqualTiles(
                            x -> board[finalI][x],
                            (x, value) -> board[finalI][x] = value,
                            addScore);
                    case "right" -> mergeEqualTiles(
                            x -> board[finalI][3 - x],
                            (x, value) -> board[finalI][3 - x] = value,
                            addScore);
                }
            }
        }

        public static void moveZerosToEnd(Function<Integer, Integer> getValue, BiConsumer<Integer, Integer> setValue) {
            int index = 0; // Index to place the next non-zero element

            // Move all non-zero elements to the front
            for (int i = 0; i < 4; i++) {
                int num = getValue.apply(i);
                if (num != 0) {
                    setValue.accept(index++, num);
                }
            }

            // Fill the remaining positions with zeros
            while (index < 4) {
                setValue.accept(index++, 0);
            }
        }

        public static void mergeEqualTiles(Function<Integer, Integer> getValue, BiConsumer<Integer, Integer> setValue, Consumer<Integer> addScore) {
            for (int i = 0; i < 3; i++) {
                int num = getValue.apply(i);
                int nextNum = getValue.apply(i + 1);
                if (num == nextNum) {
                    int newTile = num * 2;
                    addScore.accept(newTile);
                    setValue.accept(i, newTile);
                    setValue.accept(i + 1, 0);
                }
            }
        }

        public boolean canMove(String direction) {
            int[][] boardToModify = Arrays.stream(board)
                    .map(int[]::clone)
                    .toArray(int[][]::new);
            makeMove(direction, boardToModify, addition -> {});
            return !Arrays.deepEquals(boardToModify, board);
        }
    }
}
