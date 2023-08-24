package dev.darealturtywurty.superturtybot.commands.minigames;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class WordleCommand extends CoreCommand {
    private static final Map<Long, String> GUILD_WORDS = new HashMap<>();
    private static final AtomicReference<String> GLOBAL_WORD = new AtomicReference<>();

    private static final String API_URL = "https://api.turtywurty.dev/words/random?length=5";
    private static final BufferedImage DEFAULT_IMAGE;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Long, List<Game>> GAMES = new HashMap<>();

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
        event.deferReply().queue();

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
            IOUtils.write(stream.toByteArray(), new BufferedOutputStream(Files.newOutputStream(Constants.WORDLE_FILE)));
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
            IOUtils.write(stream.toByteArray(), new BufferedOutputStream(Files.newOutputStream(Constants.WORDLE_FILE)));
            byte[] data = stream.toByteArray();
            FileUpload upload = FileUpload.fromData(data, "wordle.png");

            event.getChannel()
                    .asTextChannel()
                    .createThreadChannel(event.getUser().getName() + "'s Wordle Game")
                    .setInvitable(false)
                    .queue(thread -> {
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
            GAMES.computeIfAbsent(channelId, id -> new ArrayList<>()).add(game);
        });

        return createImage(word, List.of());
    }

    private static BufferedImage createImage(String word, List<String> guesses) {
//        try {
//            Graphics2D graphics = DEFAULT_IMAGE.createGraphics();
//
//        } catch (IOException exception) {
//
//        }

        return null;
    }

    @Getter
    public static class Game {
        private final String word;
        private final List<Character> guessed;
        private final List<Character> correct;
        private final List<Character> incorrect;
        @Setter
        private int tries;

        private final @Nullable Long guildId;
        private final long channelId;
        private final long messageId;
        private final long userId;

        public Game(String word, @Nullable Long guildId, long channelId, long messageId, long userId) {
            this.word = word;
            this.tries = 6;
            this.guessed = new ArrayList<>();
            this.correct = new ArrayList<>();
            this.incorrect = new ArrayList<>();

            this.guildId = guildId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
        }

        public boolean isOver() {
            return tries == 0 || correct.size() == word.length();
        }

        public boolean isWon() {
            return correct.size() == word.length();
        }

        public boolean isLost() {
            return tries == 0;
        }

        // TODO: Got here!
        public boolean guess(String guess) {
            if (guess.length() != 1) {
                return false;
            }

            char character = guess.charAt(0);
            if (guessed.contains(character)) {
                return false;
            }

            guessed.add(character);
            if (word.indexOf(character) != -1) {
                correct.add(character);
                return true;
            } else {
                incorrect.add(character);
                tries--;
                return false;
            }
        }
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

            Files.writeString(Constants.WORDLE_FILE, Constants.GSON.toJson(jsonToStore), StandardCharsets.UTF_8);
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
            if (Files.exists(Constants.WORDLE_FILE)) {
                String jsonStr = Files.readString(Constants.WORDLE_FILE, StandardCharsets.UTF_8);
                JsonObject json = Constants.GSON.fromJson(jsonStr, JsonObject.class);

                String word = json.get("word").getAsString();
                if (isValidWord(word)) {
                    GLOBAL_WORD.set(word);
                } else {
                    fetchAndStoreWords();
                }

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
}
