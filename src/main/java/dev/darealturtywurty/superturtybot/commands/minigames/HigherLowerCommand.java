package dev.darealturtywurty.superturtybot.commands.minigames;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

public class HigherLowerCommand extends CoreCommand {
    private static final Map<String, Pair<Integer, Long>> COUNTRY_POPULATIONS = new HashMap<>();
    private static final Map<String, Long> COUNTRY_AREAS = new HashMap<>();
    private static final List<String> WORLD_LIST = new ArrayList<>();

    private static final String POPULATION_API_URL = "https://countriesnow.space/api/v0.1/countries/population";
    private static final String WORD_LIST_API_URL = "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt";
    private static final String WORD_FREQUENCY_API_URL = "https://api.datamuse.com/words?sp=%s&md=f&max=1";

    private static final Map<Long, Game> GAMES = new HashMap<>();

//    static {
//        var populationRequest = new Request.Builder().url(POPULATION_API_URL).build();
//
//        try (Response response = Constants.HTTP_CLIENT.newCall(populationRequest).execute()) {
//            if (response.isSuccessful()) {
//                String result = response.body() != null ? response.body().string() : null;
//                JsonObject json = Constants.GSON.fromJson(result, JsonObject.class);
//                if (json != null && !json.get("error").getAsBoolean()) {
//                    boolean foundAfghanistan = false;
//
//                    JsonArray data = json.getAsJsonArray("data");
//                    for (JsonElement datum : data) {
//                        JsonObject country = datum.getAsJsonObject();
//                        if ("Afghanistan".equalsIgnoreCase(country.get("country").getAsString()))
//                            foundAfghanistan = true;
//
//                        if (!foundAfghanistan) continue;
//
//                        String countryName = country.get("country").getAsString();
//                        JsonArray populationCounts = country.getAsJsonArray("populationCounts");
//                        JsonObject object = StreamSupport.stream(populationCounts.spliterator(), false)
//                                .map(JsonElement::getAsJsonObject)
//                                .max(Comparator.comparingLong(value -> value.getAsJsonObject().get("year").getAsLong()))
//                                .orElseThrow();
//
//                        COUNTRY_POPULATIONS.put(countryName,
//                                Pair.of(object.get("year").getAsInt(), object.get("value").getAsLong()));
//                    }
//
//                    System.out.println("Loaded " + COUNTRY_POPULATIONS.size() + " country populations!");
//                }
//            }
//        } catch (IOException exception) {
//            throw new IllegalStateException("Failed to fetch country populations!", exception);
//        }
//
//        try {
//            var landAreaPath = Path.of("src/main/resources/country_land_area.json");
//            JsonArray landAreaJson = Constants.GSON.fromJson(Files.readString(landAreaPath), JsonArray.class);
//            if (landAreaJson != null) {
//                for (JsonElement element : landAreaJson) {
//                    JsonObject object = element.getAsJsonObject();
//                    COUNTRY_AREAS.put(object.get("country").getAsString(), object.get("landAreaKm").getAsLong());
//                }
//
//                System.out.println("Loaded " + COUNTRY_AREAS.size() + " country land areas!");
//            }
//        } catch (IOException exception) {
//            throw new IllegalStateException("Failed to fetch country land areas!", exception);
//        }
//
//        Request wordListRequest = new Request.Builder().url(WORD_LIST_API_URL).build();
//
//        try (Response response = Constants.HTTP_CLIENT.newCall(wordListRequest).execute()) {
//            if (response.isSuccessful()) {
//                String result = response.body() != null ? response.body().string() : null;
//                String[] words = result != null ? result.split("\n") : new String[0];
//
//                for (String s : words) {
//                    String word = s;
//                    if (word.length() < 3) continue;
//
//                    word = word.toLowerCase(Locale.ROOT).trim();
//                    WORLD_LIST.add(word);
//                }
//
//                System.out.println("Loaded " + WORLD_LIST.size() + " words!");
//            }
//        } catch (IOException exception) {
//            throw new IllegalStateException("Failed to fetch word list!", exception);
//        }
//    }

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
        return "/higherlower population|area|word_frequency|trending";
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

