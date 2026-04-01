package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.WordDefinition;
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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrosswordCommand extends CoreCommand {
    private static final Pattern GUESS_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*=\\s*([a-zA-Z]+)\\s*$");
    private static final Pattern CLEAR_PATTERN = Pattern.compile("^\\s*clear\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final int GRID_SIZE = 13;
    private static final int TARGET_WORD_COUNT = 7;
    private static final int MINIMUM_WORD_COUNT = 5;
    private static final int MAX_GENERATION_ATTEMPTS = 18;
    private static final int MAX_WORD_FETCH_ATTEMPTS = 18;
    private static final int CANDIDATE_BATCH_SIZE = 24;
    private static final int CELL_SIZE = 54;
    private static final int HEADER_HEIGHT = 96;
    private static final int PADDING = 24;
    private static final int CLUE_COLUMN_WIDTH = 420;
    private static final Font TITLE_FONT = new Font("Georgia", Font.BOLD, 30);
    private static final Font SUBTITLE_FONT = new Font("Georgia", Font.PLAIN, 15);
    private static final Font LETTER_FONT = new Font("Georgia", Font.BOLD, 30);
    private static final Font CLUE_FONT = new Font("Georgia", Font.PLAIN, 15);
    private static final Font SECTION_FONT = new Font("Georgia", Font.BOLD, 20);
    private static final Font NUMBER_FONT = new Font("Georgia", Font.PLAIN, 12);
    private static final Stroke CELL_STROKE = new BasicStroke(2.5f);
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
                        && (event.getMessage().getContentRaw().equalsIgnoreCase("give up")
                        || GUESS_PATTERN.matcher(event.getMessage().getContentRaw()).matches()
                        || CLEAR_PATTERN.matcher(event.getMessage().getContentRaw()).matches()))
                .timeout(10, TimeUnit.MINUTES)
                .timeoutAction(() -> endGame(thread, game, "❌ You took too long. Crossword revealed.", true))
                .failure(() -> endGame(thread, game, "❌ Something went wrong. Crossword revealed.", true))
                .success(event -> {
                    String raw = event.getMessage().getContentRaw().trim();
                    if (raw.equalsIgnoreCase("give up")) {
                        endGame(thread, game, "✅ Crossword ended. Solution revealed.", true);
                        return;
                    }

                    Matcher clearMatcher = CLEAR_PATTERN.matcher(raw);
                    if (clearMatcher.matches()) {
                        int clueNumber = Integer.parseInt(clearMatcher.group(1));
                        if (!game.clearGuess(clueNumber)) {
                            thread.sendMessage("❌ That clue number does not exist.")
                                    .queue(ignored -> createEventWaiter(game, thread).build());
                            return;
                        }

                        Optional<FileUpload> upload = createUpload(game, false);
                        if (upload.isPresent()) {
                            thread.sendMessage("✍️ Crossword updated.")
                                    .setFiles(upload.get())
                                    .queue(ignored -> createEventWaiter(game, thread).build());
                        } else {
                            thread.sendMessage("✍️ Crossword updated.")
                                    .queue(ignored -> createEventWaiter(game, thread).build());
                        }

                        return;
                    }

                    Matcher matcher = GUESS_PATTERN.matcher(raw);
                    if (!matcher.matches()) {
                        createEventWaiter(game, thread).build();
                        return;
                    }

                    int clueNumber = Integer.parseInt(matcher.group(1));
                    String guess = normalizeWord(matcher.group(2));
                    GuessResult result = game.applyGuess(clueNumber, guess);
                    if (result == GuessResult.DOES_NOT_FIT) {
                        thread.sendMessage("❌ That word does not fit clue " + clueNumber + ".")
                                .queue(ignored -> createEventWaiter(game, thread).build());
                        return;
                    }

                    if (result == GuessResult.INVALID_CLUE) {
                        thread.sendMessage("❌ That clue number does not exist.")
                                .queue(ignored -> createEventWaiter(game, thread).build());
                        return;
                    }

                    if (result == GuessResult.CANNOT_PLACE) {
                        thread.sendMessage("❌ That word cannot be placed with the letters already on the board.")
                                .queue(ignored -> createEventWaiter(game, thread).build());
                        return;
                    }

                    boolean completed = game.isComplete();
                    Optional<FileUpload> upload = createUpload(game, completed);
                    if (completed) {
                        if (upload.isPresent()) {
                            thread.sendMessage("✅ Crossword complete.")
                                    .setFiles(upload.get())
                                    .queue(ignored -> archiveThread(thread));
                        } else {
                            thread.sendMessage("✅ Crossword complete.")
                                    .queue(ignored -> archiveThread(thread));
                        }

                        GAMES.remove(game);
                        return;
                    }

                    if (upload.isPresent()) {
                        thread.sendMessage("✍️ Crossword updated.")
                                .setFiles(upload.get())
                                .queue(ignored -> createEventWaiter(game, thread).build());
                    } else {
                        thread.sendMessage("✍️ Crossword updated.")
                                .queue(ignored -> createEventWaiter(game, thread).build());
                    }
                });
    }

    private static void endGame(ThreadChannel thread, Game game, String message, boolean revealAll) {
        Optional<FileUpload> upload = createUpload(game, revealAll);
        if (upload.isPresent()) {
            thread.sendMessage(message)
                    .setFiles(upload.get())
                    .queue(ignored -> archiveThread(thread));
        } else {
            thread.sendMessage(message)
                    .queue(ignored -> archiveThread(thread));
        }

        GAMES.remove(game);
    }

    private static void archiveThread(ThreadChannel thread) {
        thread.getManager().setArchived(true).setLocked(true).queue();
    }

    private static Optional<FileUpload> createUpload(Game game, boolean revealAll) {
        BufferedImage image = generateImage(game, revealAll);
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write crossword image", exception);
            return Optional.empty();
        }

        return Optional.of(FileUpload.fromData(baos.toByteArray(), "crossword.png"));
    }

    private static BufferedImage generateImage(Game game, boolean revealAll) {
        Bounds bounds = game.getUsedBounds();
        int boardWidth = bounds.width() * CELL_SIZE;
        int boardHeight = bounds.height() * CELL_SIZE;
        int clueHeight = Math.max(boardHeight, 560);
        int imageWidth = boardWidth + CLUE_COLUMN_WIDTH + (PADDING * 3);
        int imageHeight = HEADER_HEIGHT + clueHeight + PADDING;

        var image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setPaint(new GradientPaint(0, 0, new Color(245, 238, 226),
                imageWidth, imageHeight, new Color(226, 234, 244)));
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        int boardOriginX = PADDING;
        int boardOriginY = HEADER_HEIGHT;
        int clueOriginX = boardOriginX + boardWidth + PADDING;

        graphics.setColor(new Color(0, 0, 0, 24));
        graphics.fillRoundRect(boardOriginX + 8, boardOriginY + 8, boardWidth, boardHeight, 26, 26);
        graphics.fillRoundRect(clueOriginX + 8, boardOriginY + 8, CLUE_COLUMN_WIDTH, clueHeight, 26, 26);

        graphics.setColor(new Color(255, 251, 243));
        graphics.fillRoundRect(boardOriginX, boardOriginY, boardWidth, boardHeight, 26, 26);
        graphics.setColor(new Color(244, 247, 251));
        graphics.fillRoundRect(clueOriginX, boardOriginY, CLUE_COLUMN_WIDTH, clueHeight, 26, 26);

        graphics.setColor(new Color(42, 54, 66));
        graphics.setFont(TITLE_FONT);
        graphics.drawString("Crossword", PADDING, 38);
        graphics.setFont(SUBTITLE_FONT);
        graphics.setColor(new Color(91, 103, 116));
        graphics.drawString("Enter answers as `x=word`, or use `clear x` to empty a clue.", PADDING, 62);
        graphics.drawString("The board updates with any guess that fits the current crossings.", PADDING, 82);
        graphics.drawString("Solved " + game.getSolvedCount() + "/" + game.getEntries().size(), PADDING, 102);

        FontMetrics letterMetrics = graphics.getFontMetrics(LETTER_FONT);
        FontMetrics numberMetrics = graphics.getFontMetrics(NUMBER_FONT);
        graphics.setStroke(CELL_STROKE);

        for (int row = bounds.minRow(); row <= bounds.maxRow(); row++) {
            for (int column = bounds.minColumn(); column <= bounds.maxColumn(); column++) {
                int drawX = boardOriginX + ((column - bounds.minColumn()) * CELL_SIZE);
                int drawY = boardOriginY + ((row - bounds.minRow()) * CELL_SIZE);

                if (!game.hasLetter(row, column)) {
                    graphics.setColor(new Color(55, 64, 78));
                    graphics.fillRoundRect(drawX + 2, drawY + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);
                    continue;
                }

                graphics.setColor(new Color(255, 255, 255));
                graphics.fillRoundRect(drawX + 2, drawY + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);
                graphics.setColor(new Color(131, 142, 154));
                graphics.drawRoundRect(drawX + 2, drawY + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);

                Entry startEntry = game.getEntryStartingAt(row, column);
                if (startEntry != null) {
                    graphics.setFont(NUMBER_FONT);
                    graphics.setColor(new Color(95, 105, 118));
                    graphics.drawString(Integer.toString(startEntry.getNumber()), drawX + 7,
                            drawY + 7 + numberMetrics.getAscent());
                }

                if (revealAll || game.hasFilledLetter(row, column)) {
                    char currentLetter = revealAll ? game.getSolution(row, column) : game.getDisplayedLetter(row, column);
                    String letter = String.valueOf(Character.toUpperCase(currentLetter));
                    graphics.setFont(LETTER_FONT);
                    graphics.setColor(new Color(42, 54, 66));
                    int textX = drawX + (CELL_SIZE - letterMetrics.stringWidth(letter)) / 2;
                    int textY = drawY + ((CELL_SIZE - letterMetrics.getHeight()) / 2) + letterMetrics.getAscent();
                    graphics.drawString(letter, textX, textY);
                }
            }
        }

        int clueY = boardOriginY + 28;
        graphics.setColor(new Color(45, 63, 81));
        graphics.setFont(SECTION_FONT);
        graphics.drawString("Across", clueOriginX + 20, clueY);
        clueY += 24;
        graphics.setFont(CLUE_FONT);
        for (Entry entry : game.getEntries(Direction.ACROSS)) {
            clueY = drawWrappedClue(graphics, clueOriginX + 20, clueY, CLUE_COLUMN_WIDTH - 40, entry);
            clueY += 8;
        }

        clueY += 12;
        graphics.setColor(new Color(45, 63, 81));
        graphics.setFont(SECTION_FONT);
        graphics.drawString("Down", clueOriginX + 20, clueY);
        clueY += 24;
        graphics.setFont(CLUE_FONT);
        for (Entry entry : game.getEntries(Direction.DOWN)) {
            clueY = drawWrappedClue(graphics, clueOriginX + 20, clueY, CLUE_COLUMN_WIDTH - 40, entry);
            clueY += 8;
        }

        graphics.dispose();
        return image;
    }

    private static int drawWrappedClue(Graphics2D graphics, int x, int y, int maxWidth, Entry entry) {
        FontMetrics metrics = graphics.getFontMetrics();
        String prefix = entry.getNumber() + ". ";
        List<String> wrappedLines = wrapText(prefix, entry.getClue(), maxWidth, metrics);
        graphics.setColor(new Color(90, 102, 116));
        for (String line : wrappedLines) {
            graphics.drawString(line, x, y);
            y += metrics.getHeight() + 2;
        }

        return y;
    }

    private static List<String> wrapText(String prefix, String text, int maxWidth, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        String indent = " ".repeat(prefix.length());
        StringBuilder line = new StringBuilder(prefix);
        boolean firstLine = true;

        for (String word : words) {
            String candidate = line + (line.length() > (firstLine ? prefix.length() : indent.length()) ? " " : "") + word;
            int threshold = firstLine ? prefix.length() : indent.length();
            if (metrics.stringWidth(candidate) > maxWidth && line.length() > threshold) {
                lines.add(line.toString());
                line = new StringBuilder(indent).append(word);
                firstLine = false;
                continue;
            }

            if (line.length() > threshold) {
                line.append(' ');
            }

            line.append(word);
        }

        lines.add(line.toString());
        return lines;
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a clue-based crossword.";
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

        if (!guild.getSelfMember().hasPermission(event.getGuildChannel(),
                Permission.CREATE_PUBLIC_THREADS, Permission.MANAGE_THREADS)) {
            reply(event, "❌ I do not have permission to create or manage threads in this channel!", false, true);
            return;
        }

        if (GAMES.stream().anyMatch(game -> game.getGuildId() == guild.getIdLong()
                && game.getChannelId() == event.getChannel().getIdLong()
                && game.getUserId() == event.getUser().getIdLong())) {
            reply(event, "❌ You already have a crossword game in this channel!", false, true);
            return;
        }

        reply(event, "✅ Creating a crossword...");
        event.getHook().editOriginal("✅ Crossword created.").queue(message -> {
            try {
                var game = new Game(guild.getIdLong(), event.getUser().getIdLong(), event.getChannel().getIdLong());
                GAMES.add(game);
                message.createThreadChannel(event.getUser().getEffectiveName() + "'s Crossword").queue(thread -> {
                    thread.addThreadMember(event.getUser()).queue();
                    game.setThreadId(thread.getIdLong());

                    Optional<FileUpload> upload = createUpload(game, false);
                    if (upload.isEmpty()) {
                        thread.sendMessage("❌ Failed to render the crossword.")
                                .queue(ignored -> archiveThread(thread));
                        GAMES.remove(game);
                        return;
                    }

                    thread.sendMessage("✍️ Fill clues with `x=word`, where `x` is the clue number. Use `clear x` to empty a clue. Any guess that fits the current crossings is written into the board, and completion is only confirmed at the end.")
                            .setFiles(upload.get())
                            .queue(ignored -> createEventWaiter(game, thread).build());
                });
            } catch (IllegalStateException exception) {
                Constants.LOGGER.error("Failed to create crossword", exception);
                message.editMessage("❌ Failed to create a playable crossword. Please try again.")
                        .queue(ignored -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                RATE_LIMITS.put(event.getUser().getIdLong(), Pair.of(getName(), System.currentTimeMillis()));
            }
        });
    }

    @Getter
    public static class Game {
        private static final Random RANDOM = new Random();

        private final long guildId;
        private final long userId;
        private final long channelId;
        private final char[][] solution = new char[GRID_SIZE][GRID_SIZE];
        private final List<Entry> entries = new ArrayList<>();
        @Setter
        private long threadId;

        public Game(long guildId, long userId, long channelId) {
            this.guildId = guildId;
            this.userId = userId;
            this.channelId = channelId;

            if (!generate()) {
                throw new IllegalStateException("Unable to generate crossword");
            }
        }

        private boolean generate() {
            for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
                clearBoard();
                this.entries.clear();

                List<CandidateWord> candidates = fetchCandidateWords();
                if (candidates.size() < TARGET_WORD_COUNT) {
                    continue;
                }

                candidates.sort(Comparator
                        .comparingInt((CandidateWord definition) -> Math.abs(6 - definition.word().length()))
                        .thenComparing(definition -> definition.word()));

                CandidateWord seed = selectSeed(candidates);
                int startRow = GRID_SIZE / 2;
                int startColumn = Math.max(0, (GRID_SIZE - seed.word().length()) / 2);
                place(new Entry(seed.word(), seed.definition(), Direction.ACROSS, startRow, startColumn));

                for (CandidateWord candidate : candidates) {
                    if (candidate.word().equals(seed.word())) {
                        continue;
                    }

                    Placement placement = findPlacement(candidate.word());
                    if (placement == null) {
                        continue;
                    }

                    place(new Entry(candidate.word(), candidate.definition(),
                            placement.direction(), placement.row(), placement.column()));
                    if (this.entries.size() >= TARGET_WORD_COUNT) {
                        break;
                    }
                }

                if (!isPlayableLayout()) {
                    continue;
                }

                assignNumbers();
                return true;
            }

            return false;
        }

        private CandidateWord selectSeed(List<CandidateWord> candidates) {
            List<CandidateWord> sorted = new ArrayList<>(candidates);
            sorted.sort(Comparator.comparingInt((CandidateWord definition) -> definition.word().length()).reversed());
            for (CandidateWord candidate : sorted) {
                if (candidate.word().length() <= GRID_SIZE - 2) {
                    return candidate;
                }
            }

            return sorted.getFirst();
        }

        private boolean isPlayableLayout() {
            if (this.entries.size() < MINIMUM_WORD_COUNT) {
                return false;
            }

            int acrossCount = 0;
            int downCount = 0;
            for (Entry entry : this.entries) {
                int intersections = countIntersections(entry);
                if (intersections == 0) {
                    return false;
                }

                if (entry.direction == Direction.ACROSS) {
                    acrossCount++;
                } else {
                    downCount++;
                }
            }

            return acrossCount >= 2 && downCount >= 2;
        }

        private int countIntersections(Entry entry) {
            int intersections = 0;
            for (int index = 0; index < entry.word.length(); index++) {
                int row = entry.row + (entry.direction == Direction.DOWN ? index : 0);
                int column = entry.column + (entry.direction == Direction.ACROSS ? index : 0);
                if (countEntriesAt(row, column) > 1) {
                    intersections++;
                }
            }

            return intersections;
        }

        private int countEntriesAt(int row, int column) {
            int count = 0;
            for (Entry entry : this.entries) {
                if (entry.contains(row, column)) {
                    count++;
                }
            }

            return count;
        }

        private void clearBoard() {
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int column = 0; column < GRID_SIZE; column++) {
                    this.solution[row][column] = '\u0000';
                }
            }
        }

        private List<CandidateWord> fetchCandidateWords() {
            Map<String, CandidateWord> definitions = new HashMap<>();
            int attempts = 0;
            while (definitions.size() < TARGET_WORD_COUNT * 3 && attempts++ < MAX_WORD_FETCH_ATTEMPTS) {
                Either<List<String>, HttpStatus> response = ApiHandler.getCommonWords(new RandomWordRequestData.Builder()
                        .length(3, 8)
                        .amount(CANDIDATE_BATCH_SIZE)
                        .build());
                if (response.isRight()) {
                    Constants.LOGGER.warn("Failed to fetch crossword words: {}", response.getRight());
                    continue;
                }

                for (String rawWord : response.getLeft()) {
                    String word = normalizeWord(rawWord);
                    if (word.isBlank() || definitions.containsKey(word)) {
                        continue;
                    }

                    Optional<String> definition = lookupDefinition(word);
                    if (definition.isEmpty()) {
                        continue;
                    }

                    definitions.put(word, new CandidateWord(word, definition.get()));
                }
            }

            return new ArrayList<>(definitions.values());
        }

        private Placement findPlacement(String word) {
            List<Placement> placements = new ArrayList<>();
            for (Entry placed : this.entries) {
                for (int placedIndex = 0; placedIndex < placed.word.length(); placedIndex++) {
                    char placedLetter = placed.word.charAt(placedIndex);
                    int placedRow = placed.row + (placed.direction == Direction.DOWN ? placedIndex : 0);
                    int placedColumn = placed.column + (placed.direction == Direction.ACROSS ? placedIndex : 0);

                    for (int wordIndex = 0; wordIndex < word.length(); wordIndex++) {
                        if (word.charAt(wordIndex) != placedLetter) {
                            continue;
                        }

                        Direction direction = placed.direction == Direction.ACROSS ? Direction.DOWN : Direction.ACROSS;
                        int row = direction == Direction.DOWN ? placedRow - wordIndex : placedRow;
                        int column = direction == Direction.ACROSS ? placedColumn - wordIndex : placedColumn;
                        if (!isValidPlacement(word, row, column, direction)) {
                            continue;
                        }

                        placements.add(new Placement(row, column, direction, scorePlacement(word, row, column, direction)));
                    }
                }
            }

            if (placements.isEmpty()) {
                return null;
            }

            placements.sort(Comparator.comparingInt(Placement::score).reversed());
            return placements.getFirst();
        }

        private int scorePlacement(String word, int row, int column, Direction direction) {
            int intersections = 0;
            int centerDistance = 0;
            int center = GRID_SIZE / 2;
            for (int index = 0; index < word.length(); index++) {
                int currentRow = row + (direction == Direction.DOWN ? index : 0);
                int currentColumn = column + (direction == Direction.ACROSS ? index : 0);
                if (this.solution[currentRow][currentColumn] == word.charAt(index)) {
                    intersections++;
                }

                centerDistance += Math.abs(currentRow - center) + Math.abs(currentColumn - center);
            }

            return (intersections * 100) - centerDistance;
        }

        private boolean isValidPlacement(String word, int row, int column, Direction direction) {
            if (row < 0 || column < 0) {
                return false;
            }

            int endRow = row + (direction == Direction.DOWN ? word.length() - 1 : 0);
            int endColumn = column + (direction == Direction.ACROSS ? word.length() - 1 : 0);
            if (endRow >= GRID_SIZE || endColumn >= GRID_SIZE) {
                return false;
            }

            if (startsAnotherEntry(row, column)) {
                return false;
            }

            int intersections = 0;
            for (int index = 0; index < word.length(); index++) {
                int currentRow = row + (direction == Direction.DOWN ? index : 0);
                int currentColumn = column + (direction == Direction.ACROSS ? index : 0);
                char existing = this.solution[currentRow][currentColumn];
                char letter = word.charAt(index);

                if (existing != '\u0000' && existing != letter) {
                    return false;
                }

                if (existing == letter) {
                    intersections++;
                    continue;
                }

                if (direction == Direction.ACROSS) {
                    if (hasLetter(currentRow - 1, currentColumn) || hasLetter(currentRow + 1, currentColumn)) {
                        return false;
                    }
                } else if (hasLetter(currentRow, currentColumn - 1) || hasLetter(currentRow, currentColumn + 1)) {
                    return false;
                }
            }

            if (direction == Direction.ACROSS) {
                if (hasLetter(row, column - 1) || hasLetter(row, endColumn + 1)) {
                    return false;
                }
            } else if (hasLetter(row - 1, column) || hasLetter(endRow + 1, column)) {
                return false;
            }

            return intersections > 0;
        }

        private boolean startsAnotherEntry(int row, int column) {
            for (Entry entry : this.entries) {
                if (entry.row == row && entry.column == column) {
                    return true;
                }
            }

            return false;
        }

        public boolean hasLetter(int row, int column) {
            return row >= 0 && row < GRID_SIZE
                    && column >= 0 && column < GRID_SIZE
                    && this.solution[row][column] != '\u0000';
        }

        private void place(Entry entry) {
            for (int index = 0; index < entry.word.length(); index++) {
                int row = entry.row + (entry.direction == Direction.DOWN ? index : 0);
                int column = entry.column + (entry.direction == Direction.ACROSS ? index : 0);
                this.solution[row][column] = entry.word.charAt(index);
            }

            this.entries.add(entry);
        }

        private void assignNumbers() {
            List<Entry> sorted = new ArrayList<>(this.entries);
            sorted.sort(Comparator.comparingInt(Entry::getRow)
                    .thenComparingInt(Entry::getColumn)
                    .thenComparing(entry -> entry.direction == Direction.ACROSS ? 0 : 1));
            for (int index = 0; index < sorted.size(); index++) {
                sorted.get(index).number = index + 1;
            }
        }

        public GuessResult applyGuess(int clueNumber, String guess) {
            Entry entry = getEntry(clueNumber);
            if (entry == null) {
                return GuessResult.INVALID_CLUE;
            }

            if (entry.word.length() != guess.length()) {
                return GuessResult.DOES_NOT_FIT;
            }

            if (!canPlaceGuess(entry, guess)) {
                return GuessResult.CANNOT_PLACE;
            }

            entry.filledWord = guess;
            return GuessResult.APPLIED;
        }

        public boolean clearGuess(int clueNumber) {
            Entry entry = getEntry(clueNumber);
            if (entry == null) {
                return false;
            }

            entry.filledWord = "";
            return true;
        }

        private boolean canPlaceGuess(Entry entry, String guess) {
            for (int index = 0; index < guess.length(); index++) {
                int row = entry.row + (entry.direction == Direction.DOWN ? index : 0);
                int column = entry.column + (entry.direction == Direction.ACROSS ? index : 0);
                char existing = getDisplayedLetter(row, column, entry);
                if (existing != '\u0000' && existing != guess.charAt(index)) {
                    return false;
                }
            }

            return true;
        }

        public boolean isComplete() {
            for (Entry entry : this.entries) {
                if (!entry.isSolved()) {
                    return false;
                }
            }

            return true;
        }

        public int getSolvedCount() {
            int count = 0;
            for (Entry entry : this.entries) {
                if (entry.isSolved()) {
                    count++;
                }
            }

            return count;
        }

        public boolean hasFilledLetter(int row, int column) {
            return getDisplayedLetter(row, column) != '\u0000';
        }

        public char getSolution(int row, int column) {
            return this.solution[row][column];
        }

        public char getDisplayedLetter(int row, int column) {
            return getDisplayedLetter(row, column, null);
        }

        private char getDisplayedLetter(int row, int column, Entry ignoredEntry) {
            for (Entry entry : this.entries) {
                if (entry == ignoredEntry || !entry.hasFilledWord() || !entry.contains(row, column)) {
                    continue;
                }

                return entry.getFilledLetter(row, column);
            }

            return '\u0000';
        }

        public Entry getEntryStartingAt(int row, int column) {
            for (Entry entry : this.entries) {
                if (entry.row == row && entry.column == column) {
                    return entry;
                }
            }

            return null;
        }

        public Entry getEntry(int number) {
            for (Entry entry : this.entries) {
                if (entry.number == number) {
                    return entry;
                }
            }

            return null;
        }

        public List<Entry> getEntries(Direction direction) {
            List<Entry> filtered = new ArrayList<>();
            for (Entry entry : this.entries) {
                if (entry.direction == direction) {
                    filtered.add(entry);
                }
            }

            filtered.sort(Comparator.comparingInt(Entry::getNumber));
            return filtered;
        }

        public Bounds getUsedBounds() {
            int minRow = GRID_SIZE;
            int minColumn = GRID_SIZE;
            int maxRow = 0;
            int maxColumn = 0;

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int column = 0; column < GRID_SIZE; column++) {
                    if (this.solution[row][column] == '\u0000') {
                        continue;
                    }

                    minRow = Math.min(minRow, row);
                    minColumn = Math.min(minColumn, column);
                    maxRow = Math.max(maxRow, row);
                    maxColumn = Math.max(maxColumn, column);
                }
            }

            if (minRow == GRID_SIZE) {
                return new Bounds(0, 0, GRID_SIZE - 1, GRID_SIZE - 1);
            }

            return new Bounds(minRow, minColumn, maxRow, maxColumn);
        }
    }

    private static Optional<String> lookupDefinition(String word) {
        Either<WordDefinition, HttpStatus> response = ApiHandler.getWordDefinition(word);
        if (response.isRight()) {
            if (response.getRight() != HttpStatus.NOT_FOUND) {
                Constants.LOGGER.warn("Failed to fetch definition for crossword word {}: {}", word, response.getRight());
            }

            return Optional.empty();
        }

        WordDefinition definition = response.getLeft();
        String clue = sanitizeDefinition(definition.definition(), word, definition.partOfSpeech());
        return clue.isBlank() ? Optional.empty() : Optional.of(clue);
    }

    private static String sanitizeDefinition(String definition, String word, String partOfSpeech) {
        String cleaned = definition == null ? "" : definition.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return "";
        }

        if (cleaned.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT))) {
            return "";
        }

        if (cleaned.length() > 180) {
            cleaned = cleaned.substring(0, 177) + "...";
        }

        if (partOfSpeech == null || partOfSpeech.isBlank()) {
            return cleaned;
        }

        return "(" + partOfSpeech + ") " + cleaned;
    }

    private static String normalizeWord(String rawWord) {
        if (rawWord == null) {
            return "";
        }

        String word = rawWord.trim().toLowerCase(Locale.ROOT);
        if (!word.matches("[a-z]+")) {
            return "";
        }

        return word;
    }

    private enum Direction {
        ACROSS,
        DOWN
    }

    private enum GuessResult {
        INVALID_CLUE,
        DOES_NOT_FIT,
        CANNOT_PLACE,
        APPLIED
    }

    @Getter
    private static class Entry {
        private final String word;
        private final String clue;
        private final Direction direction;
        private final int row;
        private final int column;
        private int number;
        private String filledWord = "";

        private Entry(String word, String clue, Direction direction, int row, int column) {
            this.word = word;
            this.clue = clue;
            this.direction = direction;
            this.row = row;
            this.column = column;
        }

        private boolean contains(int checkRow, int checkColumn) {
            for (int index = 0; index < this.word.length(); index++) {
                int row = this.row + (this.direction == Direction.DOWN ? index : 0);
                int column = this.column + (this.direction == Direction.ACROSS ? index : 0);
                if (row == checkRow && column == checkColumn) {
                    return true;
                }
            }

            return false;
        }

        private boolean hasFilledWord() {
            return !this.filledWord.isBlank();
        }

        private char getFilledLetter(int checkRow, int checkColumn) {
            if (!hasFilledWord()) {
                return '\u0000';
            }

            for (int index = 0; index < this.word.length(); index++) {
                int row = this.row + (this.direction == Direction.DOWN ? index : 0);
                int column = this.column + (this.direction == Direction.ACROSS ? index : 0);
                if (row == checkRow && column == checkColumn) {
                    return this.filledWord.charAt(index);
                }
            }

            return '\u0000';
        }

        private boolean isSolved() {
            return this.word.equals(this.filledWord);
        }
    }

    private record CandidateWord(String word, String definition) {
    }

    private record Placement(int row, int column, Direction direction, int score) {
    }

    private record Bounds(int minRow, int minColumn, int maxRow, int maxColumn) {
        private int width() {
            return this.maxColumn - this.minColumn + 1;
        }

        private int height() {
            return this.maxRow - this.minRow + 1;
        }
    }
}
