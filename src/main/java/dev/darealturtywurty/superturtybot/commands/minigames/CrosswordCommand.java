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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class CrosswordCommand extends CoreCommand {
    private static final Font FONT = new Font("Arial", Font.PLAIN, 50);
    private static final Stroke STROKE = new BasicStroke(5);

    private static final List<Game> GAMES = new ArrayList<>();

    public CrosswordCommand() {
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


                });
    }

    private static BufferedImage generateImage(Game game) {
        var image = new BufferedImage(game.board.length * 100, game.board.length * 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(FONT);

        FontMetrics metrics = graphics.getFontMetrics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setColor(Color.BLACK);
        for (int x = 0; x < game.board.length; x++) {
            for (int y = 0; y < game.board[x].length; y++) {
                char letter = game.get(x, y);
                if (letter != '\u0000') {
                    graphics.drawString(String.valueOf(letter), 10 + x * 100 + metrics.charWidth(letter) / 2, 10 + y * 100 + metrics.getAscent() / 2);
                }
            }
        }

        // draw rectangles around each letter
        graphics.setColor(Color.BLACK);
        graphics.setStroke(STROKE);
        for (int x = 0; x < game.board.length; x++) {
            for (int y = 0; y < game.board[x].length; y++) {
                var letter = game.get(x, y);
                if (letter != '\u0000')
                    graphics.drawRect(x * 100, y * 100, 100, 100);
            }
        }

        graphics.dispose();
        return image;
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
        return "Play crossword!";
    }

    @Override
    public String getName() {
        return "crossword";
    }

    @Override
    public String getRichName() {
        return "Crossword";
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

        reply(event, "✅ Creating a crossword game...");

        if (GAMES.stream().anyMatch(game -> game.getGuildId() == guild.getIdLong()
                && game.getChannelId() == event.getChannel().getIdLong()
                && game.getUserId() == event.getUser().getIdLong())) {
            event.getHook().editOriginal("❌ You already have a crossword game in this server!").queue();
            return;
        }

        event.getHook().editOriginal("✅ Game created!").queue(message -> {
            try {
                var game = new Game(10, guild.getIdLong(), event.getUser().getIdLong(), event.getChannel().getIdLong());
                //GAMES.add(game);
                message.createThreadChannel(event.getUser().getEffectiveName() + "'s Crossword Game").queue(thread -> {
                    thread.addThreadMember(event.getUser()).queue();
                    game.setThreadId(thread.getIdLong());

                    Optional<FileUpload> upload = createUpload(game);
                    if (upload.isEmpty()) {
                        thread.sendMessage("❌ Failed to create crossword! Please try running the command again!")
                                .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                        GAMES.remove(game);
                        return;
                    }

                    thread.sendMessage("✅ Here is your crossword! Type the words you find in the thread!")
                            .setFiles(upload.get())
                            .queue(ignored -> createEventWaiter(game, thread).build());
                });
            } catch (IllegalStateException exception) {
                message.editMessage("❌ Failed to create crossword! This usually happens when it fails to find a possible location for a word! " +
                                "Please try running the command again!")
                        .queue(ignored -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                RATELIMITS.put(event.getUser().getIdLong(), Pair.of(getName(), System.currentTimeMillis()));
            }
        });
    }

    @Getter
    public static class Game {
        private static final Random RANDOM = new Random();
        private final List<String> words;
        private final Map<String, List<Pair<Integer, Integer>>> wordLocations = new HashMap<>();
        private final int wordCount;
        private final long guildId, userId, channelId;
        private char[][] board;
        @Setter
        private long threadId;

        public Game(int wordCount, long guildId, long userId, long channelId) throws IllegalStateException {
            if (wordCount < 1)
                throw new IllegalStateException("Word count must be greater than 0!");

            this.wordCount = wordCount;

            this.words = getWords(wordCount);
            if (this.words.isEmpty())
                throw new IllegalStateException("Failed to get words for crossword!");

            this.guildId = guildId;
            this.userId = userId;
            this.channelId = channelId;

            this.board = new char[10][10];
            fillBoard();
        }

        private static List<String> getWords(int wordCount) {
            Either<List<String>, HttpStatus> response = ApiHandler.getWords(new RandomWordRequestData.Builder()
                    .length(3, 10)
                    .amount(wordCount)
                    .build());
            if (response.isLeft())
                return List.copyOf(response.getLeft());
            else
                Constants.LOGGER.error("Failed to get words for crossword! Status: " + response.getRight());

            return List.of();
        }

        private boolean hasCharacter(int row, int column, char character) {
            return this.board[row][column] == character;
        }

        private boolean isEmpty(int row, int column) {
            return hasCharacter(row, column, '\u0000');
        }

        public boolean isOutsideBounds(int x, int y) {
            return y < 0 || y >= this.board.length || x < 0 || x >= this.board[y].length;
        }

        private void fillBoard() {
            List<String> sorted = this.words.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();
            String first = sorted.getFirst();
            while (first.length() >= this.board.length)
                expandBoard();

            int x = RANDOM.nextInt(this.board.length - first.length());
            int y = RANDOM.nextInt(this.board.length - first.length());
            boolean horizontal = RANDOM.nextBoolean();
            forcePlaceWord(first, x, y, horizontal);

            List<String> failed = new CopyOnWriteArrayList<>();
            for (int i = 1; i < sorted.size(); i++) {
                String word = sorted.get(i);
                Pair<Integer, Integer> location = findValidLocation(word);
                if (location == null) {
                    failed.add(word);
                    continue;
                }

                forcePlaceWord(word, location.getLeft(), location.getRight(), RANDOM.nextBoolean());
            }

            while (!failed.isEmpty()) {
                for (String word : failed) {
                    Pair<Integer, Integer> location = findValidLocation(word);
                    if (location == null) {
                        continue;
                    }

                    failed.remove(word);
                    forcePlaceWord(word, location.getLeft(), location.getRight(), RANDOM.nextBoolean());
                }
            }
        }

        private Pair<Integer, Integer> findValidLocation(String word) {
            for (Map.Entry<String, List<Pair<Integer, Integer>>> entry : wordLocations.entrySet()) {

            }

            return null;  // No valid intersection found
        }

        private void forcePlaceWord(String word, int x, int y, boolean horizontal) {
            List<Pair<Integer, Integer>> locations = new ArrayList<>();
            if (horizontal) {
                for (int i = 0; i < word.length(); i++) {
                    if (isOutsideBounds(x + i, y)) {
                        expandBoard();
                    }
                }

                for (int i = 0; i < word.length(); i++) {
                    board[x + i][y] = word.charAt(i);
                    locations.add(Pair.of(x + i, y));
                }
            } else {
                for (int i = 0; i < word.length(); i++) {
                    if (isOutsideBounds(x, y + i)) {
                        expandBoard();
                    }
                }

                for (int i = 0; i < word.length(); i++) {
                    board[x][y + i] = word.charAt(i);
                    locations.add(Pair.of(x, y + i));
                }
            }

            this.wordLocations.put(word, locations);
        }

        private boolean isValidIntersection(String word, int x, int y, boolean horizontal) {
            return false;
        }

        private void expandBoard() {
            char[][] oldBoard = this.board;
            this.board = new char[this.board.length + 1][this.board[0].length + 1];
            for (int j = 0; j < oldBoard.length; j++) {
                System.arraycopy(oldBoard[j], 0, this.board[j], 0, oldBoard[j].length);
            }
        }

        private void expandBoardNegatively() {
            char[][] oldBoard = this.board;
            this.board = new char[this.board.length + 1][this.board[0].length + 1];
            for (int j = 0; j < oldBoard.length; j++) {
                System.arraycopy(oldBoard[j], 0, this.board[j + 1], 0, oldBoard[j].length);
            }
        }

        public char get(int x, int y) {
            return this.board[x][y];
        }
    }
}
