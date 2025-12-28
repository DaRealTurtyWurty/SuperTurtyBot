package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.economy.promotion.youtube.*;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class YoutubePromotionMinigame implements PromotionMinigame {
    private static final Path VIDEO_DB_PATH = Path.of("video_ids.db");

    @Override
    public void start(SlashCommandInteractionEvent event, Economy account) {
        if (Environment.INSTANCE.youtubeApiKey().isEmpty()) {
            event.getHook().editOriginal("❌ Youtube API key is not set!").queue();
            return;
        }

        if (Files.notExists(VIDEO_DB_PATH)) {
            event.getHook().editOriginal("❌ Video ID database not found.").queue();
            return;
        }

        YoutubePromotionConfig config = YoutubePromotionConfig.fromEnvironment(Environment.INSTANCE);
        event.getHook().editOriginal("🔎 Finding videos for your promotion challenge... (this may take some time)").queue();
        CompletableFuture.supplyAsync(() -> loadVideoPair(config))
                .whenComplete((pair, throwable) -> {
                    if (throwable != null) {
                        Constants.LOGGER.error("Failed to start YouTube promotion minigame.", throwable);
                        event.getHook()
                                .editOriginal("❌ Could not start the promotion minigame right now. Please try again later.")
                                .queue();
                        return;
                    }

                    event.getHook()
                            .editOriginal("✅ You have started the promotion minigame! You have 20 seconds to answer.")
                            .flatMap(message -> message.createThreadChannel(event.getUser().getName() + "'s Promotion"))
                            .queue(channel -> {
                                channel.addThreadMember(event.getUser()).queue();

                                VideoPair displayPair = maybeSwap(pair);
                                int correctIndex = determineCorrectIndex(displayPair);
                                var upload = ThumbnailComposer.createComparisonImageUpload(displayPair.first(),
                                        displayPair.second());
                                channel.sendMessage(event.getUser().getAsMention()
                                                + " Which video has more views? Reply with `1` or `2`.")
                                        .addFiles(upload)
                                        .queue(message -> TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                                                .condition(e -> e.getChannel().getIdLong() == channel.getIdLong()
                                                        && e.getAuthor().getIdLong() == event.getUser().getIdLong()
                                                        && parseChoice(e.getMessage().getContentRaw()) != 0)
                                                .timeout(config.answerTimeoutSeconds(), TimeUnit.SECONDS)
                                                .timeoutAction(() -> {
                                                    channel.sendMessageFormat(
                                                                    "❌ You took too long to answer! The correct answer was %d.\n1) %,d views - %s\n2) %,d views - %s",
                                                                    correctIndex,
                                                                    displayPair.first().viewCount(),
                                                                    buildVideoUrl(displayPair.first()),
                                                                    displayPair.second().viewCount(),
                                                                    buildVideoUrl(displayPair.second()))
                                                            .queue(ignored -> channel.getManager().setArchived(true)
                                                                    .setLocked(true).queue());
                                                    account.setReadyForPromotion(false);
                                                    EconomyManager.updateAccount(account);
                                                })
                                                .success(messageEvent -> {
                                                    int choice = parseChoice(messageEvent.getMessage().getContentRaw());
                                                    if (choice == correctIndex) {
                                                        channel.sendMessageFormat(
                                                                        "✅ You have been promoted to level %d!\n1) %,d views - %s\n2) %,d views - %s",
                                                                        account.getJobLevel() + 1,
                                                                        displayPair.first().viewCount(),
                                                                        buildVideoUrl(displayPair.first()),
                                                                        displayPair.second().viewCount(),
                                                                        buildVideoUrl(displayPair.second()))
                                                                .queue(ignored -> channel.getManager().setArchived(true)
                                                                        .setLocked(true).queue());
                                                        account.setJobLevel(account.getJobLevel() + 1);
                                                    } else {
                                                        channel.sendMessageFormat(
                                                                        "❌ That is not correct! The correct answer was %d.\n1) %,d views - %s\n2) %,d views - %s",
                                                                        correctIndex,
                                                                        displayPair.first().viewCount(),
                                                                        buildVideoUrl(displayPair.first()),
                                                                        displayPair.second().viewCount(),
                                                                        buildVideoUrl(displayPair.second()))
                                                                .queue(ignored -> channel.getManager().setArchived(true)
                                                                        .setLocked(true).queue());
                                                    }

                                                    account.setReadyForPromotion(false);
                                                    EconomyManager.updateAccount(account);
                                                }).build());
                            });
                });
    }

    private VideoPair loadVideoPair(YoutubePromotionConfig config) {
        String apiKey = Environment.INSTANCE.youtubeApiKey()
                .orElseThrow(() -> new IllegalStateException("Youtube API key is not set!"));
        var apiClient = new YoutubeApiClient(apiKey, config.videosApiUrl());
        var repository = new VideoCacheRepository(VIDEO_DB_PATH);
        var selector = new VideoPairSelector(config.minVideoAgeMs(), config.filterShorts(),
                config.minViewDiff(), config.minViewDiffRatio());

        var candidates = repository.loadCandidates(apiClient, config.randomVideoCount(), config.maxFetchAttempts(),
                config.cacheTtlMs());
        VideoPair pair = selector.pickPair(candidates)
                .orElseThrow(() -> new IllegalStateException("Not enough videos available."));
        repository.removeFromCache(List.of(pair.first().id(), pair.second().id()));
        return pair;
    }

    private static int parseChoice(String input) {
        if (input == null)
            return 0;

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("1") || normalized.equals("a"))
            return 1;

        if (normalized.equals("2") || normalized.equals("b"))
            return 2;

        return 0;
    }

    private static String buildVideoUrl(YoutubeVideo video) {
        return "https://www.youtube.com/watch?v=" + video.id();
    }

    private static int determineCorrectIndex(VideoPair pair) {
        if (pair.first().viewCount() > pair.second().viewCount())
            return 1;

        if (pair.second().viewCount() > pair.first().viewCount())
            return 2;

        return pair.first().likeCount() >= pair.second().likeCount() ? 1 : 2;
    }

    private static VideoPair maybeSwap(VideoPair pair) {
        if (ThreadLocalRandom.current().nextBoolean())
            return new VideoPair(pair.second(), pair.first());

        return pair;
    }
}
