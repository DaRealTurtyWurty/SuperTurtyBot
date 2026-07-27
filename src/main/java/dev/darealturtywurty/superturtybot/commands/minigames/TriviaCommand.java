package dev.darealturtywurty.superturtybot.commands.minigames;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu.Builder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TriviaCommand extends CoreCommand {
    private static final List<TriviaData> CACHED_TRIVIA = new CopyOnWriteArrayList<>();

    private static final String URL = "https://the-trivia-api.com/api/questions?limit=1";
    private static final String CATEGORIES_URL = "https://the-trivia-api.com/api/categories";
    private final List<String> categories = new ArrayList<>();

    public TriviaCommand() {
        super(new Types(true, false, false, false));

        Constants.HTTP_CLIENT.newCall(new Request.Builder()
                        .url(CATEGORIES_URL).get().build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                        throw new IllegalStateException("Failed to get trivia categories!", exception);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (!response.isSuccessful())
                            throw new IllegalStateException("Unable to get trivia categories! Response code: " + response.code());

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
                             TriviaQuestion question, List<String> answers) {
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals(getName()))
            return;

        if (!"category".equals(event.getFocusedOption().getName()))
            return;

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

        String category = event.getOption("category", OptionMapping::getAsString);
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

        String difficulty = event.getOption("difficulty", OptionMapping::getAsString);
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

        Guild guild = event.getGuild();
        TextChannel anchorChannel = resolveTriviaAnchor(guild);
        if (anchorChannel == null) {
            reply(event, "❌ I could not find a text channel where trivia threads can be hosted.");
            return;
        }

        if (!canHostTriviaThreads(guild, anchorChannel)) {
            reply(event, "❌ I need permission to view messages, read message history, create public threads, "
                    + "and send messages in threads in " + anchorChannel.getAsMention() + ".");
            return;
        }

        event.deferReply().queue();
        String threadName = event.getUser().getName() + " trivia";

        getTrivia(url).whenComplete((optional, triviaError) -> {
            if (triviaError != null || optional.isEmpty()) {
                if (triviaError != null) {
                    Constants.LOGGER.error("Failed to load trivia for user {}", event.getUser().getId(), triviaError);
                }
                event.getHook().editOriginal("❌ Failed to get trivia!").queue();
                return;
            }

            TriviaQuestion question = optional.get();
            getOrCreateTriviaThread(anchorChannel, threadName).whenComplete((thread, threadError) -> {
                if (threadError != null) {
                    Constants.LOGGER.error("Failed to open trivia thread for user {}", event.getUser().getId(),
                            threadError);
                    event.getHook().editOriginal("❌ I could not open your trivia thread in "
                            + anchorChannel.getAsMention() + ".").queue();
                    return;
                }

                thread.addThreadMember(event.getUser()).queue(null,
                        error -> Constants.LOGGER.debug("Could not add user {} to trivia thread {}",
                                event.getUser().getId(), thread.getId(), error));

                sendTriviaQuestion(thread, event, question).whenComplete((message, sendError) -> {
                    if (sendError != null) {
                        Constants.LOGGER.error("Failed to send trivia question in thread {}", thread.getId(),
                                sendError);
                        event.getHook().editOriginal("❌ I could not post the trivia question in "
                                + thread.getAsMention() + ".").queue();
                        return;
                    }

                    if (event.getChannel().getIdLong() == thread.getIdLong()) {
                        event.getHook().deleteOriginal().queue();
                    } else {
                        event.getHook().editOriginal(
                                "✅ Your trivia question is in " + thread.getAsMention() + ".").queue();
                    }
                });
            });
        });
    }

    private static TextChannel resolveTriviaAnchor(Guild guild) {
        GuildData config = GuildData.getOrCreateGuildData(guild);
        if (config.getTriviaChannel() != 0L) {
            TextChannel configuredChannel = guild.getTextChannelById(config.getTriviaChannel());
            if (configuredChannel != null)
                return configuredChannel;
        }

        TextChannel systemChannel = guild.getSystemChannel();
        if (systemChannel != null && canHostTriviaThreads(guild, systemChannel))
            return systemChannel;

        if (guild.getDefaultChannel() instanceof TextChannel defaultChannel && canHostTriviaThreads(guild, defaultChannel))
            return defaultChannel;

        return guild.getTextChannels().stream()
                .filter(channel -> canHostTriviaThreads(guild, channel))
                .findFirst()
                .orElse(null);
    }

    private static boolean canHostTriviaThreads(Guild guild, TextChannel channel) {
        return channel.canTalk() && guild.getSelfMember().hasPermission(channel,
                Permission.MESSAGE_HISTORY,
                Permission.CREATE_PUBLIC_THREADS,
                Permission.MESSAGE_SEND_IN_THREADS);
    }

    private static CompletableFuture<ThreadChannel> getOrCreateTriviaThread(TextChannel anchorChannel,
                                                                            String threadName) {
        Optional<ThreadChannel> activeThread = anchorChannel.getThreadChannels().stream()
                .filter(thread -> !thread.isArchived())
                .filter(thread -> thread.getName().equals(threadName))
                .findFirst();
        if (activeThread.isPresent()) {
            return CompletableFuture.completedFuture(activeThread.get());
        }

        AtomicReference<ThreadChannel> archivedThread = new AtomicReference<>();
        return anchorChannel.retrieveArchivedPublicThreadChannels()
                .forEachAsync(thread -> {
                    if (thread.getName().equals(threadName)) {
                        archivedThread.set(thread);
                        return false;
                    }
                    return true;
                })
                .thenCompose(ignored -> {
                    ThreadChannel existingThread = archivedThread.get();
                    if (existingThread != null)
                        return existingThread.getManager()
                                .setArchived(false)
                                .submit()
                                .thenApply(unarchived -> existingThread);

                    return anchorChannel.createThreadChannel(threadName).submit();
                });
    }

    private static CompletableFuture<Message> sendTriviaQuestion(ThreadChannel thread,
                                                                 SlashCommandInteractionEvent event,
                                                                 TriviaQuestion question) {
        String selectId = UUID.randomUUID().toString();
        Builder selectMenu = StringSelectMenu.create(selectId)
                .setPlaceholder("Select an answer...");

        List<String> answers = new ArrayList<>(question.incorrectAnswers);
        answers.add(question.correctAnswer);
        Collections.shuffle(answers);
        for (int index = 0; index < answers.size(); index++) {
            selectMenu.addOption(answers.get(index), Integer.toString(index));
        }

        return thread.sendMessage(event.getUser().getAsMention() + " Here's your trivia question!")
                .setEmbeds(new EmbedBuilder()
                        .setTitle(question.question())
                        .setDescription("Category: " + question.category() + "\nDifficulty: " + question.difficulty())
                        .setTimestamp(Instant.now())
                        .setColor(question.difficulty().equalsIgnoreCase("easy") ? 0x00FF00
                                : question.difficulty().equalsIgnoreCase("medium") ? 0xFFFF00 : 0xFF0000)
                        .build())
                .setComponents(ActionRow.of(selectMenu.build()))
                .submit()
                .thenApply(message -> {
                    CACHED_TRIVIA.add(new TriviaData(selectId, event.getGuild().getIdLong(),
                            thread.getIdLong(), message.getIdLong(), event.getUser().getIdLong(), question,
                            List.copyOf(answers)));
                    return message;
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

        if (data == null)
            return;

        event.deferEdit().queue();
        if (data.userId() != event.getUser().getIdLong()) {
            return;
        }

        int selectedAnswer;
        try {
            selectedAnswer = Integer.parseInt(event.getValues().getFirst());
        } catch (NumberFormatException exception) {
            Constants.LOGGER.warn("Received an invalid answer value for trivia question {}",
                    data.question().id(), exception);
            event.getHook().editOriginalComponents().queue();
            CACHED_TRIVIA.remove(data);
            return;
        }

        if (selectedAnswer < 0 || selectedAnswer >= data.answers().size()) {
            Constants.LOGGER.warn("Received out-of-range answer index {} for trivia question {}",
                    selectedAnswer, data.question().id());
            event.getHook().editOriginalComponents().queue();
            CACHED_TRIVIA.remove(data);
            return;
        }

        String response = data.answers().get(selectedAnswer);
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
