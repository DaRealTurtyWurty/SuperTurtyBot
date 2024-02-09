package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.request.RandomWordRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WordSearchCommand extends CoreCommand {
    private static final List<Game> GAMES = new ArrayList<>();
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 50);

    public WordSearchCommand() {
        super(new Types(true, false, false, false));
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createEventWaiter(Game game, ThreadChannel thread) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event -> event.isFromGuild() && event.isFromThread()
                        && event.getGuild().getIdLong() == game.getGuildId()
                        && event.getChannel().getIdLong() == game.getThreadId()
                        && event.getAuthor().getIdLong() == game.getUserId()
                        && (event.getMessage().getContentRaw().split(" ").length == 1
                        || event.getMessage().getContentRaw().equalsIgnoreCase("give up")))
                .timeout(5, TimeUnit.MINUTES)
                .timeoutAction(() -> {
                    thread.sendMessage("❌ You took too long to find a word! Game over!")
                            .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                    GAMES.remove(game);
                })
                .failure(() -> {
                    thread.sendMessage("❌ Something went wrong! Game over!")
                            .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                    GAMES.remove(game);
                })
                .success(event -> {
                    String word = event.getMessage().getContentRaw().trim().toLowerCase(Locale.ROOT);
                    if (word.equalsIgnoreCase("give up")) {
                        thread.sendMessage("✅ Game over!")
                                .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                        GAMES.remove(game);
                        return;
                    }

                    if (game.getGuessedWords().contains(word)) {
                        createEventWaiter(game, thread).build();
                        return;
                    }

                    if (game.guess(word)) {
                        Optional<FileUpload> upload = createUpload(game);
                        if (upload.isEmpty()) {
                            thread.sendMessage("❌ Failed to create word search! Please try running the command again!")
                                    .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                            GAMES.remove(game);
                            return;
                        }

                        List<String> foundWords = game.getFoundWords();
                        if (foundWords.size() == game.getWordCount()) {
                            thread.sendMessage("✅ You found all the words! Game over!")
                                    .setFiles(upload.get())
                                    .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                            GAMES.remove(game);
                            return;
                        }

                        thread.sendMessage("✅ You found a word! " + (game.getWordCount() - foundWords.size()) + " left!")
                                .setFiles(upload.get())
                                .queue(ignored -> createEventWaiter(game, thread).build());
                    } else {
                        Optional<FileUpload> upload = createUpload(game);
                        if (upload.isEmpty()) {
                            thread.sendMessage("❌ Failed to create word search! Please try running the command again!")
                                    .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                            GAMES.remove(game);
                            return;
                        }

                        thread.sendMessage("❌ That is not a word! Try again!")
                                .setFiles(upload.get())
                                .queue(ignored -> createEventWaiter(game, thread).build());
                    }
                });
    }

    private static BufferedImage generateImage(Game game) {
        var image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 1000, 1000);

        graphics.setColor(Color.WHITE);
        graphics.fillRect(10, 100, 850, 800);

        graphics.setColor(Color.BLACK);
        // draw grid
        for (int i = 0; i < game.getSize() + 1; i++) {
            graphics.drawLine(25, 125 + (i * 70), 725, 125 + (i * 70));
            graphics.drawLine(25 + (i * 70), 125, 25 + (i * 70), 825);
        }

        graphics.setColor(Color.WHITE);
        graphics.setFont(TITLE_FONT);
        graphics.drawString("Word Search", 10, 50);

        graphics.setColor(Color.BLACK);
        FontMetrics metrics = graphics.getFontMetrics();
        char[][] board = game.getBoard();
        for (int i = 0; i < board.length; i++) {
            char[] row = board[i];
            for (int j = 0; j < row.length; j++) {
                char letter = row[j];
                graphics.drawString(
                        String.valueOf(letter),
                        62 + (j * 70) - metrics.charWidth(letter) / 2,
                        162 + (i * 70) + (int) (metrics.getHeight() / 2.5f));
            }
        }

        graphics.setColor(Color.RED);
        // circle found words
        for (String word : game.getFoundWords()) {
            circleWord(graphics, game, word);
        }

        graphics.dispose();
        return image;
    }

    private static void circleWord(Graphics2D graphics, Game game, String word) {
        FontMetrics metrics = graphics.getFontMetrics();
        List<Pair<Integer, Integer>> locations = game.getWordLocations().get(word);
        if (locations == null)
            return;

        graphics.setStroke(new BasicStroke(5));
        Pair<Integer, Integer> first = locations.getFirst();
        Pair<Integer, Integer> last = locations.getLast();
        Direction dir = Direction.fromCoordinates(first, last);

        switch (dir) {
            case RIGHT, DOWN: {
                int x = 50 + (first.getRight() * 70) - metrics.charWidth(word.charAt(0)) / 2;
                int y = 150 + (first.getLeft() * 70) - (int) (metrics.getHeight() / 2f) + metrics.getAscent() / 2;
                int width = 50 + (last.getRight() * 70) + metrics.charWidth(word.charAt(word.length() - 1)) - x;
                int height = 150 + (last.getLeft() * 70) + (int) (metrics.getHeight() * 0.75f) - y;
                graphics.drawRoundRect(x, y, width, height, 10, 10);
                break;
            }
            case LEFT, UP: {
                int x = 50 + (last.getRight()* 70) - metrics.charWidth(word.charAt(word.length() - 1)) / 2;
                int y = 150 + (last.getLeft()* 70) - (int) (metrics.getHeight() / 2.5f);
                int width = 50 + (first.getRight()* 70) + metrics.charWidth(word.charAt(0)) - x;
                int height = 150 + (first.getLeft()* 70) + (int) (metrics.getHeight() * 0.75f) - y;
                graphics.drawRoundRect(x, y, width, height, 10, 10);
                break;
            }
            // TODO: Figure out how to draw a diagonal rectangle
            case UP_RIGHT, DOWN_LEFT: {
                graphics.rotate(Math.toRadians(45), 0, 0);
                int x = 50 + (first.getRight()* 70) - metrics.charWidth(word.charAt(0)) / 2;
                int y = 150 + (first.getLeft()* 70) - (int) (metrics.getHeight() / 2.5f);
                int width = 50 + (last.getRight()* 70) + (int) (metrics.charWidth(word.charAt(word.length() - 1)) * 2.5f) - x;
                int height = 150 + (last.getLeft()* 70) + (int) (metrics.getHeight() * 0.75f) - y;
                graphics.drawRoundRect(x, y, width, height, 10, 10);
                graphics.rotate(Math.toRadians(-45), 0, 0);
            }
            case UP_LEFT, DOWN_RIGHT: {
                graphics.rotate(Math.toRadians(-45), 0, 0);
                int x = 50 + (first.getRight()* 70) - metrics.charWidth(word.charAt(0)) / 2;
                int y = 150 + (first.getLeft()* 70) - (int) (metrics.getHeight() / 2.5f);
                int width = 50 + (last.getRight()* 70) + (int) (metrics.charWidth(word.charAt(word.length() - 1)) * 2.5f) - x;
                int height = 150 + (last.getLeft()* 70) + (int) (metrics.getHeight() * 0.75f) - y;
                graphics.drawRoundRect(x, y, width, height, 10, 10);
                graphics.rotate(Math.toRadians(45), 0, 0);
            }
        }
    }

    private static Optional<FileUpload> createUpload(Game game) {
        BufferedImage image = generateImage(game);
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write image to byte array!", exception);
            return Optional.empty();
        }

        byte[] data = baos.toByteArray();
        return Optional.of(FileUpload.fromData(data, "word_search.png"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of word search!";
    }

    @Override
    public String getName() {
        return "wordsearch";
    }

    @Override
    public String getRichName() {
        return "Word Search";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (event.getChannelType().isThread()) {
            reply(event, "❌ This command cannot be used in a thread!", false, true);
            return;
        }

        if (!guild.getSelfMember().hasPermission(event.getGuildChannel(), Permission.CREATE_PUBLIC_THREADS, Permission.MANAGE_THREADS)) {
            reply(event, "❌ I do not have permission to create or manage threads in this channel!", false, true);
            return;
        }

        reply(event, "✅ Creating a game of word search...");

        if (GAMES.stream().anyMatch(game -> game.getGuildId() == guild.getIdLong()
                && game.getChannelId() == event.getChannel().getIdLong()
                && game.getUserId() == event.getUser().getIdLong())) {
            event.getHook().editOriginal("❌ There is already a game of word search in this channel!").queue();
            return;
        }

        event.getHook().editOriginal("✅ Game created!").queue(message -> {
            try {
                var game = new Game(10, 10,
                        guild.getIdLong(), event.getUser().getIdLong(), event.getChannel().getIdLong());
                GAMES.add(game);
                message.createThreadChannel(event.getUser().getEffectiveName() + "'s Word Search").queue(thread -> {
                    thread.addThreadMember(event.getUser()).queue();
                    game.setThreadId(thread.getIdLong());

                    Optional<FileUpload> upload = createUpload(game);
                    if (upload.isEmpty()) {
                        thread.sendMessage("❌ Failed to create word search! Please try running the command again!")
                                .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                        GAMES.remove(game);
                        return;
                    }

                    thread.sendMessage("✅ Here is your word search! Type the words you find in the thread!")
                            .setFiles(upload.get())
                            .queue(ignored -> createEventWaiter(game, thread).build());
                });
            } catch (IllegalStateException exception) {
                message.editMessage("❌ Failed to create word search! This usually happens when it cannot fit a word in the grid. Please try running the command again!")
                        .queue(ignored -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                RATELIMITS.put(event.getUser().getIdLong(), Pair.of(getName(), System.currentTimeMillis()));
            }
        });
    }

    private enum Direction {
        UP, RIGHT, DOWN, LEFT, UP_RIGHT, DOWN_RIGHT, DOWN_LEFT, UP_LEFT;

        public static Direction fromCoordinates(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
            int startRow = from.getLeft();
            int startColumn = from.getRight();
            int endRow = to.getLeft();
            int endColumn = to.getRight();

            if (startRow == endRow) {
                return startColumn < endColumn ? RIGHT : LEFT;
            } else if (startColumn == endColumn) {
                return startRow < endRow ? DOWN : UP;
            } else if (startRow < endRow) {
                return startColumn < endColumn ? DOWN_RIGHT : DOWN_LEFT;
            } else {
                return startColumn < endColumn ? UP_RIGHT : UP_LEFT;
            }
        }
    }

    @Getter
    public static class Game {
        private static final Random RANDOM = new Random();

        private final char[][] board;
        private final List<String> words, foundWords, guessedWords = new ArrayList<>();
        private final Map<String, List<Pair<Integer, Integer>>> wordLocations = new HashMap<>();

        private final int size;
        private final int wordCount;
        private final long guildId, userId, channelId;

        @Setter
        private long threadId;

        public Game(int size, int wordCount, long guildId, long userId, long channelId) throws IllegalStateException {
            if (size < 3)
                throw new IllegalStateException("Size must be greater than 3!");
            if (wordCount < 1)
                throw new IllegalStateException("Word count must be greater than 0!");
            if (wordCount > size * size)
                throw new IllegalStateException("Word count must be less than or equal to the size squared!");

            this.size = size;
            this.wordCount = wordCount;
            this.board = new char[size][size];

            this.words = getWords(size, wordCount);
            if (this.words.isEmpty())
                throw new IllegalStateException("Failed to get words for word search!");
            this.foundWords = new ArrayList<>(wordCount);

            this.guildId = guildId;
            this.userId = userId;
            this.channelId = channelId;

            fillBoard();
        }

        private static List<String> getWords(int size, int wordCount) {
            Either<List<String>, HttpStatus> response = ApiHandler.getWords(new RandomWordRequestData.Builder()
                    .length(3, size)
                    .amount(wordCount)
                    .build());
            if (response.isLeft())
                return List.copyOf(response.getLeft());
            else
                System.err.println("Failed to get words for word search! Status: " + response.getRight());

            return List.of();
        }

        private boolean isOutOfBounds(int row, int column) {
            return row < 0 || row >= this.size || column < 0 || column >= this.size;
        }

        private boolean hasCharacter(int row, int column, char character) {
            return this.board[row][column] == character;
        }

        private boolean isEmpty(int row, int column) {
            return hasCharacter(row, column, '\u0000');
        }

        private Direction guessBestDirection(int row, int column) {
            int up = 0, right = 0, down = 0, left = 0, upRight = 0, downRight = 0, downLeft = 0, upLeft = 0;
            for (String word : this.words) {
                if (canFitWord(word, row, column, Direction.UP))
                    up++;
                if (canFitWord(word, row, column, Direction.RIGHT))
                    right++;
                if (canFitWord(word, row, column, Direction.DOWN))
                    down++;
                if (canFitWord(word, row, column, Direction.LEFT))
                    left++;
                if (canFitWord(word, row, column, Direction.UP_RIGHT))
                    upRight++;
                if (canFitWord(word, row, column, Direction.DOWN_RIGHT))
                    downRight++;
                if (canFitWord(word, row, column, Direction.DOWN_LEFT))
                    downLeft++;
                if (canFitWord(word, row, column, Direction.UP_LEFT))
                    upLeft++;
            }

            int max = Math.max(Math.max(Math.max(Math.max(Math.max(Math.max(Math.max(up, right), down), left), upRight), downRight), downLeft), upLeft);
            if (max == up)
                return Direction.UP;
            if (max == right)
                return Direction.RIGHT;
            if (max == down)
                return Direction.DOWN;
            if (max == left)
                return Direction.LEFT;
            if (max == upRight)
                return Direction.UP_RIGHT;
            if (max == downRight)
                return Direction.DOWN_RIGHT;
            if (max == downLeft)
                return Direction.DOWN_LEFT;
            return Direction.UP_LEFT;
        }

        private void fillBoard() {
            for (String word : this.words) {
                int row = RANDOM.nextInt(this.size);
                int column = RANDOM.nextInt(this.size);
                Direction dir = guessBestDirection(row, column);
                boolean found = false;
                long start = System.currentTimeMillis();

                while (!found) {
                    if (canFitWord(word, row, column, dir)) {
                        found = true;

                        List<Pair<Integer, Integer>> locations = new ArrayList<>();
                        switch (dir) {
                            case UP -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row - i, column));
                                    this.board[row - i][column] = word.charAt(i);
                                }
                            }
                            case RIGHT -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row, column + i));
                                    this.board[row][column + i] = word.charAt(i);
                                }
                            }
                            case DOWN -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row + i, column));
                                    this.board[row + i][column] = word.charAt(i);
                                }
                            }
                            case LEFT -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row, column - i));
                                    this.board[row][column - i] = word.charAt(i);
                                }
                            }
                            case UP_RIGHT -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row - i, column + i));
                                    this.board[row - i][column + i] = word.charAt(i);
                                }
                            }
                            case DOWN_RIGHT -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row + i, column + i));
                                    this.board[row + i][column + i] = word.charAt(i);
                                }
                            }
                            case DOWN_LEFT -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row + i, column - i));
                                    this.board[row + i][column - i] = word.charAt(i);
                                }
                            }
                            case UP_LEFT -> {
                                for (int i = 0; i < word.length(); i++) {
                                    locations.add(Pair.of(row - i, column - i));
                                    this.board[row - i][column - i] = word.charAt(i);
                                }
                            }
                        }

                        this.wordLocations.put(word, locations);
                    } else {
                        row = RANDOM.nextInt(this.size);
                        column = RANDOM.nextInt(this.size);
                        dir = guessBestDirection(row, column);

                        if (System.currentTimeMillis() - start > 5000)
                            throw new IllegalStateException("Failed to find a place for the word '" + word + "'!");
                    }
                }
            }

            for (int i = 0; i < this.size; i++) {
                for (int j = 0; j < this.size; j++) {
                    if (isEmpty(i, j)) {
                        this.board[i][j] = (char) (RANDOM.nextInt(26) + 'a');
                    }
                }
            }
        }

        private boolean canFitWord(String word, int row, int column, Direction dir) {
            switch (dir) {
                case UP -> {
                    if (row - word.length() < 0)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row - i, column))
                            return false;

                        if (!isEmpty(row - i, column) && !hasCharacter(row - i, column, word.charAt(i)))
                            return false;
                    }
                }
                case RIGHT -> {
                    if (column + word.length() >= this.size)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row, column + i))
                            return false;

                        if (!isEmpty(row, column + i) && !hasCharacter(row, column + i, word.charAt(i)))
                            return false;
                    }
                }
                case DOWN -> {
                    if (row + word.length() >= this.size)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row + i, column))
                            return false;

                        if (!isEmpty(row + i, column) && !hasCharacter(row + i, column, word.charAt(i)))
                            return false;
                    }
                }
                case LEFT -> {
                    if (column - word.length() < 0)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row, column - i))
                            return false;

                        if (!isEmpty(row, column - i) && !hasCharacter(row, column - i, word.charAt(i)))
                            return false;
                    }
                }
                case UP_RIGHT -> {
                    if (row - word.length() < 0 || column + word.length() >= this.size)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row - i, column + i))
                            return false;

                        if (!isEmpty(row - i, column + i) && !hasCharacter(row - i, column + i, word.charAt(i)))
                            return false;
                    }
                }
                case DOWN_RIGHT -> {
                    if (row + word.length() >= this.size || column + word.length() >= this.size)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row + i, column + i))
                            return false;

                        if (!isEmpty(row + i, column + i) && !hasCharacter(row + i, column + i, word.charAt(i)))
                            return false;
                    }
                }
                case DOWN_LEFT -> {
                    if (row + word.length() >= this.size || column - word.length() < 0)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row + i, column - i))
                            return false;

                        if (!isEmpty(row + i, column - i) && !hasCharacter(row + i, column - i, word.charAt(i)))
                            return false;
                    }
                }
                case UP_LEFT -> {
                    if (row - word.length() < 0 || column - word.length() < 0)
                        return false;

                    for (int i = 0; i < word.length(); i++) {
                        if (isOutOfBounds(row - i, column - i))
                            return false;

                        if (!isEmpty(row - i, column - i) && !hasCharacter(row - i, column - i, word.charAt(i)))
                            return false;
                    }
                }
            }

            return true;
        }

        public boolean guess(String word) {
            if (this.guessedWords.contains(word))
                return false;

            if (this.words.contains(word)) {
                this.foundWords.add(word);
                this.guessedWords.add(word);
                return true;
            }

            return false;
        }
    }
}