        switch (subcommand) {
            case "population" -> {
                // make a copy of the map
                Map<String, Pair<Integer, Long>> countryPopulations = new HashMap<>(COUNTRY_POPULATIONS);

                Map.Entry<String, Pair<Integer, Long>> country0 = countryPopulations.entrySet().stream()
                        .skip((int) (Math.random() * countryPopulations.size())).findFirst().orElseThrow();
                countryPopulations.remove(country0.getKey(), country0.getValue());

                Map.Entry<String, Pair<Integer, Long>> country1 = countryPopulations.entrySet().stream()
                        .skip((int) (Math.random() * countryPopulations.size())).findFirst().orElseThrow();

                String toSend = String.format("Does %s have a higher or lower population than %s?", country0.getKey(),
                        country1.getKey());
                event.deferReply().setContent(toSend).flatMap(InteractionHook::retrieveOriginal).queue(message -> {
                    message.createThreadChannel(event.getUser().getName() + "'s Game").queue(threadChannel -> {
                        GAMES.put(threadChannel.getIdLong(),
                                new PopulationGame(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                        threadChannel.getIdLong(), event.getUser().getIdLong(), message.getIdLong(),
                                        message.getIdLong(), country0.getKey(), country0.getValue().getKey(),
                                        country0.getValue().getValue(), country1.getKey(), country1.getValue().getKey(),
                                        country1.getValue().getValue()));

                        threadChannel.sendMessage("Game started! " + event.getUser().getAsMention()).queue();
                    });

                    var actionRow = ActionRow.of(
                            Button.primary("higherlower:population:higher-" + message.getId(), "Higher"),
                            Button.primary("higherlower:population:lower-" + message.getId(), "Lower"));

                    message.editMessageComponents(actionRow).queue();
                });
            }
            case "area" -> {
                // make a copy of the map
                Map<String, Long> countryAreas = new HashMap<>(COUNTRY_AREAS);

                Map.Entry<String, Long> country0 = countryAreas.entrySet().stream()
                        .skip((int) (Math.random() * countryAreas.size())).findFirst().orElseThrow();
                countryAreas.remove(country0.getKey(), country0.getValue());

                Map.Entry<String, Long> country1 = countryAreas.entrySet().stream()
                        .skip((int) (Math.random() * countryAreas.size())).findFirst().orElseThrow();

                String toSend = String.format("Does %s have a higher or lower area than %s?", country0.getKey(),
                        country1.getKey());
                event.deferReply().setContent(toSend).flatMap(InteractionHook::retrieveOriginal).queue(message -> {
                    message.createThreadChannel(event.getUser().getName() + "'s Game").queue(threadChannel -> {
                        GAMES.put(threadChannel.getIdLong(),
                                new AreaGame(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                        threadChannel.getIdLong(), event.getUser().getIdLong(), message.getIdLong(),
                                        message.getIdLong(), country0.getKey(), country0.getValue(), country1.getKey(),
                                        country1.getValue()));

                        threadChannel.sendMessage("Game started! " + event.getUser().getAsMention()).queue();
                    });

                    var actionRow = ActionRow.of(Button.primary("higherlower:area:higher-" + message.getId(), "Higher"),
                            Button.primary("higherlower:area:lower-" + message.getId(), "Lower"));

                    message.editMessageComponents(actionRow).queue();
                });
            }
            case "word_frequency" -> {
                // choose a random word from the word list
                String word0 = WORLD_LIST.get((int) (Math.random() * WORLD_LIST.size()));
                String word1 = WORLD_LIST.get((int) (Math.random() * WORLD_LIST.size()));

                // get the frequency of the words
                float frequency0 = getWordFrequency(word0);
                float frequency1 = getWordFrequency(word1);

                String toSend = String.format("Does the word `%s` have a higher or lower frequency than the word `%s`?",
                        word0, word1);

                event.deferReply().setContent(toSend).flatMap(InteractionHook::retrieveOriginal).queue(message -> {
                    message.createThreadChannel(event.getUser().getName() + "'s Game").queue(threadChannel -> {
                        GAMES.put(threadChannel.getIdLong(),
                                new WordFrequencyGame(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                        threadChannel.getIdLong(), event.getUser().getIdLong(), message.getIdLong(),
                                        message.getIdLong(), word0, frequency0, word1, frequency1));

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
                return;
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

        if (game instanceof PopulationGame populationGame) {
            if (option.equals("higher")) {
                if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                    event.deferEdit().setComponents().queue();
                } else {
                    event.deferEdit().queue();
                }

                if (populationGame.getPopulation0() > populationGame.getPopulation1()) {
                    threadChannel.sendMessage(
                                    "✅ Correct! " + populationGame.getCountry0() + " has a higher population than " + populationGame.getCountry1())
                            .queue();

                    // change the second country to the first country and get a new country
                    populations(threadChannel, populationGame);
                } else {
                    threadChannel.sendMessage(
                                    "❌ Incorrect! " + populationGame.getCountry0() + " has a lower population than " + populationGame.getCountry1())
                            .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                    GAMES.remove(game.getMessageId());
                }
            } else if (option.equals("lower")) {
                if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                    event.deferEdit().setComponents().queue();
                } else {
                    event.deferEdit().queue();
                }

                if (populationGame.getPopulation0() < populationGame.getPopulation1()) {
                    threadChannel.sendMessage(
                                    "✅ Correct! " + populationGame.getCountry0() + " has a lower population than " + populationGame.getCountry1())
                            .queue();

                    // change the second country to the first country and get a new country
                    populations(threadChannel, populationGame);
                } else {
                    threadChannel.sendMessage(
                                    "❌ Incorrect! " + populationGame.getCountry0() + " has a higher population than " + populationGame.getCountry1())
                            .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                    GAMES.remove(game.getMessageId());
                }
            } else {
                event.reply("❌ Unknown option!").setEphemeral(true).queue();
            }
        } else if (game instanceof AreaGame areaGame) {
            if (option.equals("higher")) {
                if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                    event.deferEdit().setComponents().queue();
                } else {
                    event.deferEdit().queue();
                }

                if (areaGame.getArea0() > areaGame.getArea1()) {
                    threadChannel.sendMessage(
                                    "✅ Correct! " + areaGame.getCountry0() + " has a higher area than " + areaGame.getCountry1())
                            .queue();

                    // change the second country to the first country and get a new country
                    countryAreas(threadChannel, areaGame);
                } else {
                    threadChannel.sendMessage(
                                    "❌ Incorrect! " + areaGame.getCountry0() + " has a lower area than " + areaGame.getCountry1())
                            .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                    GAMES.remove(game.getMessageId());
                }
            } else if (option.equals("lower")) {
                if (event.getChannel().getIdLong() == game.getOwnerChannelId()) {
                    event.deferEdit().setComponents().queue();
                } else {
                    event.deferEdit().queue();
                }

                if (areaGame.getArea0() < areaGame.getArea1()) {
                    threadChannel.sendMessage(
                                    "✅ Correct! " + areaGame.getCountry0() + " has a lower area than " + areaGame.getCountry1())
                            .queue();

                    // change the second country to the first country and get a new country
                    countryAreas(threadChannel, areaGame);
                } else {
                    threadChannel.sendMessage(
                                    "❌ Incorrect! " + areaGame.getCountry0() + " has a higher area than " + areaGame.getCountry1())
                            .queue(message -> threadChannel.getManager().setArchived(true).setLocked(true).queue());

                    GAMES.remove(game.getMessageId());
                }
            } else {
                event.reply("❌ Unknown option!").setEphemeral(true).queue();
            }
        } else if (game instanceof WordFrequencyGame wordFrequencyGame) {
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
    }

    private static void countryAreas(ThreadChannel threadChannel, AreaGame areaGame) {
        Map<String, Long> countryAreas = new HashMap<>(COUNTRY_AREAS);
        countryAreas.remove(areaGame.getCountry0());

        Map.Entry<String, Long> country1 = countryAreas.entrySet().stream()
                .skip((int) (Math.random() * countryAreas.size())).findFirst().orElseThrow();

        areaGame.set0(areaGame.getCountry1(), areaGame.getArea1());
        areaGame.set1(country1.getKey(), country1.getValue());

        String toSend = String.format("Does %s have a higher or lower area than %s?", areaGame.getCountry0(),
                areaGame.getCountry1());

        threadChannel.sendMessage(toSend).queue(message -> {
            var actionRow = ActionRow.of(Button.primary("higherlower:area:higher-" + message.getId(), "Higher"),
                    Button.primary("higherlower:area:lower-" + message.getId(), "Lower"));

            message.editMessageComponents(actionRow).queue();

            areaGame.setLatestMessageId(message.getIdLong());
        });
    }

    private static void populations(ThreadChannel threadChannel, PopulationGame populationGame) {
        Map<String, Pair<Integer, Long>> countryPopulations = new HashMap<>(COUNTRY_POPULATIONS);
        countryPopulations.remove(populationGame.getCountry0());

        Map.Entry<String, Pair<Integer, Long>> country1 = countryPopulations.entrySet().stream()
                .skip((int) (Math.random() * countryPopulations.size())).findFirst().orElseThrow();

        populationGame.set0(populationGame.getCountry1(), populationGame.getYear1(), populationGame.getPopulation1());
        populationGame.set1(country1.getKey(), country1.getValue().getKey(), country1.getValue().getValue());

        String toSend = String.format("Does %s have a higher or lower population than %s?",
                populationGame.getCountry0(), populationGame.getCountry1());

        threadChannel.sendMessage(toSend).queue(message -> {
            var actionRow = ActionRow.of(Button.primary("higherlower:population:higher-" + message.getId(), "Higher"),
                    Button.primary("higherlower:population:lower-" + message.getId(), "Lower"));

            message.editMessageComponents(actionRow).queue();

            populationGame.setLatestMessageId(message.getIdLong());
        });
    }

    private void findAndSendWord(ThreadChannel threadChannel, WordFrequencyGame wordFrequencyGame) {
        String word1 = WORLD_LIST.stream().skip((int) (Math.random() * WORLD_LIST.size())).findFirst().orElseThrow();
        float frequency1 = getWordFrequency(word1);

        wordFrequencyGame.set0(wordFrequencyGame.getWord1(), wordFrequencyGame.getFrequency1());
        wordFrequencyGame.set1(word1, frequency1);

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

    public static abstract class Game {
        private final long guildId, ownerChannelId, channelId, userId, messageId;
        private long latestMessageId;
        private final String subcommand;

        public Game(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, String subcommand) {
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.userId = userId;
            this.messageId = messageId;
            this.latestMessageId = latestMessageId;
            this.subcommand = subcommand;
        }

        public long getGuildId() {
            return this.guildId;
        }

        public long getOwnerChannelId() {
            return this.ownerChannelId;
        }

        public long getChannelId() {
            return this.channelId;
        }

        public long getUserId() {
            return this.userId;
        }

        public long getMessageId() {
            return this.messageId;
        }

        public long getLatestMessageId() {
            return this.latestMessageId;
        }

        public void setLatestMessageId(long latestMessageId) {
            this.latestMessageId = latestMessageId;
        }

        public String getSubcommand() {
            return this.subcommand;
        }
    }

    public static class PopulationGame extends Game {
        private String country0, country1;
        private int year0, year1;
        private long population0, population1;

        public PopulationGame(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, String country0, int year0, long population0, String country1, int year1, long population1) {
            super(guildId, ownerChannelId, channelId, userId, messageId, latestMessageId, "population");
            this.country0 = country0;
            this.year0 = year0;
            this.population0 = population0;
            this.country1 = country1;
            this.year1 = year1;
            this.population1 = population1;
        }

        public String getCountry0() {
            return this.country0;
        }

        public int getYear0() {
            return this.year0;
        }

        public long getPopulation0() {
            return this.population0;
        }

        public String getCountry1() {
            return this.country1;
        }

        public int getYear1() {
            return this.year1;
        }

        public long getPopulation1() {
            return this.population1;
        }

        public void set0(String country, int year, long population) {
            this.country0 = country;
            this.year0 = year;
            this.population0 = population;
        }

        public void set1(String country, int year, long population) {
            this.country1 = country;
            this.year1 = year;
            this.population1 = population;
        }
    }

    public static class AreaGame extends Game {
        private String country0, country1;
        private long area0, area1;

        public AreaGame(long guildId, long ownerChannelId, long channelId, long userId, long messageId, long latestMessageId, String country0, long area0, String country1, long area1) {
            super(guildId, ownerChannelId, channelId, userId, messageId, latestMessageId, "area");
            this.country0 = country0;
            this.area0 = area0;
            this.country1 = country1;
            this.area1 = area1;
        }

        public String getCountry0() {
            return this.country0;
        }

        public long getArea0() {
            return this.area0;
        }

        public String getCountry1() {
            return this.country1;
        }

        public long getArea1() {
            return this.area1;
        }

        public void set0(String country, long area) {
            this.country0 = country;
            this.area0 = area;
        }

        public void set1(String country, long area) {
            this.country1 = country;
            this.area1 = area;
        }
    }

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

        public String getWord0() {
            return this.word0;
        }

        public float getFrequency0() {
            return this.frequency0;
        }

        public String getWord1() {
            return this.word1;
        }

        public float getFrequency1() {
            return this.frequency1;
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
