package io.github.darealturtywurty.superturtybot.weblisteners.social;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.appnews.GetNewsForApp;
import com.lukaspradel.steamapi.data.json.appnews.Newsitem;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetNewsForAppRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.SteamAppNews;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.SteamNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class SteamListener {
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final SteamWebApiClient CLIENT = new SteamWebApiClient.SteamWebApiClientBuilder(
        Environment.INSTANCE.steamKey()).build();

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }
    
    public static void runExecutor(JDA jda) {
        if (IS_RUNNING.get())
            return;
        
        IS_RUNNING.set(true);
        
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
                
                final GetNewsForAppRequest request = SteamWebApiRequestFactory.createGetNewsForAppRequest(appId);
                try {
                    final GetNewsForApp newses = CLIENT.processRequest(request);
                    final Newsitem news = newses.getAppnews().getNewsitems().get(0);
                    final SteamAppNews current = constructPojo(news);
                    for (final SteamNotifier steamNotifier : notifiers) {
                        final SteamAppNews original = constructPojo(steamNotifier.getPreviousData());
                        if (isNew(original, current)) {
                            final EmbedBuilder changes = getFormattedChanges(original, current);
                            sendUpdate(jda, steamNotifier, original, changes);
                            steamNotifier.setPreviousData(news);
                            Database.getDatabase().steamNotifier.updateOne(
                                Filters.and(Filters.eq("guild", steamNotifier.getGuild()),
                                    Filters.eq("channel", steamNotifier.getChannel()), Filters.eq("appId", appId)),
                                Updates.set("previousData", steamNotifier.getPreviousData()));
                        }
                    }
                } catch (final SteamApiException exception) {
                    exception.printStackTrace();
                }
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    private static SteamAppNews constructPojo(Newsitem news) {
        return new SteamAppNews(news.getAuthor(), news.getContents(), news.getDate(), news.getFeedlabel(),
            news.getFeedname(), news.getGid(), news.getIsExternalUrl(), news.getTitle(), news.getUrl(),
            news.getAdditionalProperties());
    }

    private static EmbedBuilder getFormattedChanges(SteamAppNews original, SteamAppNews current) {
        final var embed = new EmbedBuilder();
        
        class Utils {
            void appendChange(String name, Function<SteamAppNews, String> str) {
                embed.addField(name, getEmojiForChange(str.apply(original), str.apply(current)) + " `"
                    + str.apply(original) + "` -> `" + str.apply(current) + "`\n", false);
            }
            
            void compareAndAdd(String name, Function<SteamAppNews, String> str) {
                if (compareData(str)) {
                    appendChange(name, str);
                }
            }
            
            boolean compareData(Function<SteamAppNews, String> str) {
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
        
        utils.compareAndAdd("Title", SteamAppNews::getTitle);
        utils.compareAndAdd("URL", SteamAppNews::getUrl);
        utils.compareAndAdd("Author", SteamAppNews::getAuthor);
        utils.compareAndAdd("Contents", SteamAppNews::getContents);
        utils.compareAndAdd("Has External URL", news -> Boolean.toString(news.isExternalUrl()));
        utils.compareAndAdd("Feed Label", SteamAppNews::getFeedlabel);
        utils.compareAndAdd("Feed Name", SteamAppNews::getFeedname);
        utils.compareAndAdd("Group ID", SteamAppNews::getGid);
        
        return embed;
    }

    private static boolean isNew(SteamAppNews original, SteamAppNews current) {
        return !original.getAuthor().equals(current.getAuthor())
            || !original.getContents().equals(current.getContents()) || original.getDate() != current.getDate()
            || !original.getFeedlabel().equals(current.getFeedlabel())
            || !original.getFeedname().equals(current.getFeedname()) || !original.getGid().equals(current.getGid())
            || original.isExternalUrl() != current.isExternalUrl() || !original.getTitle().equals(current.getTitle())
            || !original.getUrl().equals(current.getUrl())
            || !original.getAdditionalProperties().equals(current.getAdditionalProperties());
    }

    private static void sendUpdate(JDA jda, SteamNotifier notifier, SteamAppNews news, EmbedBuilder changed) {
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

        changed.setTitle(news.getTitle(), news.getUrl());
        changed.setTimestamp(Instant.now());
        changed.setColor(Color.BLUE);
        changed.setDescription("🟩 -> Added\n🟨 -> Modified\n🟥 -> Removed");
        channel.sendMessage(notifier.getMention() + " **" + news.getTitle() + "** has just been updated on **Steam**!")
            .addEmbeds(changed.build()).queue();
    }
}
