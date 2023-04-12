package dev.darealturtywurty.superturtybot.modules.minigames;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class TriviaCommand extends CoreCommand {
    private static final List<TriviaData> CACHED_TRIVIA = new ArrayList<>();

    private static final String URL = "https://the-trivia-api.com/api/questions?limit=1";
    private static final String CATEGORIES_URL = "https://the-trivia-api.com/api/categories";
    private final List<String> categories = new ArrayList<>();

    public TriviaCommand() {
        super(new Types(true, false, false, false));

        Constants.HTTP_CLIENT.newCall(new Request.Builder().url(CATEGORIES_URL).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                throw new IllegalStateException("Failed to get trivia categories!", exception);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IllegalStateException(
                            "Unable to get trivia categories! Response code: " + response.code());
                }

                if (response.body() == null) return;

                JsonObject json = Constants.GSON.fromJson(response.body().string(), JsonObject.class);

                if (json == null) return;

                json.keySet().stream().map(json::getAsJsonArray)
                        .forEach(array -> array.forEach(element -> categories.add(element.getAsString())));
            }
        });
    }

    public record TriviaQuestion(String category, String id, String correctAnswer, List<String> incorrectAnswers,
                                 String question, List<String> tags, String type, String difficulty,
                                 List<String> regions, boolean isNiche) {
    }

    public record TriviaData(String selectMenuId, long guildId, long channelId, long messageId, long userId,
                             TriviaQuestion question) {
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName())) return;
        if (!"category".equals(event.getFocusedOption().getName())) return;

        System.out.println(this.categories);
        event.replyChoiceStrings(this.categories).queue();
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play trivia with your friends!";
    }

    @Override
    public String getName() {
        return "trivia";
    }

    @Override
    public String getRichName() {
        return "Trivia";
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "category", "The category of trivia to play", false, true),
                new OptionData(OptionType.STRING, "difficulty", "The difficulty of the trivia", false).addChoice("Easy",
                        "easy").addChoice("Medium", "medium").addChoice("Hard", "hard"));
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "This command can only be used in a guild!");
            return;
        }

        String category = event.getOption("category", null, OptionMapping::getAsString);
        if (category != null && category.isBlank()) {
            category = null;
        }

        if (category != null && !this.categories.contains(category)) {
            reply(event, "Invalid category! Please use one of the following: " + String.join(", ", this.categories));
            return;
        }

        String url = URL;
        if (category != null) {
            url += "&categories=" + category;
        }

        String difficulty = event.getOption("difficulty", null, OptionMapping::getAsString);
        if (difficulty != null && difficulty.isBlank()) {
            difficulty = null;
        }

        if (difficulty != null && !difficulty.equalsIgnoreCase("easy") && !difficulty.equalsIgnoreCase(
                "medium") && !difficulty.equalsIgnoreCase("hard")) {
            reply(event, "Invalid difficulty! Please use one of the following: easy, medium, hard");
            return;
        }

        if (difficulty != null) {
            url += "&difficulty=" + difficulty;
        }

        event.deferReply().setContent("Loading...").queue();

        CompletableFuture<Optional<TriviaQuestion>> future = getTrivia(url);
        future.thenAcceptAsync(optional -> {
            if (optional.isEmpty()) {
                event.getHook().editOriginal("❌ Failed to get trivia!").queue();
                return;
            }

            TriviaQuestion question = optional.get();
            String selectId = UUID.randomUUID().toString();
            StringSelectMenu.Builder selectMenu = StringSelectMenu.create(selectId)
                    .setPlaceholder("Select an answer...");

            List<String> answers = new ArrayList<>(question.incorrectAnswers);
            answers.add(question.correctAnswer);
            Collections.shuffle(answers);

            for (String answer : answers) {
                selectMenu.addOption(answer, answer);
            }

            event.getHook().editOriginal(event.getUser().getAsMention() + " Here's your trivia question!").setEmbeds(
                            new EmbedBuilder().setTitle(question.question()).setDescription(
                                            "Category: " + question.category() + "\nDifficulty: " + question.difficulty())
                                    .setTimestamp(Instant.now()).setColor(
                                            question.difficulty().equalsIgnoreCase("easy") ? 0x00FF00 : question.difficulty()
                                                    .equalsIgnoreCase("medium") ? 0xFFFF00 : 0xFF0000).build())
                    .setActionRow(selectMenu.build()).queue(msg -> CACHED_TRIVIA.add(
                            new TriviaData(selectId, event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                    msg.getIdLong(), event.getUser().getIdLong(), question)));
        });
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;

        TriviaData data = CACHED_TRIVIA.stream()
                .filter(triviaData -> triviaData.selectMenuId().equals(event.getComponentId()))
                .filter(triviaData -> triviaData.guildId() == event.getGuild().getIdLong())
                .filter(triviaData -> triviaData.channelId() == event.getChannel().getIdLong())
                .filter(triviaData -> triviaData.messageId() == event.getMessage().getIdLong()).findFirst()
                .orElse(null);

        if (data == null) return;

        event.deferEdit().queue();
        if (data.userId() != event.getUser().getIdLong()) {
            return;
        }

        String response = event.getValues().get(0);
        String correctAnswer = data.question().correctAnswer();

        if (correctAnswer.equals(response)) {
            event.getMessage().reply("✅ " + event.getUser().getAsMention() + ", you are correct! " + correctAnswer +
                    " was indeed the correct answer.").queue();
            event.getHook().editOriginalComponents().queue();

            int xpTally = 0;
            if (data.question().difficulty().equalsIgnoreCase("easy")) {
                xpTally = 10;
            } else if (data.question().difficulty().equalsIgnoreCase("medium")) {
                xpTally = 20;
            } else if (data.question().difficulty().equalsIgnoreCase("hard")) {
                xpTally = 30;
            }

            if (data.question().isNiche()) {
                xpTally *= 2;
            }

            if (LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
                LevellingManager.INSTANCE.addXP(event.getGuild(), event.getUser(),
                        ThreadLocalRandom.current().nextInt(xpTally, xpTally * 2));
            }
        } else {
            event.getMessage().reply("❌" + event.getUser()
                    .getAsMention() + ", you are incorrect! The correct answer is " + correctAnswer).queue();
            event.getHook().editOriginalComponents().queue();
        }

        CACHED_TRIVIA.remove(data);
    }

    private static CompletableFuture<Optional<TriviaQuestion>> getTrivia(String url) {
        CompletableFuture<Optional<TriviaQuestion>> future = new CompletableFuture<>();

        Constants.HTTP_CLIENT.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                future.completeExceptionally(exception);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    future.completeExceptionally(
                            new IllegalStateException("Unable to get trivia! Response code: " + response.code()));
                    return;
                }

                JsonArray json = Constants.GSON.fromJson(body.string(), JsonArray.class);
                JsonObject trivia = json.get(0).getAsJsonObject();

                String category = trivia.get("category").getAsString();
                String id = trivia.get("id").getAsString();
                String correctAnswer = trivia.get("correctAnswer").getAsString();

                List<String> incorrectAnswers = new ArrayList<>();
                trivia.get("incorrectAnswers").getAsJsonArray().asList().stream().map(JsonElement::getAsString)
                        .forEach(incorrectAnswers::add);

                String question = trivia.get("question").getAsString();

                List<String> tags = new ArrayList<>();
                trivia.get("tags").getAsJsonArray().asList().stream().map(JsonElement::getAsString).forEach(tags::add);

                String type = trivia.get("type").getAsString();
                String difficulty = trivia.get("difficulty").getAsString();

                List<String> regions = new ArrayList<>();
                trivia.get("regions").getAsJsonArray().asList().stream().map(JsonElement::getAsString)
                        .forEach(regions::add);

                boolean isNiche = trivia.get("isNiche").getAsBoolean();

                future.complete(Optional.of(
                        new TriviaQuestion(category, id, correctAnswer, incorrectAnswers, question, tags, type,
                                difficulty, regions, isNiche)));
            }
        });

        return future;
    }
}