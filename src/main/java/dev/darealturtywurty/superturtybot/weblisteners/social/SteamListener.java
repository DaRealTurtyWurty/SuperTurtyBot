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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.time.Instant;
import java.util.List;
import java.util.*;
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

        if (CLIENT == null)
            return;

        EXECUTOR.scheduleAtFixedRate(() -> {
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
                if (notifiers.isEmpty()) {
                    continue;
                }

                final GetNewsForAppRequest request = SteamWebApiRequestFactory.createGetNewsForAppRequest(appId, 1,
                        MessageEmbed.DESCRIPTION_MAX_LENGTH);
                try {
                    final GetNewsForApp newses = CLIENT.processRequest(request);
                    final Newsitem current = newses.getAppnews().getNewsitems().getFirst();
                    current.getAdditionalProperties().clear();
                    for (final SteamNotifier steamNotifier : notifiers) {
                        final Newsitem original = steamNotifier.getPreviousData();
                        if (current.equals(original)) continue;
                        if (original != null) {
                            final EmbedBuilder changes = getFormattedChanges(original, current);
                            sendUpdate(jda, steamNotifier, original, current, changes);
                        }
                        steamNotifier.setPreviousData(current);
                        Database.getDatabase().steamNotifier.updateOne(
                                Filters.and(Filters.eq("guild", steamNotifier.getGuild()),
                                        Filters.eq("channel", steamNotifier.getChannel()), Filters.eq("appId", appId)),
                                Updates.set("previousData", steamNotifier.getPreviousData()));
                    }
                } catch (final SteamApiException exception) {
                    Constants.LOGGER.error("Failed to get news for app!", exception);
                }
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    private static EmbedBuilder getFormattedChanges(Newsitem original, Newsitem current) {
        final var embed = new EmbedBuilder();

        class Utils {
            void appendChange(String name, Function<Newsitem, String> str) {
                embed.addField(name,
                    getEmojiForChange(str.apply(original), str.apply(current))
                        + (!str.apply(original).isBlank() ? " `" + str.apply(original) + "` ->" : "") + " `"
                        + str.apply(current) + "`\n",
                    false);
            }

            void compareAndAdd(String name, Function<Newsitem, String> str) {
                if (compareData(str)) {
                    appendChange(name, str);
                }
            }

            boolean compareData(Function<Newsitem, String> str) {
                return !str.apply(original).equals(str.apply(current));
            }

            String getEmojiForChange(String old, String current) {
                if (old.isBlank() && !current.isBlank())
                    return "🟩";

                if (current.isBlank() && !old.isBlank())
                    return "🟥";

                return "🟨";
            }
        }

        final var utils = new Utils();

        utils.compareAndAdd("Title", Newsitem::getTitle);
        utils.compareAndAdd("URL", Newsitem::getUrl);
        utils.compareAndAdd("Author", Newsitem::getAuthor);
        utils.compareAndAdd("Contents", Newsitem::getContents);
        utils.compareAndAdd("Has External URL", news -> Boolean.toString(news.getIsExternalUrl()));
        utils.compareAndAdd("Feed Label", Newsitem::getFeedlabel);
        utils.compareAndAdd("Feed Name", Newsitem::getFeedname);
        utils.compareAndAdd("Group ID", Newsitem::getGid);

        return embed;
    }
    
    private static Optional<String> getGameBanner(int appId) {
        try {
            final URLConnection connection = new URI(APP_DETAILS_URL.formatted(appId)).toURL().openConnection();
            final JsonObject response = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            return Optional
                .ofNullable(response.has(Integer.toString(appId)) ? response.getAsJsonObject(Integer.toString(appId))
                    .getAsJsonObject("data").get("header_image").getAsString() : null);
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

    private static void sendUpdate(JDA jda, SteamNotifier notifier, Newsitem original, Newsitem current,
        EmbedBuilder changed) {
        final Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().steamNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return;
        }

        final TextChannel channel = guild.getTextChannelById(notifier.getChannel());
        if (channel == null) {
            Database.getDatabase().steamNotifier.deleteMany(Filters.eq("channel", notifier.getGuild()));
            return;
        }
        
        final String name = getGameName(notifier.getAppId()).orElse("undefined");
        final String thumbnailURL = getGameBanner(notifier.getAppId()).orElse(null);
        changed.setTitle(name, original.getUrl().isBlank() ? null : original.getUrl());
        changed.setTimestamp(Instant.now());
        changed.setColor(Color.BLUE);
        changed.setThumbnail(thumbnailURL);
        changed.setDescription("🟩 -> Added\n🟨 -> Modified\n🟥 -> Removed");
        channel.sendMessage(notifier.getMention() + " **" + name + "** has just been updated on **Steam**!")
            .addEmbeds(changed.build()).queue();
    }
}
