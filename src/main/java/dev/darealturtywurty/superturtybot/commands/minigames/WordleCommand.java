package dev.darealturtywurty.superturtybot.commands.minigames;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.CoupledPair;
import dev.darealturtywurty.superturtybot.core.util.EventWaiter;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WordleCommand extends CoreCommand {
    private static final Map<Long, String> GUILD_WORDS = new HashMap<>();
    private static final AtomicReference<String> GLOBAL_WORD = new AtomicReference<>();

    private static final String API_URL = "https://api.turtywurty.dev/words/random?length=5";
    public static final Path WORDLE_FILE = Path.of("wordle.json");
    private static final BufferedImage DEFAULT_IMAGE;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Long, List<Game>> GUILD_GAMES = new HashMap<>();
    private static final Map<Long, Game> DM_GAMES = new HashMap<>();

    private static final Map<Character, CoupledPair<Integer>> LETTER_POSITIONS = new HashMap<>();

    static {
        List<Character> topRow = List.of('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P');
        List<Character> middleRow = List.of('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L');
        List<Character> bottomRow = List.of('Z', 'X', 'C', 'V', 'B', 'N', 'M');

        final int width = 85, spacing = 10;

        final int topRowStartX = 131;
        final int topRowStartY = 958;
        for (int column = 0; column < topRow.size(); column++) {
            char character = topRow.get(column);
            int x = topRowStartX + (column * (width + spacing));
            LETTER_POSITIONS.put(character, new CoupledPair<>(x, topRowStartY));
        }

        final int middleRowStartX = 178;
        final int middleRowStartY = 1068;
        for (int column = 0; column < middleRow.size(); column++) {
            char character = middleRow.get(column);
            int x = middleRowStartX + (column * (width + spacing));
            LETTER_POSITIONS.put(character, new CoupledPair<>(x, middleRowStartY));
        }

        final int bottomRowStartX = 273;
        final int bottomRowStartY = 1178;
        for (int column = 0; column < bottomRow.size(); column++) {
            char character = bottomRow.get(column);
            int x = bottomRowStartX + (column * (width + spacing));
            LETTER_POSITIONS.put(character, new CoupledPair<>(x, bottomRowStartY));
        }
    }

    static {
        fetchTodayWords();
        EXECUTOR.scheduleAtFixedRate(WordleCommand::fetchAndStoreWords, getInitialDelay(), 24, TimeUnit.HOURS);

        try {
            DEFAULT_IMAGE = ImageIO.read(TurtyBot.class.getResourceAsStream("/wordle.png"));
        } catch (IOException | NullPointerException exception) {
            throw new IllegalStateException("Failed to load default image!", exception);
        }
    }

    public WordleCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play the daily game of Wordle! A game where you have to guess today's chosen word.";
    }

    @Override
    public String getName() {
        return "wordle";
    }

    @Override
    public String getRichName() {
        return "Wordle";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> {}, error -> {});

        if (!event.isFromGuild()) {
            runGlobal(event);
        } else {
            runGuild(event);
        }
    }

    private static void runGlobal(SlashCommandInteractionEvent event) {
        String word = GLOBAL_WORD.get();
        if (word == null) {
            event.getHook().sendMessage("❌ The word of the day has not been set yet!").queue();
            return;
        }

        CompletableFuture<Message> messageFuture = new CompletableFuture<>();
        BufferedImage image = createGame(event, word, messageFuture);
        if (image == null) {
            event.getHook().sendMessage("❌ An error occurred while creating the game!").queue();
            return;
        }

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            byte[] data = stream.toByteArray();
            FileUpload upload = FileUpload.fromData(data, "wordle.png");

            event.getHook()
                    .sendMessage("Can you guess today's word? You have 6 tries!")
                    .setFiles(upload)
                    .queue(messageFuture::complete);
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while creating the game!").queue();
            Constants.LOGGER.error("An error occurred while creating the game!", exception);
        }
    }

    private static void runGuild(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().sendMessage("❌ An error occurred while getting the guild!").queue();
            return;
        }

        String word = getOrFetch(guild.getIdLong());
        if (word == null) {
            event.getHook().sendMessage("❌ The word of the day has not been set yet!").queue();
            return;
        }

        CompletableFuture<Message> messageFuture = new CompletableFuture<>();
        BufferedImage image = createGame(event, word, messageFuture);
        if (image == null) {
            event.getHook().sendMessage("❌ An error occurred while creating the game!").queue();
            return;
        }

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            byte[] data = stream.toByteArray();
            FileUpload upload = FileUpload.fromData(data, "wordle.png");

            event.getChannel()
                    .asTextChannel()
                    .createThreadChannel(event.getUser().getName() + "'s Wordle Game", true)
                    .setInvitable(false)
                    .queue(thread -> {
                        thread.addThreadMember(event.getUser()).queue();
                        thread.sendMessage("Can you guess today's word? You have 6 tries!")
                                .setFiles(upload)
                                .queue(message -> {
                                    try {
                                        upload.close();
                                    } catch (IOException exception) {
                                        Constants.LOGGER.error("An error occurred while closing the file upload!", exception);
                                    } finally {
                                        messageFuture.complete(message);
                                    }
                                });

                        event.getHook()
                                .sendMessage("✅ Created a thread for you to play the game in! " + thread.getAsMention())
                                .queue();
                    });
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while creating the game!").queue();
            Constants.LOGGER.error("An error occurred while creating the game!", exception);
        }
    }

    private static BufferedImage createGame(SlashCommandInteractionEvent event, String word, CompletableFuture<Message> messageFuture) {
        messageFuture.thenAccept(message -> {
            @Nullable Long guildId = event.isFromGuild() && event.getGuild() != null ? event.getGuild().getIdLong() : null;
            long channelId = message.getChannel().getIdLong();
            long messageId = message.getIdLong();
            long userId = event.getUser().getIdLong();

            var game = new Game(word, guildId, channelId, messageId, userId);
            if(guildId != null) {
                GUILD_GAMES.computeIfAbsent(channelId, id -> new ArrayList<>()).add(game);
            } else {
                DM_GAMES.put(userId, game);
            }

            createEventWaiter(event.getGuild(), game).build();
        });

        return createImage(new ArrayList<>(), (letterIndex, character) -> Game.LetterState.NOT_GUESSED, character -> new Color(0x6D7C87));
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createEventWaiter(Guild guild, Game game) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event -> {
                    String content = event.getMessage().getContentRaw();
                            return ((event.isFromGuild() && event.getGuild().getIdLong() == guild.getIdLong())
                                        || !event.isFromGuild())
                                    && event.getChannel().getIdLong() == game.getChannelId()
                                    && event.getAuthor().getIdLong() == game.getUserId()
                                    && (content.length() == 5
                                        && content.matches("[a-zA-Z]+")
                                        || content.equalsIgnoreCase("give up"));
                        }
                )
                .success(event -> {
                    if(handleResponse(event, game)) {
                        createEventWaiter(guild, game).build();
                    }
                });
    }

    private static boolean handleResponse(MessageReceivedEvent event, Game game) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        if(content.equalsIgnoreCase("give up")) {
            if(event.isFromGuild()) {
                endGame(event.getGuild(), game);
            } else {
                endGame(event.getAuthor(), game);
            }

            return false;
        }

        if(game.guess(content)) {
            sendUpdatedImage(event.getChannel(), game);

            if(event.isFromGuild()) {
                endGame(event.getGuild(), game);
            } else {
                endGame(event.getAuthor(), game);
            }

            return false;
        }

        sendUpdatedImage(event.getChannel(), game);
        return true;
    }

    private static void sendUpdatedImage(MessageChannel channel, Game game) {
        try {
            BufferedImage image = createImage(game.getGuesses(), game::getLetterState, game::getCharacterColor);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] data = baos.toByteArray();
            FileUpload upload = FileUpload.fromData(data, "wordle.png");
            channel.sendFiles(upload).queue();
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred whilst sending end game image!", exception);
        }
    }

    private static void endGame(@NotNull Guild guild, @NotNull Game game) {
        List<Game> games = GUILD_GAMES.computeIfAbsent(guild.getIdLong(), id -> new ArrayList<>());
        games.remove(game);

        if (games.isEmpty()) {
            GUILD_GAMES.remove(guild.getIdLong());
        }

        ThreadChannel thread = guild.getThreadChannelById(game.getChannelId());
        if (thread == null) return;

        if (game.isWon()) {
            thread.sendMessage("Congratulations! You won! The word was: " + game.getWord())
                    .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
            return;
        }

        if (game.isLost()) {
            thread.sendMessage("You lost! The word was: " + game.getWord())
                    .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
            return;
        }

        thread.sendMessage("The game has ended! The word was: " + game.getWord())
                .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
    }

    private static void endGame(@NotNull User user, @NotNull Game game) {
        DM_GAMES.remove(user.getIdLong());

        user.openPrivateChannel().queue(channel -> {
            if (channel == null) return;

            if (game.isWon()) {
                channel.sendMessage("Congratulations! You won! The word was: " + game.getWord()).queue();
                return;
            }

            if (game.isLost()) {
                channel.sendMessage("You lost! The word was: " + game.getWord()).queue();
                return;
            }

            channel.sendMessage("The game has ended! The word was: " + game.getWord()).queue();
        }, ignored -> {});
    }

    private static BufferedImage createImage(List<String> guesses, BiFunction<Integer, Character, Game.LetterState> letterStateGetter, Function<Character, Color> characterColorGetter) {
        BufferedImage image = new BufferedImage(DEFAULT_IMAGE.getWidth(), DEFAULT_IMAGE.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        FontMetrics metrics = graphics.getFontMetrics();

        graphics.drawImage(DEFAULT_IMAGE, 0, 0, null);

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(new Color(0x151515));
        graphics.setFont(new Font("Arial", Font.BOLD, 50));

        final int startX = 280, startY = 70, spacing = 36, guessedSize = 100;
        for (int guessIndex = 0; guessIndex < guesses.size(); guessIndex++) {
            String guess = guesses.get(guessIndex);
            for (int letterIndex = 0; letterIndex < guess.toCharArray().length; letterIndex++) {
                char character = guess.charAt(letterIndex);
                graphics.setColor(letterStateGetter.apply(letterIndex, character).getColor());

                graphics.fillRect(
                        startX + (letterIndex * (guessedSize + spacing)),
                        startY + (guessIndex * (guessedSize + spacing)),
                        guessedSize,
                        guessedSize);

                graphics.setColor(Color.BLACK);

                String characterStr = String.valueOf(character).toUpperCase(Locale.ROOT);
                graphics.drawString(
                        characterStr,
                        startX + (letterIndex * (guessedSize + spacing)) + guessedSize / 2 - metrics.stringWidth(characterStr),
                        startY + (guessIndex * (guessedSize + spacing)) + guessedSize / 2 + (metrics.getAscent() - metrics.getDescent()) / 2);
            }
        }

        final int letterWidth = 85, letterHeight = 100;
        int ascent = metrics.getAscent();
        int descent = metrics.getDescent();
        LETTER_POSITIONS.forEach((character, position) -> {
            graphics.setColor(characterColorGetter.apply(character));
            graphics.fillRect(position.getLeft(), position.getRight(), letterWidth, letterHeight);

            graphics.setColor(Color.BLACK);

            String characterStr = String.valueOf(character).toUpperCase(Locale.ROOT);
            graphics.drawString(
                    characterStr,
                    position.getLeft() + letterWidth / 2 - metrics.stringWidth(characterStr),
                    position.getRight() + letterHeight / 2 + (ascent - descent) / 2);
        });

        return image;
    }

    private static long getInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = (Calendar) now.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        if (now.after(midnight)) {
            midnight.add(Calendar.DAY_OF_MONTH, 1);
        }

        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }

    private static void fetchAndStoreWords() {
        try {
            String json = IOUtils.toString(new URL(API_URL), StandardCharsets.UTF_8);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            if (array.isEmpty()) {
                fetchAndStoreWords();
                return;
            }

            String word = array.get(0).getAsString();
            if (!isValidWord(word)) {
                fetchAndStoreWords();
                return;
            }

            GLOBAL_WORD.set(word);

            for (Map.Entry<Long, String> entry : GUILD_WORDS.entrySet()) {
                fetchWordForGuild(entry.getKey());
            }

            JsonObject jsonToStore = new JsonObject();
            jsonToStore.addProperty("word", GLOBAL_WORD.get());
            for (Map.Entry<Long, String> entry : GUILD_WORDS.entrySet()) {
                jsonToStore.addProperty(entry.getKey().toString(), entry.getValue());
            }

            Files.writeString(WORDLE_FILE, Constants.GSON.toJson(jsonToStore), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch word from API!", exception);
        }
    }

    private static void fetchWordForGuild(long guildId) throws IOException {
        String json = IOUtils.toString(new URL(API_URL), StandardCharsets.UTF_8);
        JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);

        int attempts = 0;
        while (array.isEmpty() && attempts < 5) {
            json = IOUtils.toString(new URL(API_URL), StandardCharsets.UTF_8);
            array = Constants.GSON.fromJson(json, JsonArray.class);
            attempts++;
        }

        if (array.isEmpty())
            return;

        String word = array.get(0).getAsString();
        attempts = 0;
        while (!isValidWord(word) && attempts < 5) {
            json = IOUtils.toString(new URL(API_URL), StandardCharsets.UTF_8);
            array = Constants.GSON.fromJson(json, JsonArray.class);
            word = array.get(0).getAsString();
            attempts++;
        }

        if (!isValidWord(word))
            return;

        GUILD_WORDS.put(guildId, word);
    }

    private static String getOrFetch(long guildId) {
        String word = GUILD_WORDS.get(guildId);
        if (word == null) {
            try {
                fetchWordForGuild(guildId);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to fetch word from API!", exception);
            }
        }

        return GUILD_WORDS.get(guildId);
    }

    private static void fetchTodayWords() {
        try {
            if (Files.exists(WORDLE_FILE)) {
                String jsonStr = Files.readString(WORDLE_FILE, StandardCharsets.UTF_8);
                JsonObject json = Constants.GSON.fromJson(jsonStr, JsonObject.class);
                if (json == null) {
                    fetchAndStoreWords();
                    fetchTodayWords();
                    return;
                }

                String word = json.get("word").getAsString();
                if (isValidWord(word)) {
                    GLOBAL_WORD.set(word);
                } else {
                    fetchAndStoreWords();
                }

                if(!json.has("guild_words"))
                    return;

                JsonObject guildWords = json.get("guild_words").getAsJsonObject();
                for (String guildId : guildWords.keySet()) {
                    if (!guildId.matches("[0-9]+"))
                        continue;

                    String guildWord = guildWords.get(guildId).getAsString();
                    if (isValidWord(guildWord)) {
                        GUILD_WORDS.put(Long.parseLong(guildId), guildWord);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read word from file!", exception);
        }
    }

    private static boolean isValidWord(String word) {
        return word.length() == 5 && word.matches("[a-zA-Z]+") && !word.isBlank() && !word.contains(" ");
    }

    @Getter
    public static class Game {
        private final List<String> guesses = new ArrayList<>();

        private final String word;
        private int tries;

        private final @Nullable Long guildId;
        private final long channelId;
        private final long messageId;
        private final long userId;

        public Game(String word, @Nullable Long guildId, long channelId, long messageId, long userId) {
            this.word = word;
            this.tries = 6;

            this.guildId = guildId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
        }

        public boolean isLost() {
            return tries == 0 || guesses.size() >= 6;
        }

        public boolean guess(String guess) {
            if(!isValidWord(guess))
                return false;

            if(guesses.contains(guess))
                return false;

            guesses.add(guess);

            if(guess.equalsIgnoreCase(this.word))
                return true;

            if (isWon() || isLost())
                return true;

            tries--;
            return false;
        }

        public LetterState getLetterState(int letterIndex, char character) {
            if (this.word.charAt(letterIndex) == character) {
                return LetterState.CORRECT;
            }

            if (this.word.contains(String.valueOf(character))) {
                return LetterState.WRONG_POSITION;
            }

            return LetterState.NOT_GUESSED;
        }

        /**
         * Gets the color of the character.
         * <p>
         * The color is based on the letter state of the character.
         * - If the character is in the word and is part of one of the guesses but not at the right index then the color is yellow.
         * - If the character is in the word and is part of one of the guesses and is at the right index then the color is green.
         * - If the character is not in the word and is part of one of the guesses then the color is red.
         * - If the character is not in the word and is not part of one of the guesses then the color is white.
         *
         * @param character The character to get the color of.
         * @return The color of the character.
         */
        public Color getCharacterColor(Character character) {
            String lowercaseWord = word.toLowerCase();
            char lowercaseChar = Character.toLowerCase(character);
            boolean isInWord = lowercaseWord.contains(String.valueOf(lowercaseChar));
            boolean isAtCorrectIndex = false;
            Color color = Color.WHITE; // Default color is white

            for (String guess : guesses) {
                String lowercaseGuess = guess.toLowerCase();
                if (lowercaseGuess.contains(String.valueOf(lowercaseChar))) {
                    if (isInWord) {
                        // Check if the character is at the right index
                        isAtCorrectIndex = lowercaseWord.indexOf(lowercaseChar) == lowercaseGuess.indexOf(lowercaseChar);
                        if (isAtCorrectIndex) {
                            color = Color.GREEN; // Green has the highest priority
                            return color;
                        } else {
                            color = Color.YELLOW; // Yellow has the second highest priority
                        }
                    } else {
                        color = Color.RED; // Red has the third highest priority
                    }
                }
            }

            return color;
        }

        public boolean isWon() {
            return guesses.stream().anyMatch(word -> word.equalsIgnoreCase(this.word));
        }

        public enum LetterState {
            CORRECT(Color.GREEN),
            INCORRECT(Color.RED),
            WRONG_POSITION(Color.YELLOW),
            NOT_GUESSED(Color.WHITE);

            @Getter
            private final Color color;

            LetterState(Color color) {
                this.color = color;
            }
        }
    }
}
