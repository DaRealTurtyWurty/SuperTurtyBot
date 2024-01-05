package dev.darealturtywurty.superturtybot.commands.minigames;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Region;
import dev.darealturtywurty.superturtybot.core.api.request.RandomWordRequestData;
import dev.darealturtywurty.superturtybot.core.api.request.RegionExcludeRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HigherLowerCommand extends CoreCommand {
    private static final String WORD_FREQUENCY_API_URL = "https://api.datamuse.com/words?sp=%s&md=f&max=1";
    private static final Map<Long, Game> GAMES = new HashMap<>();

    public HigherLowerCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("population", "Population of countries"),
                new SubcommandData("area", "Area of countries"),
                new SubcommandData("word_frequency", "Word frequency in the English language"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Higher or lower game";
    }

    @Override
    public String getName() {
        return "higherlower";
    }

    @Override
    public String getRichName() {
        return "Higher or Lower";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public String getHowToUse() {
        return "/higherlower population|area|word_frequency";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 10L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        // check that this user does not already have a running game in this server
        if (GAMES.values().stream().anyMatch(
                game -> game.getGuildId() == event.getGuild().getIdLong() && game.getUserId() == event.getUser()
                        .getIdLong())) {
            reply(event, "❌ You already have a game running in this channel!", false, true);
            return;
        }

        final var subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        event.deferReply().queue();

        switch (subcommand) {
            case "population" -> {
                RegionExcludeRequestData requestData = new RegionExcludeRequestData.Builder()
                        .excludeTerritories().build();

                // make a copy of the map
                Either<Region, HttpStatus> country0 = ApiHandler.getTerritoryData(requestData);
                int attempts = 0;
                while (country0.isRight() && attempts < 5) {
                    country0 = ApiHandler.getTerritoryData(requestData);
                    attempts++;
                }

                if (country0.isRight()) {
                    event.getHook().editOriginal("❌ An error occurred while getting the country data!").queue();
                    return;
                }

                Either<Region, HttpStatus> country1 = ApiHandler.getTerritoryData(requestData);
                attempts = 0;
                while ((country1.isRight() ||
                        country0.getLeft().getCca3().equals(country1.getLeft().getCca3())) &&
                        attempts < 5) {
                    country1 = ApiHandler.getTerritoryData(requestData);
                    attempts++;
                }

                if (country1.isRight()) {
                    event.getHook().editOriginal("❌ An error occurred while getting the country data!").queue();
                    return;
                }

                Region region0 = country0.getLeft();
                Region region1 = country1.getLeft();

                String toSend = String.format("Does %s have a higher or lower population than %s?", region0.getName(),
                        region1.getName());
                event.getHook().editOriginal(toSend).queue(message -> {
                    message.createThreadChannel(event.getUser().getName() + "'s Game").queue(threadChannel -> {
                        GAMES.put(threadChannel.getIdLong(),
                                new PopulationGame(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                        threadChannel.getIdLong(), event.getUser().getIdLong(), message.getIdLong(),
                                        message.getIdLong(), region0, region1));

                        threadChannel.sendMessage("Game started! " + event.getUser().getAsMention()).queue();
                    });

                    var actionRow = ActionRow.of(
                            Button.primary("higherlower:population:higher-" + message.getId(), "Higher"),
                            Button.primary("higherlower:population:lower-" + message.getId(), "Lower"));

                    message.editMessageComponents(actionRow).queue();
                });
            }
            case "area" -> {
                Either<Region, HttpStatus> country0 = ApiHandler.getTerritoryData();
                int attempts = 0;
                while (country0.isRight() && attempts < 5) {
                    country0 = ApiHandler.getTerritoryData();
                    attempts++;
                }

                if (country0.isRight()) {
                    event.getHook().editOriginal("❌ An error occurred while getting the country data!").queue();
                    return;
                }

                Either<Region, HttpStatus> country1 = ApiHandler.getTerritoryData();
                attempts = 0;
                while ((country1.isRight() ||
                        country0.getLeft().getCca3().equals(country1.getLeft().getCca3())) &&
                        attempts < 5) {
                    country1 = ApiHandler.getTerritoryData();
                    attempts++;
                }

                if (country1.isRight()) {
                    event.getHook().editOriginal("❌ An error occurred while getting the country data!").queue();
                    return;
                }

                Region region0 = country0.getLeft();
                Region region1 = country1.getLeft();

                String toSend = String.format("Does %s have a higher or lower area than %s?", region0.getName(),
                        region1.getName());
                event.getHook().editOriginal(toSend).queue(message -> {
                    message.createThreadChannel(event.getUser().getName() + "'s Game").queue(threadChannel -> {
                        GAMES.put(threadChannel.getIdLong(),
                                new AreaGame(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                        threadChannel.getIdLong(), event.getUser().getIdLong(), message.getIdLong(),
                                        message.getIdLong(), region0, region1));

                        threadChannel.sendMessage("Game started! " + event.getUser().getAsMention()).queue();
                    });

                    var actionRow = ActionRow.of(Button.primary("higherlower:area:higher-" + message.getId(), "Higher"),
                            Button.primary("higherlower:area:lower-" + message.getId(), "Lower"));

                    message.editMessageComponents(actionRow).queue();
                });
            }
            case "word_frequency" -> {
                var requestData = new RandomWordRequestData.Builder()
                        .amount(1)
                        .build();

                Either<List<String>, HttpStatus> word0 = ApiHandler.getWords(requestData);
                int attempts = 0;
                while (word0.isRight() && attempts < 5) {
                    word0 = ApiHandler.getWords(requestData);
                    attempts++;
                }

                if (word0.isRight()) {
                    event.getHook().editOriginal("❌ An error occurred while getting the word list!").queue();
                    return;
                }

                Either<List<String>, HttpStatus> word1 = ApiHandler.getWords(requestData);
                attempts = 0;
                while ((word1.isRight() ||
                        word0.getLeft().getFirst().equals(word1.getLeft().getFirst())) &&
                        attempts < 5) {
                    word1 = ApiHandler.getWords(requestData);
                    attempts++;
                }

                if (word1.isRight()) {
                    event.getHook().editOriginal("❌ An error occurred while getting the word list!").queue();
                    return;
                }

                String word0Str = word0.getLeft().getFirst();
                String word1Str = word1.getLeft().getFirst();
                float frequency0 = getWordFrequency(word0Str);
                float frequency1 = getWordFrequency(word1Str);

                String toSend = String.format("Does the word `%s` have a higher or lower frequency than the word `%s`?",
                        word0Str, word1Str);

                event.getHook().editOriginal(toSend).queue(message -> {
                    message.createThreadChannel(event.getUser().getName() + "'s Game").queue(threadChannel -> {
                        GAMES.put(threadChannel.getIdLong(),
                                new WordFrequencyGame(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                        threadChannel.getIdLong(), event.getUser().getIdLong(), message.getIdLong(),
                                        message.getIdLong(), word0Str, frequency0, word1Str, frequency1));

                        threadChannel.sendMessage("Game started! " + event.getUser().getAsMention()).queue();
                    });

                    var actionRow = ActionRow.of(
                            Button.primary("higherlower:word_frequency:higher-" + message.getId(), "Higher"),
                            Button.primary("higherlower:word_frequency:lower-" + message.getId(), "Lower"));

                    message.editMessageComponents(actionRow).queue();
                });
            }
            default -> {
                reply(event, "❌ Unknown subcommand!", false, true);
            }
        }
    }

    private static float getWordFrequency(String word) {
        Request request = new Request.Builder().url(WORD_FREQUENCY_API_URL.formatted(word)).build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            ResponseBody body = response.body();
            if (body == null) {
                return 0;
            }

            String bodyString = body.string();
            if (bodyString.isBlank()) {
                return 0;
            }

            JsonArray array = Constants.GSON.fromJson(bodyString, JsonArray.class);
            if (array.isEmpty()) {
                return 0;
            }

            JsonObject object = array.get(0).getAsJsonObject();
            if (!object.has("tags")) {
                return 0;
            }

            JsonArray tags = object.getAsJsonArray("tags");
            if (tags.isEmpty()) {
                return 0;
            }

            String frequencyStr = tags.get(0).getAsString();
            if (frequencyStr.isBlank()) {
                return 0;
            }

            return Float.parseFloat(frequencyStr.substring(2));
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while getting the word frequency!", exception);
            return 0;
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;

        String id = event.getButton().getId();
        if (id == null) return;

        if (!id.startsWith("higherlower:")) return;

        String[] split = event.getButton().getId().split("-");
        if (split.length != 2) {
            event.reply("❌ Invalid button!").setEphemeral(true).queue();
            return;
        }

        String option = split[0].split(":")[2];
        long messageId = Long.parseLong(split[1]);

        if (event.getMessageIdLong() != messageId) return;

        Game game = GAMES.values().stream().filter(g -> g.getLatestMessageId() == messageId).findFirst().orElse(null);
        if (game == null) {
            event.reply("❌ No game is running in this channel!").setEphemeral(true).queue();
            return;
        }

        if (game.getOwnerChannelId() != event.getChannel().getIdLong() && game.getChannelId() != event.getChannel()
                .getIdLong()) {
            event.reply("❌ This game is not in this channel!").setEphemeral(true).queue();
            return;
        }

        if (game.getGuildId() != event.getGuild().getIdLong()) {
            event.reply("❌ This game is not in this server!").setEphemeral(true).queue();
            return;
        }

        if (game.getUserId() != event.getUser().getIdLong()) {
            event.reply("❌ You are not the owner of this game!").setEphemeral(true).queue();
            return;
        }

        ThreadChannel threadChannel = event.getGuild().getThreadChannelById(game.getChannelId());
        if (threadChannel == null) {
            event.reply("❌ The thread channel for this game was deleted!").setEphemeral(true).queue();
            return;
        }

        if (game.getLatestMessageId() == game.getMessageId()) {
            TextChannel textChannel = event.getGuild().getTextChannelById(game.getOwnerChannelId());
            if (textChannel == null) {
                event.reply("❌ The text channel for this game was deleted!").setEphemeral(true).queue();
                return;
            }

            textChannel.retrieveMessageById(game.getLatestMessageId())
                    .queue(message -> message.editMessageComponents().queue());
        } else {
            threadChannel.retrieveMessageById(game.getLatestMessageId())
                    .queue(message -> message.editMessageComponents().queue());
        }

        switch (game) {
            case PopulationGame populationGame -> {
                if (option.equals("higher")) {
                    if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                        event.deferEdit().setComponents().queue();
                    } else {
                        event.deferEdit().queue();
                    }

                    if (populationGame.getCountry0().getPopulation() > populationGame.getCountry1().getPopulation()) {
                        threadChannel.sendMessage(
                                        "✅ Correct! " + populationGame.getCountry0().getName() + " has a higher population than " + populationGame.getCountry1().getName())
                                .queue();

                        // change the second country to the first country and get a new country
                        populations(threadChannel, populationGame);
                    } else {
                        threadChannel.sendMessage(
                                        "❌ Incorrect! " + populationGame.getCountry0().getName() + " has a lower population than " + populationGame.getCountry1().getName())
                                .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                        GAMES.remove(game.getMessageId());
                    }
                } else if (option.equals("lower")) {
                    if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                        event.deferEdit().setComponents().queue();
                    } else {
                        event.deferEdit().queue();
                    }

                    if (populationGame.getCountry0().getPopulation() < populationGame.getCountry1().getPopulation()) {
                        threadChannel.sendMessage(
                                        "✅ Correct! " + populationGame.getCountry0().getName() + " has a lower population than " + populationGame.getCountry1().getName())
                                .queue();

                        // change the second country to the first country and get a new country
                        populations(threadChannel, populationGame);
                    } else {
                        threadChannel.sendMessage(
                                        "❌ Incorrect! " + populationGame.getCountry0().getName() + " has a higher population than " + populationGame.getCountry1().getName())
                                .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                        GAMES.remove(game.getMessageId());
                    }
                } else {
                    event.reply("❌ Unknown option!").setEphemeral(true).queue();
                }
            }
            case AreaGame areaGame -> {
                if (option.equals("higher")) {
                    if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                        event.deferEdit().setComponents().queue();
                    } else {
                        event.deferEdit().queue();
                    }

                    if (areaGame.getCountry0().getLandAreaKm() > areaGame.getCountry1().getLandAreaKm()) {
                        threadChannel.sendMessage(
                                        "✅ Correct! " + areaGame.getCountry0().getName() + " has a higher area than " + areaGame.getCountry1().getName())
                                .queue();

                        // change the second country to the first country and get a new country
                        countryAreas(threadChannel, areaGame);
                    } else {
                        threadChannel.sendMessage(
                                        "❌ Incorrect! " + areaGame.getCountry0().getName() + " has a lower area than " + areaGame.getCountry1().getName())
                                .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                        GAMES.remove(game.getMessageId());
                    }
                } else if (option.equals("lower")) {
                    if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                        event.deferEdit().setComponents().queue();
                    } else {
                        event.deferEdit().queue();
                    }

                    if (areaGame.getCountry0().getLandAreaKm() < areaGame.getCountry1().getLandAreaKm()) {
                        threadChannel.sendMessage(
                                        "✅ Correct! " + areaGame.getCountry0().getName() + " has a lower area than " + areaGame.getCountry1().getName())
                                .queue();

                        // change the second country to the first country and get a new country
                        countryAreas(threadChannel, areaGame);
                    } else {
                        threadChannel.sendMessage(
                                        "❌ Incorrect! " + areaGame.getCountry0().getName() + " has a higher area than " + areaGame.getCountry1().getName())
                                .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                        GAMES.remove(game.getMessageId());
                    }
                } else {
                    event.reply("❌ Unknown option!").setEphemeral(true).queue();
                }
            }
            case WordFrequencyGame wordFrequencyGame -> {
                if (option.equals("higher")) {
                    if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                        event.deferEdit().setComponents().queue();
                    } else {
                        event.deferEdit().queue();
                    }

                    if (wordFrequencyGame.getFrequency0() > wordFrequencyGame.getFrequency1() || wordFrequencyGame.getFrequency0() == wordFrequencyGame.getFrequency1()) {
                        threadChannel.sendMessage(
                                        "✅ Correct! `" + wordFrequencyGame.getWord0() + "` has a higher word frequency " + "than `" + wordFrequencyGame.getWord1() + "`")
                                .queue();

                        // change the second country to the first country and get a new country
                        findAndSendWord(threadChannel, wordFrequencyGame);
                    } else {
                        threadChannel.sendMessage(
                                        "❌ Incorrect! `" + wordFrequencyGame.getWord0() + "` has a lower word frequency " + "than `" + wordFrequencyGame.getWord1() + "`")
                                .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                        GAMES.remove(game.getMessageId());
                    }
                } else if (option.equals("lower")) {
                    if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                        event.deferEdit().setComponents().queue();
                    } else {
                        event.deferEdit().queue();
                    }

                    if (wordFrequencyGame.getFrequency0() < wordFrequencyGame.getFrequency1() || wordFrequencyGame.getFrequency0() == wordFrequencyGame.getFrequency1()) {
                        threadChannel.sendMessage(
                                        "✅ Correct! `" + wordFrequencyGame.getWord0() + "` has a lower word frequency " + "than `" + wordFrequencyGame.getWord1() + "`")
                                .queue();

                        // change the second country to the first country and get a new country
                        findAndSendWord(threadChannel, wordFrequencyGame);
                    } else {
                        threadChannel.sendMessage(
                                        "❌ Incorrect! `" + wordFrequencyGame.getWord0() + "` has a higher word frequency " + "than `" + wordFrequencyGame.getWord1() + "`")
                                .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                        GAMES.remove(game.getMessageId());
                    }
                } else {
                    event.reply("❌ Unknown option!").setEphemeral(true).queue();
                }
            }
            default -> {
            }
        }
    }

    private static void countryAreas(ThreadChannel threadChannel, AreaGame areaGame) {
        RegionExcludeRequestData requestData = new RegionExcludeRequestData.Builder()
                .excludeTerritories().build();
        Either<Region, HttpStatus> country1 = ApiHandler.getTerritoryData(requestData);
        int attempts = 0;
        while (country1.isRight() && attempts < 5) {
            country1 = ApiHandler.getTerritoryData(requestData);
            attempts++;
        }

        areaGame.setCountry0(areaGame.getCountry1());
        areaGame.setCountry1(country1.getLeft());

        String toSend = String.format("Does %s have a higher or lower area than %s?", areaGame.getCountry0().getName(),
                areaGame.getCountry1().getName());

        threadChannel.sendMessage(toSend).queue(message -> {
            var actionRow = ActionRow.of(Button.primary("higherlower:area:higher-" + message.getId(), "Higher"),
                    Button.primary("higherlower:area:lower-" + message.getId(), "Lower"));

            message.editMessageComponents(actionRow).queue();

            areaGame.setLatestMessageId(message.getIdLong());
        });
    }

    private static void populations(ThreadChannel threadChannel, PopulationGame populationGame) {
        RegionExcludeRequestData requestData = new RegionExcludeRequestData.Builder()
                .excludeTerritories().build();
        Either<Region, HttpStatus> country1 = ApiHandler.getTerritoryData(requestData);
        int attempts = 0;
        while (country1.isRight() && attempts < 5) {
            country1 = ApiHandler.getTerritoryData(requestData);
            attempts++;
        }

        populationGame.setCountry0(populationGame.getCountry1());
        populationGame.setCountry1(country1.getLeft());

        String toSend = String.format("Does %s have a higher or lower population than %s?",
                populationGame.getCountry0().getName(), populationGame.getCountry1().getName());

        threadChannel.sendMessage(toSend).queue(message -> {
            var actionRow = ActionRow.of(Button.primary("higherlower:population:higher-" + message.getId(), "Higher"),
                    Button.primary("higherlower:population:lower-" + message.getId(), "Lower"));

            message.editMessageComponents(actionRow).queue();

            populationGame.setLatestMessageId(message.getIdLong());
        });
    }

    private void findAndSendWord(ThreadChannel threadChannel, WordFrequencyGame wordFrequencyGame) {
        String word0 = wordFrequencyGame.getWord1();
        float frequency0 = wordFrequencyGame.getFrequency1();

        var requestData = new RandomWordRequestData.Builder()
                .amount(1)
                .build();
        Either<List<String>, HttpStatus> word1 = ApiHandler.getWords(requestData);
        int attempts = 0;
        while (word1.isRight() && attempts < 5) {
            word1 = ApiHandler.getWords(requestData);
            attempts++;
        }

        if (word1.isRight()) {
            threadChannel.sendMessage("❌ An error occurred while getting the word list!").queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());
            GAMES.remove(wordFrequencyGame.getMessageId());
            return;
        }

        String word1Str = word1.getLeft().getFirst();
        float frequency1 = getWordFrequency(word1Str);

        wordFrequencyGame.set0(word0, frequency0);
        wordFrequencyGame.set1(word1Str, frequency1);

        String toSend = String.format("Does `%s` have a higher or lower word frequency than `%s`?",
                wordFrequencyGame.getWord0(), wordFrequencyGame.getWord1());

        threadChannel.sendMessage(toSend).queue(message -> {
            var actionRow = ActionRow.of(
                    Button.primary("higherlower:word_frequency:higher-" + message.getId(), "Higher"),
                    Button.primary("higherlower:word_frequency:lower-" + message.getId(), "Lower"));

            message.editMessageComponents(actionRow).queue();

            wordFrequencyGame.setLatestMessageId(message.getIdLong());
        });
    }

    @Getter
    public static abstract class Game {
        private final long guildId, ownerChannelId, channelId, userId, messageId;
        private final String subcommand;

        @Setter
        private long latestMessageId;

        public Game(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, String subcommand) {
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.userId = userId;
            this.messageId = messageId;
            this.latestMessageId = latestMessageId;
            this.subcommand = subcommand;
        }
    }


    @Getter
    @Setter
    public static class PopulationGame extends Game {
        private Region country0, country1;

        public PopulationGame(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, Region country0, Region country1) {
            super(guildId, ownerChannelId, channelId, userId, messageId, latestMessageId, "population");
            this.country0 = country0;
            this.country1 = country1;
        }
    }

    @Getter
    @Setter
    public static class AreaGame extends Game {
        private Region country0, country1;

        public AreaGame(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, Region country0, Region country1) {
            super(guildId, ownerChannelId, channelId, userId, messageId, latestMessageId, "area");
            this.country0 = country0;
            this.country1 = country1;
        }
    }

    @Getter
    public static class WordFrequencyGame extends Game {
        private String word0, word1;
        private float frequency0, frequency1;

        public WordFrequencyGame(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, String word0, float frequency0, String word1, float frequency1) {
            super(guildId, ownerChannelId, channelId, userId, messageId, latestMessageId, "word_frequency");
            this.word0 = word0;
            this.frequency0 = frequency0;
            this.word1 = word1;
            this.frequency1 = frequency1;
        }

        public void set0(String word, float frequency) {
            this.word0 = word;
            this.frequency0 = frequency;
        }

        public void set1(String word, float frequency) {
            this.word1 = word;
            this.frequency1 = frequency;
        }
    }
}
