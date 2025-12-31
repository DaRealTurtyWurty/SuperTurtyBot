package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import dev.darealturtywurty.superturtybot.modules.ArtistNsfwCache;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ArtistPromotionMinigame implements PromotionMinigame {
    private static final int ANSWER_TIMEOUT_SECONDS = 10;
    private static final Path DATASET_ROOT = Path.of("Real_AI_SD_LD_Dataset");
    private static final List<Path> DATASET_SPLITS = List.of(
            DATASET_ROOT.resolve("train"),
            DATASET_ROOT.resolve("test")
    );

    private static void endPromotion(Economy account) {
        account.setReadyForPromotion(false);
        EconomyManager.updateAccount(account);
    }

    private static void closeChannel(ThreadChannel channel) {
        channel.getManager().setArchived(true).setLocked(true).queue();
    }

    private static ArtistChallengeResult createChallenge(boolean useNsfwFilter) {
        boolean isAi = ThreadLocalRandom.current().nextBoolean();
        Optional<Path> image = useNsfwFilter
                ? ArtistNsfwCache.pickRandomSafeImage(isAi)
                : Optional.ofNullable(pickRandomImage(isAi));

        if (image.isEmpty()) {
            String error = useNsfwFilter
                    ? "❌ NSFW filter is enabled but no filtered images are available yet."
                    : "❌ Could not find artist images.";
            return new ArtistChallengeResult(null, error);
        }

        return new ArtistChallengeResult(new ArtistChallenge(isAi, image.get()), null);
    }

    private static Path pickRandomImage(boolean isAi) {
        try (Stream<Path> stream = DATASET_SPLITS.stream().flatMap(root -> {
            try {
                return Files.walk(root);
            } catch (IOException exception) {
                return Stream.empty();
            }
        })) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(ArtistPromotionMinigame::isSupportedImage)
                    .filter(path -> isAi == isAiPath(path))
                    .toList();
            if (files.isEmpty())
                return null;

            return files.get(ThreadLocalRandom.current().nextInt(files.size()));
        }
    }

    private static boolean isAiPath(Path path) {
        Path parent = path.getParent();
        if (parent == null)
            return false;

        return parent.getFileName().toString().startsWith("AI_");
    }

    private static boolean isSupportedImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
    }

    private static FileUpload buildUpload(Path imagePath, boolean spoiler) throws IOException {
        String extension = getExtension(imagePath.getFileName().toString());
        String randomName = generateRandomBase64Name(18) + extension;
        if (!spoiler)
            return FileUpload.fromData(imagePath.toFile(), randomName);

        String spoilerName = randomName.startsWith("SPOILER_") ? randomName : "SPOILER_" + randomName;
        return FileUpload.fromData(imagePath.toFile(), spoilerName);
    }

    private static String generateRandomBase64Name(int byteCount) {
        byte[] bytes = new byte[Math.max(1, byteCount)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index == -1 || index == fileName.length() - 1)
            return "";

        return fileName.substring(index);
    }

    private static Boolean parseAnswer(String input) {
        if (input == null)
            return null;

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("ai") || normalized.equals("a.i."))
            return true;

        if (normalized.equals("real") || normalized.equals("r"))
            return false;

        return null;
    }

    private static String formatAnswer(boolean isAi) {
        return isAi ? "AI" : "real";
    }

    private static boolean shouldUseNsfwFilter(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null)
            return false;

        GuildData config = GuildData.getOrCreateGuildData(event.getGuild());
        if (!config.isArtistNsfwFilterEnabled())
            return false;

        UserConfig userConfig = Database.getDatabase().userConfig
                .find(Filters.eq("user", event.getUser().getIdLong()))
                .first();
        return userConfig != null && userConfig.isArtistNsfwFilterOptIn();
    }

    @Override
    public void start(SlashCommandInteractionEvent event, Economy account) {
        if (DATASET_SPLITS.stream().anyMatch(Files::notExists)) {
            event.getHook().editOriginal("❌ Artist dataset not found.").queue();
            return;
        }

        boolean useNsfwFilter = shouldUseNsfwFilter(event);
        ArtistChallengeResult result = createChallenge(useNsfwFilter);
        if (result.errorMessage() != null) {
            event.getHook().editOriginal(result.errorMessage()).queue();
            return;
        }

        event.getHook()
                .editOriginal("✅ You have started the promotion minigame! You have 10 seconds to answer.")
                .flatMap(message -> message.createThreadChannel(event.getUser().getName() + "'s Promotion"))
                .queue(channel -> {
                    channel.addThreadMember(event.getUser()).queue();
                    sendChallenge(channel, event, account, result.challenge(), useNsfwFilter);
                });
    }

    private void sendChallenge(ThreadChannel channel, SlashCommandInteractionEvent event, Economy account,
                               ArtistChallenge challenge, boolean useNsfwFilter) {
        String prompt = event.getUser().getAsMention()
                + " Is this image AI or real? Reply with `ai` or `real`.";
        if (useNsfwFilter) {
            prompt += "\n⚠️ Potential NSFW warning. Image is spoilered.";
        }

        try (FileUpload upload = buildUpload(challenge.imagePath(), useNsfwFilter)) {
            channel.sendMessage(prompt)
                    .addFiles(upload)
                    .queue(message -> TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                            .condition(e -> e.getChannel().getIdLong() == channel.getIdLong()
                                    && e.getAuthor().getIdLong() == event.getUser().getIdLong()
                                    && parseAnswer(e.getMessage().getContentRaw()) != null)
                            .timeout(ANSWER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .timeoutAction(() -> handleTimeout(channel, account, challenge))
                            .success(messageEvent -> handleAnswer(channel, account,
                                    messageEvent.getMessage().getContentRaw(), challenge))
                            .build());
        } catch (IOException exception) {
            channel.sendMessage("❌ Could not load the artist challenge image.")
                    .queue(ignored -> closeChannel(channel));
            endPromotion(account);
        }
    }

    private void handleTimeout(ThreadChannel channel, Economy account, ArtistChallenge challenge) {
        channel.sendMessage("❌ You took too long to answer! The correct answer was "
                        + formatAnswer(challenge.isAi()) + ".")
                .queue(ignored -> closeChannel(channel));
        endPromotion(account);
    }

    private void handleAnswer(ThreadChannel channel, Economy account, String input, ArtistChallenge challenge) {
        Boolean answer = parseAnswer(input);
        if (answer == null) {
            channel.sendMessage("❌ Invalid answer. Reply with `ai` or `real`.")
                    .queue(ignored -> closeChannel(channel));
            endPromotion(account);
            return;
        }

        if (answer == challenge.isAi()) {
            channel.sendMessageFormat("✅ You have been promoted to level %d!",
                            account.getJobLevel() + 1)
                    .queue(ignored -> closeChannel(channel));
            account.setJobLevel(account.getJobLevel() + 1);
        } else {
            channel.sendMessage("❌ That is not correct! The correct answer was "
                            + formatAnswer(challenge.isAi()) + ".")
                    .queue(ignored -> closeChannel(channel));
        }

        endPromotion(account);
    }

    private record ArtistChallengeResult(ArtistChallenge challenge, String errorMessage) {
    }

    private record ArtistChallenge(boolean isAi, Path imagePath) {
    }
}
