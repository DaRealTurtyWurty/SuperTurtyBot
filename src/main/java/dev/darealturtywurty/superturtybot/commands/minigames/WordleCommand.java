package dev.darealturtywurty.superturtybot.commands.minigames;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.request.RandomWordRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.core.util.object.CoupledPair;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.WordleStreakData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.WordleProfile;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@SuppressWarnings("SuspiciousNameCombination")
public class WordleCommand extends CoreCommand {
    private static final Map<Long, String> GUILD_WORDS = new HashMap<>();
    private static final AtomicReference<String> GLOBAL_WORD = new AtomicReference<>();

    private static final RandomWordRequestData REQUEST_DATA = new RandomWordRequestData.Builder().length(5).amount(1).build();
    private static final Path WORDLE_FILE = Path.of("./wordle.json");

    private static final Map<Long, List<Game>> GUILD_GAMES = new HashMap<>();
    private static final Map<Long, Game> PRIVATE_GAMES = new HashMap<>();

    private static final Map<Character, CoupledPair<Integer>> LETTER_POSITIONS = new HashMap<>();
    private static final BufferedImage DEFAULT_IMAGE;

    static {
        if(Environment.INSTANCE.turtyApiKey().isPresent()) {
            char[] topRow = "QWERTYUIOP".toCharArray();
            char[] middleRow = "ASDFGHJKL".toCharArray();
            char[] bottomRow = "ZXCVBNM".toCharArray();

            final int width = 85, spacing = 10;

            final int topRowStartX = 131;
            final int topRowStartY = 958;
            for (int column = 0; column < topRow.length; column++) {
                char character = topRow[column];
                int x = topRowStartX + (column * (width + spacing));
                LETTER_POSITIONS.put(character, new CoupledPair<>(x, topRowStartY));
            }

            final int middleRowStartX = 178;
            final int middleRowStartY = 1068;
            for (int column = 0; column < middleRow.length; column++) {
                char character = middleRow[column];
                int x = middleRowStartX + (column * (width + spacing));
                LETTER_POSITIONS.put(character, new CoupledPair<>(x, middleRowStartY));
            }

            final int bottomRowStartX = 273;
            final int bottomRowStartY = 1178;
            for (int column = 0; column < bottomRow.length; column++) {
                char character = bottomRow[column];
                int x = bottomRowStartX + (column * (width + spacing));
                LETTER_POSITIONS.put(character, new CoupledPair<>(x, bottomRowStartY));
            }

            loadTodayWords();
            DailyTaskScheduler.addTask(new DailyTask(() -> {
                fetchAndStoreWords();
                RATE_LIMITS.clear();
            }, 0, 0));

            DEFAULT_IMAGE = TurtyBot.loadImage("wordle.png");
            if(DEFAULT_IMAGE == null) {
                throw new IllegalStateException("Failed to load 'wordle.png'!");
            }
        } else {
            DEFAULT_IMAGE = null;
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
        return Pair.of(TimeUnit.DAYS, 1L);
    }

    protected void runSlash(SlashCommandInteractionEvent event) {
        if (Environment.INSTANCE.turtyApiKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.warn("Turty API key is not set!");
            return;
        }

        event.deferReply().queue(hook -> {
        }, error -> {
        });

        if (!event.isFromGuild()) {
            runGlobal(event);
        } else {
            runGuild(event);
        }
    }

    private static WordleProfile getProfile(long userId) {
        WordleProfile profile = Database.getDatabase().wordleProfiles.find(Filters.eq("user", userId)).first();
        if (profile == null) {
            profile = new WordleProfile(userId);
            Database.getDatabase().wordleProfiles.insertOne(profile);
        }

        return profile;
    }

    private static void runGlobal(SlashCommandInteractionEvent event) {
        // check if game is already running
        if (PRIVATE_GAMES.containsKey(event.getUser().getIdLong())) {
            event.getHook().sendMessage("❌ You already have a game running!").queue();
            return;
        }

        WordleProfile profile = getProfile(event.getUser().getIdLong());
        Optional<WordleStreakData> streakData = profile.getStreaks()
                .stream()
                .filter(streak -> streak.getGuild() == 0L)
                .findFirst();
        if (streakData.isPresent() && streakData.get().isHasPlayedToday()) {
            event.getHook().sendMessage("❌ You have already played today!").queue();
            return;
        }

        String word = getOrFetch();
        if (word == null) {
            event.getHook().sendMessage("❌ The word of the day has not been set yet!").queue();
            return;
        }

        CompletableFuture<Message> messageFuture = new CompletableFuture<>();
        BufferedImage image = createGame(event, word, messageFuture);

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            byte[] data = stream.toByteArray();
            FileUpload upload = FileUpload.fromData(data, "wordle.png");

            event.getHook()
                    .sendMessage("Can you guess today's word? You have 6 tries!")
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

        // check if game is already running
        List<Game> games = GUILD_GAMES.computeIfAbsent(guild.getIdLong(), id -> new ArrayList<>());
        if (games.stream().anyMatch(game -> game.getUserId() == event.getUser().getIdLong())) {
            event.getHook().sendMessage("❌ You already have a game running!").queue();
            return;
        }

        WordleProfile profile = getProfile(event.getUser().getIdLong());
        Optional<WordleStreakData> streakData = profile.getStreaks()
                .stream()
                .filter(streak -> streak.getGuild() == guild.getIdLong())
                .findFirst();
        if (streakData.isPresent() && streakData.get().isHasPlayedToday()) {
            event.getHook().sendMessage("❌ You have already played today!").queue();
            return;
        }

        String word = getOrFetch(guild.getIdLong());
        if (word == null) {
            event.getHook().sendMessage("❌ The word of the day has not been set yet!").queue();
            return;
        }

        CompletableFuture<Message> messageFuture = new CompletableFuture<>();
        BufferedImage image = createGame(event, word, messageFuture);

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            byte[] data = stream.toByteArray();
            //noinspection resource
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
            if (guildId != null) {
                GUILD_GAMES.computeIfAbsent(channelId, id -> new ArrayList<>()).add(game);
            } else {
                PRIVATE_GAMES.put(userId, game);
            }

            createEventWaiter(event.getGuild(), game).build();
        });
        return createImage(new ArrayList<>(),
                guess -> new Game.LetterState[] {
                        Game.LetterState.NOT_GUESSED,
                        Game.LetterState.NOT_GUESSED,
                        Game.LetterState.NOT_GUESSED,
                        Game.LetterState.NOT_GUESSED,
                        Game.LetterState.NOT_GUESSED
                },
                character -> Game.LetterState.NOT_GUESSED);
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createEventWaiter(Guild guild, Game game) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event -> {
                    String content = event.getMessage().getContentRaw();
                    return ((event.isFromGuild() && event.getGuild().getIdLong() == guild.getIdLong())
                            || !event.isFromGuild())
                            && event.getChannel().getIdLong() == game.getChannelId()
                            && event.getAuthor().getIdLong() == game.getUserId()
                            && (!isInvalidWord(content) || content.equalsIgnoreCase("give up"));
                })
                .success(event -> {
                    if (handleResponse(event, game)) {
                        createEventWaiter(guild, game).build();
                    }
                });
    }

    private static boolean handleResponse(MessageReceivedEvent event, Game game) {
        Message message = event.getMessage();
        String content = message.getContentRaw().toLowerCase(Locale.ROOT);
        if (content.equalsIgnoreCase("give up")) {
            if (event.isFromGuild()) {
                endGame(event.getGuild(), game);
            } else {
                endGame(event.getAuthor(), game);
            }

            return false;
        }

        if (game.guess(content)) {
            sendUpdatedImage(event.getChannel(), game);

            if (event.isFromGuild()) {
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
            Map<Character, Game.LetterState> characterColors = game.getCharacterColors();
            BufferedImage image = createImage(game.getGuesses(), game::getLetterStates, key -> characterColors.getOrDefault(key, Game.LetterState.NOT_GUESSED));
            var baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] data = baos.toByteArray();
            FileUpload upload = FileUpload.fromData(data, "wordle.png");
            channel.sendFiles(upload).queue();
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred whilst sending the updated image!", exception);
            channel.sendMessage("❌ An error occurred whilst sending the updated image!").queue();
        }
    }

    private static void updateDatabase(long guild, Game game, boolean won) {
        WordleProfile profile = getProfile(game.getUserId());
        WordleStreakData streakData = profile.getStreaks()
                .stream()
                .filter(streak -> streak.getGuild() == guild)
                .findFirst().orElse(null);
        if (streakData == null) {
            streakData = new WordleStreakData();
            streakData.setGuild(guild);
            profile.getStreaks().add(streakData);
        }

        streakData.setHasPlayedToday(true);
        if (won) {
            streakData.setStreak(streakData.getStreak() + 1);
            if (streakData.getStreak() > streakData.getBestStreak()) {
                streakData.setBestStreak(streakData.getStreak());
            }
        } else {
            streakData.setStreak(0);
        }

        Database.getDatabase().wordleProfiles.replaceOne(Filters.eq("user", game.getUserId()), profile);
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

            updateDatabase(guild.getIdLong(), game, true);
            return;
        }

        if (game.isLost()) {
            thread.sendMessage("You lost! The word was: " + game.getWord())
                    .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());

            updateDatabase(guild.getIdLong(), game, false);
            return;
        }

        thread.sendMessage("The game has ended! The word was: " + game.getWord())
                .queue(ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
    }

    private static void endGame(@NotNull User user, @NotNull Game game) {
        PRIVATE_GAMES.remove(user.getIdLong());

        user.openPrivateChannel().queue(channel -> {
            if (channel == null) return;

            if (game.isWon()) {
                channel.sendMessage("Congratulations! You won! The word was: " + game.getWord()).queue();

                updateDatabase(0L, game, true);
                return;
            }

            if (game.isLost()) {
                channel.sendMessage("You lost! The word was: " + game.getWord()).queue();

                updateDatabase(0L, game, false);
                return;
            }

            channel.sendMessage("The game has ended! The word was: " + game.getWord()).queue();
        }, ignored -> {
        });
    }

    private static BufferedImage createImage(List<String> guesses, Function<String, Game.LetterState[]> letterStateGetter, Function<Character, Game.LetterState> letterToState) {
        BufferedImage image = new BufferedImage(DEFAULT_IMAGE.getWidth(), DEFAULT_IMAGE.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.drawImage(DEFAULT_IMAGE, 0, 0, null);

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 75));
        FontMetrics metrics = graphics.getFontMetrics();

        final int startX = 280, startY = 62, spacing = 10, guessedSize = 120, letterHeight = 54;
        for (int guessIndex = 0; guessIndex < guesses.size(); guessIndex++) {
            String guess = guesses.get(guessIndex);
            Game.LetterState[] letterStates = letterStateGetter.apply(guess);
            for (int letterIndex = 0; letterIndex < guess.length(); letterIndex++) {
                char character = guess.charAt(letterIndex);
                graphics.setColor(letterStates[letterIndex].getColor());

                graphics.fillRect(
                        startX + letterIndex * (guessedSize + spacing),
                        startY + guessIndex * (guessedSize + spacing),
                        guessedSize,
                        guessedSize);

                graphics.setColor(Color.WHITE);

                String characterStr = String.valueOf(character).toUpperCase(Locale.ROOT);
                graphics.drawString(
                        characterStr,
                        startX + (letterIndex * (guessedSize + spacing)) + guessedSize / 2f - metrics.stringWidth(characterStr) / 2f - 0.5f,
                        startY + (guessIndex * (guessedSize + spacing)) + guessedSize / 2f + letterHeight / 2f
                );
            }
        }

        final int keyWidth = 85, keyHeight = 100;
        LETTER_POSITIONS.forEach((character, position) -> {
            graphics.setColor(letterToState.apply(Character.toLowerCase(character)).color);
            graphics.fillRoundRect(position.getLeft(), position.getRight(), keyWidth, keyHeight, 25, 25);

            graphics.setColor(Color.WHITE);

            String characterStr = String.valueOf(character).toUpperCase(Locale.ROOT);
            graphics.drawString(
                    characterStr,
                    position.getLeft() + keyWidth / 2f - metrics.stringWidth(characterStr) / 2f - 0.5f,
                    position.getRight() + keyHeight / 2f + letterHeight / 2f
            );
        });

        return image;
    }


    private static void fetchAndStoreWords() {
        resetDaily();
        fetchWordForGlobal();

        List<Long> guildIds = new ArrayList<>(GUILD_WORDS.keySet());
        GUILD_WORDS.clear();
        for (long guildId : guildIds) {
            fetchWordForGuild(guildId, false);
        }

        writeToFile();
    }

    private static void writeToFile() {
        try {
            var json = new JsonObject();
            json.addProperty("global", GLOBAL_WORD.get());

            var guildWords = new JsonObject();
            for (Map.Entry<Long, String> entry : GUILD_WORDS.entrySet()) {
                guildWords.addProperty(String.valueOf(entry.getKey()), entry.getValue());
            }
            json.add("guild_words", guildWords);

            Files.writeString(WORDLE_FILE, Constants.GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch word from API!", exception);
        }
    }

    private static void fetchWordForGlobal() {
        GLOBAL_WORD.set(null);
        fetchWord().ifPresent(GLOBAL_WORD::set);
    }

    private static void fetchWordForGuild(long guildId, boolean update) {
        fetchWord().ifPresent(word -> GUILD_WORDS.put(guildId, word));

        if (update) {
            writeToFile();
        }
    }

    private static String getOrFetch() {
        String word = GLOBAL_WORD.get();
        if (word == null) {
            word = fetchWord().orElse(null);
            GLOBAL_WORD.set(word);
        }

        return word;
    }

    private static String getOrFetch(long guildId) {
        if (!GUILD_WORDS.containsKey(guildId) || GUILD_WORDS.get(guildId) == null) {
            fetchWordForGuild(guildId, true);
        }

        return GUILD_WORDS.get(guildId);
    }

    private static Optional<String> fetchWord() {
        Either<List<String>, HttpStatus> words = ApiHandler.getWords(REQUEST_DATA);
        if (words.isLeft()) {
            List<String> wordList = words.getLeft();
            if (wordList.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(wordList.getFirst());
        }

        Constants.LOGGER.warn("Failed to fetch word from API! Status: {}", words.getRight());
        return Optional.empty();
    }

    private static void loadTodayWords() {
        try {
            if (Files.notExists(WORDLE_FILE)) {
                Files.createDirectories(WORDLE_FILE.getParent());
                Files.createFile(WORDLE_FILE);

                fetchAndStoreWords();
                return;
            }

            String json = Files.readString(WORDLE_FILE);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String globalWord;
            if (object.has("global")) {
                globalWord = object.get("global").getAsString();
                GLOBAL_WORD.set(globalWord);
            } else {
                fetchWordForGlobal();
            }

            if (object.has("guild_words")) {
                JsonObject guildWords = object.getAsJsonObject("guild_words");
                for (Map.Entry<String, JsonElement> entry : guildWords.entrySet()) {
                    String word = entry.getValue().getAsString();
                    GUILD_WORDS.put(Long.parseLong(entry.getKey()), word);
                }
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load today's words!", exception);
        }
    }

    private static boolean isInvalidWord(String word) {
        return word.length() != 5
                || !word.matches("[a-zA-Z]+")
                || word.isBlank()
                || word.contains(" ")
                || !ApiHandler.isWord(word).converge(Boolean::booleanValue, httpStatus -> false);
    }

    private static void resetDaily() {
        List<WordleProfile> profiles = Database.getDatabase().wordleProfiles.find().into(new ArrayList<>());
        for (WordleProfile profile : profiles) {
            for (WordleStreakData streak : profile.getStreaks()) {
                if (streak.isHasPlayedToday())
                    streak.setHasPlayedToday(false);
                else
                    streak.setStreak(0);
            }

            Database.getDatabase().wordleProfiles.replaceOne(Filters.eq("user", profile.getUser()), profile);
        }
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
            if (guesses.contains(guess))
                return false;

            guesses.add(guess);

            if (guess.equalsIgnoreCase(this.word))
                return true;

            if (isWon() || isLost())
                return true;

            tries--;
            return false;
        }

        public LetterState[] getLetterStates(String guess) {
            // TRANSLATED TO JAVA FROM: https://breq.dev/projects/wordle

            ArrayList<@Nullable Character> targetLetters = new ArrayList<>(Lists.charactersOf(this.word));

            int length = this.word.length();
            LetterState[] letterStates = new LetterState[length];
            // Mark correct letters as correct
            for (int i = 0; i < length; i++) {
                if (guess.charAt(i) == this.word.charAt(i)) {
                    // Remove matching green letters from the pool
                    // so that they aren't also matched as yellows
                    targetLetters.set(i, null);
                    letterStates[i] = LetterState.CORRECT;
                } else {
                    letterStates[i] = LetterState.INCORRECT;
                }
            }

            // Second pass: greedily match yellow letters
            for (int letterToColorIndex = 0; letterToColorIndex < length; letterToColorIndex++) {
                // Only change letters that were not marked as correct
                if (letterStates[letterToColorIndex] == LetterState.CORRECT) continue;
                // Yellow letters are matched by searching the entire target word
                int letterIndexInTarget = targetLetters.indexOf(guess.charAt(letterToColorIndex));
                if (letterIndexInTarget == -1) continue;
                // Remove yellow letters once matched,
                // each letter only matches once
                targetLetters.set(letterIndexInTarget, null);
                letterStates[letterToColorIndex] = LetterState.WRONG_POSITION;
            }

            return letterStates;
        }

        public Map<Character, LetterState> getCharacterColors() {
            // TRANSLATED TO JAVA FROM: https://breq.dev/projects/wordle

            HashMap<Character, LetterState> letters = new HashMap<>();

            for (String guess : this.guesses) {
                LetterState[] letterStates = getLetterStates(guess);

                for (int i = 0; i < guess.length(); i++) {
                    LetterState letterState = letterStates[i];
                    char letter = guess.charAt(i);
                    boolean noExistingData = letters.getOrDefault(letter, LetterState.NOT_GUESSED) == LetterState.NOT_GUESSED;

                    if (noExistingData && letterState == LetterState.NOT_GUESSED) {
                        letters.put(letter, LetterState.INCORRECT);
                    }
                    if (noExistingData || letterState == LetterState.CORRECT) {
                        letters.put(letter, letterState);
                    }
                }
            }

            return ImmutableMap.copyOf(letters);
        }

        public boolean isWon() {
            return guesses.stream().anyMatch(word -> word.equalsIgnoreCase(this.word));
        }

        @Getter
        public enum LetterState {
            CORRECT(new Color(0x538d4e)),
            INCORRECT(new Color(0x3a3a3c)),
            WRONG_POSITION(new Color(0xb59f3b)),
            NOT_GUESSED(new Color(0x818384));

            private final Color color;

            LetterState(Color color) {
                this.color = color;
            }
        }
    }
}
