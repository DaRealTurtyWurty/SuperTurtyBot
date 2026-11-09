package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.google.gson.JsonObject;
import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.appnews.GetNewsForApp;
import com.lukaspradel.steamapi.data.json.appnews.Newsitem;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetNewsForAppRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SteamNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class SteamListener {
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static SteamWebApiClient CLIENT;
    private static final String APP_DETAILS_URL = "https://store.steampowered.com/api/appdetails?appids=%d";
    private static final String UPDATE_DESCRIPTION = "🟩 -> Added\n🟨 -> Modified\n🟥 -> Removed";

    static {
        Environment.INSTANCE.steamKey().ifPresentOrElse(
            key -> CLIENT = new SteamWebApiClient.SteamWebApiClientBuilder(key).build(),
            () -> Constants.LOGGER.error("Steam API Key has not been set!"));
    }

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void runExecutor(JDA jda) {
        if (IS_RUNNING.get())
            return;

        IS_RUNNING.set(true);

        if (CLIENT == null) {
            Database.getDatabase().steamNotifier.find().forEach(notifier -> {
                NotifierDiagnostics.checking(diagnosticKey(notifier));
                NotifierDiagnostics.checkFailed(diagnosticKey(notifier),
                    "The bot operator has not configured the Steam API key.");
            });
            return;
        }

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                final Map<Integer, List<SteamNotifier>> appGuildMap = new HashMap<>();
                Database.getDatabase().steamNotifier.find().forEach(notifier -> {
                    final int appId = notifier.getAppId();
                    List<SteamNotifier> notifiers;
                    if (!appGuildMap.containsKey(appId)) {
                        notifiers = new ArrayList<>();
                        appGuildMap.put(appId, notifiers);
                    } else {
                        notifiers = appGuildMap.get(appId);
                    }

                    notifiers.add(notifier);
                });

                for (final Entry<Integer, List<SteamNotifier>> entry : appGuildMap.entrySet()) {
                    final int appId = entry.getKey();

                    final List<SteamNotifier> notifiers = entry.getValue();
                    if (notifiers.isEmpty())
                        continue;

                    final GetNewsForAppRequest request = SteamWebApiRequestFactory.createGetNewsForAppRequest(appId, 1,
                        MessageEmbed.DESCRIPTION_MAX_LENGTH);
                    notifiers.forEach(notifier -> NotifierDiagnostics.checking(diagnosticKey(notifier)));
                    try {
                        final GetNewsForApp newses = CLIENT.processRequest(request);
                        if (newses == null || newses.getAppnews() == null)
                            throw new IllegalStateException("Steam returned no app news response");
                        notifiers.forEach(notifier -> NotifierDiagnostics.checked(diagnosticKey(notifier)));
                        if (newses.getAppnews() == null || newses.getAppnews().getNewsitems().isEmpty())
                            continue;

                        final Newsitem current = newses.getAppnews().getNewsitems().getFirst();
                        current.getAdditionalProperties().clear();
                        for (final SteamNotifier steamNotifier : notifiers) {
                            final Newsitem original = steamNotifier.getPreviousData();
                            if (current.equals(original))
                                continue;
                            if (original != null) {
                                final EmbedBuilder changes = getFormattedChanges(original, current);
                                if (!sendUpdate(jda, steamNotifier, original, current, changes))
                                    continue;
                            }
                            steamNotifier.setPreviousData(current);
                            Database.getDatabase().steamNotifier.updateOne(
                                Filters.and(Filters.eq("guild", steamNotifier.getGuild()),
                                    Filters.eq("channel", steamNotifier.getChannel()),
                                    Filters.eq("appId", appId)),
                                Updates.set("previousData", steamNotifier.getPreviousData()));
                        }
                    } catch (final SteamApiException exception) {
                        notifiers.forEach(notifier -> NotifierDiagnostics.checkFailed(diagnosticKey(notifier),
                            NotifierDiagnostics.failureReason(exception)));
                        Constants.LOGGER.error("Failed to get news for app {}", appId, exception);
                    } catch (final Exception exception) {
                        notifiers.forEach(notifier -> NotifierDiagnostics.checkFailed(diagnosticKey(notifier),
                            NotifierDiagnostics.failureReason(exception)));
                        Constants.LOGGER.error("Unexpected error processing app {}", appId, exception);
                    }
                }
            } catch (final Exception exception) {
                Constants.LOGGER.error("Steam listener task failed", exception);
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    private static NotifierDiagnostics.Key diagnosticKey(SteamNotifier notifier) {
        return NotifierDiagnostics.key("steam", notifier.getGuild(), Integer.toString(notifier.getAppId()),
            notifier.getChannel());
    }

    private static EmbedBuilder getFormattedChanges(Newsitem original, Newsitem current) {
        final var embed = new EmbedBuilder();
        record Change(String name, String previous, String updated) {
        }
        List<Change> changes = new ArrayList<>();

        class Utils {
            private void appendChange(String name, Function<Newsitem, String> str) {
                String previous = Objects.toString(str.apply(original), "");
                String updated = Objects.toString(str.apply(current), "");
                changes.add(new Change(name, previous, updated));
            }

            private void compareAndAdd(String name, Function<Newsitem, String> str) {
                if (compareData(str)) {
                    appendChange(name, str);
                }
            }

            private boolean compareData(Function<Newsitem, String> str) {
                return !Objects.equals(str.apply(original), str.apply(current));
            }

        }

        final var utils = new Utils();

        utils.compareAndAdd("Title", Newsitem::getTitle);
        utils.compareAndAdd("URL", Newsitem::getUrl);
        utils.compareAndAdd("Author", Newsitem::getAuthor);
        utils.compareAndAdd("Contents", Newsitem::getContents);
        utils.compareAndAdd("Has External URL", news -> Objects.toString(news.getIsExternalUrl(), ""));
        utils.compareAndAdd("Feed Label", Newsitem::getFeedlabel);
        utils.compareAndAdd("Feed Name", Newsitem::getFeedname);
        utils.compareAndAdd("Group ID", Newsitem::getGid);

        if (!changes.isEmpty()) {
            int available = MessageEmbed.EMBED_MAX_LENGTH_BOT - MessageEmbed.TITLE_MAX_LENGTH
                - UPDATE_DESCRIPTION.length() - changes.stream().mapToInt(change -> change.name().length()).sum();
            int fieldLimit = Math.min(MessageEmbed.VALUE_MAX_LENGTH, available / changes.size());
            for (Change change : changes) {
                embed.addField(change.name(), formatChangeValue(change.previous(), change.updated(), fieldLimit),
                    false);
            }
        }

        return embed;
    }

    private static String formatChangeValue(String previous, String updated, int maxLength) {
        String emoji = "🟨";
        if (previous.isBlank() && !updated.isBlank()) {
            emoji = "🟩";
        } else if (!previous.isBlank() && updated.isBlank()) {
            emoji = "🟥";
        }
        String prefix = emoji + " `";
        String suffix = "`\n";
        if (previous.isBlank())
            return prefix + truncatePreview(updated, maxLength - prefix.length() - suffix.length()) + suffix;

        String separator = "` -> `";
        int available = maxLength - prefix.length() - separator.length() - suffix.length();
        int previousLimit = Math.min(previous.length(), available / 2);
        int updatedLimit = Math.min(updated.length(), available - previousLimit);
        previousLimit = Math.min(previous.length(), available - updatedLimit);
        return prefix + truncatePreview(previous, previousLimit) + separator + truncatePreview(updated, updatedLimit)
            + suffix;
    }

    private static String truncatePreview(String value, int maxLength) {
        if (value.length() <= maxLength)
            return value;
        if (maxLength <= 0)
            return "";

        int end = maxLength - 1;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))
            && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end) + "…";
    }

    private static Optional<String> getGameBanner(int appId) {
        try {
            final URLConnection connection = new URI(APP_DETAILS_URL.formatted(appId)).toURL().openConnection();
            final JsonObject response = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            return Optional
                .ofNullable(response.has(Integer.toString(appId))
                    ? response.getAsJsonObject(Integer.toString(appId))
                        .getAsJsonObject("data").get("header_image").getAsString()
                    : null);
        } catch (final IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to get game banner!", exception);
            return Optional.empty();
        }
    }

    private static Optional<String> getGameName(int appId) {
        try {
            final URLConnection connection = new URI(APP_DETAILS_URL.formatted(appId)).toURL().openConnection();
            final JsonObject response = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            return Optional.ofNullable(response.has(Integer.toString(appId))
                ? response.getAsJsonObject(Integer.toString(appId)).getAsJsonObject("data").get("name").getAsString()
                : null);
        } catch (final IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to get game name!", exception);
            return Optional.empty();
        }
    }

    private static boolean sendUpdate(
        JDA jda,
        SteamNotifier notifier,
        Newsitem original,
        Newsitem current,
        EmbedBuilder changed
    ) {
        final Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().steamNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return false;
        }

        final StandardGuildMessageChannel channel = NotifierDeliverySupport.resolveChannel(guild,
            diagnosticKey(notifier));
        if (channel == null)
            return false;

        final String gameName = getGameName(notifier.getAppId()).filter(value -> !value.isBlank()).orElse("undefined");
        final String name = truncatePreview(gameName, MessageEmbed.TITLE_MAX_LENGTH);
        final String thumbnailURL = getGameBanner(notifier.getAppId()).orElse(null);
        String originalUrl = Objects.toString(original.getUrl(), "");
        changed.setTitle(name, originalUrl.isBlank() ? null : originalUrl);
        changed.setTimestamp(Instant.now());
        changed.setColor(Color.BLUE);
        changed.setThumbnail(thumbnailURL);
        changed.setDescription(UPDATE_DESCRIPTION);
        return NotifierDeliverySupport.sendAndWait(
            channel.sendMessage(notifier.getMention() + " Steam news update: **" + name + "**.")
                .addEmbeds(changed.build()),
            diagnosticKey(notifier),
            current.getGid());
    }
}
