package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.apptasticsoftware.rssreader.DateTime;
import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RedditNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

// TODO: Move cache into the database
public class RedditListener {
    private static final RssReader READER = new RssReader();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, List<String>> SUBREDDIT_CACHE_MAP = new HashMap<>();

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                List<RedditNotifier> notifiers = Database.getDatabase().redditNotifier.find().into(new ArrayList<>());

                Map<String, List<RedditNotifier>> notifiersBySubreddit = new HashMap<>();
                for (RedditNotifier redditNotifier : notifiers) {
                    String subreddit = redditNotifier.getSubreddit();
                    notifiersBySubreddit.computeIfAbsent(subreddit, key -> new ArrayList<>()).add(redditNotifier);
                }

                for (Map.Entry<String, List<RedditNotifier>> entry : notifiersBySubreddit.entrySet()) {
                    String subreddit = entry.getKey();
                    List<RedditNotifier> subredditNotifiers = entry.getValue();
                    Stream<Item> items = READER.read("https://www.reddit.com/r/" + subreddit + "/new/.rss");

                    items.forEach(item -> {
                        final String link = item.getLink().orElse("");
                        String mediaUrl = findMediaUrl(item.getDescription().orElse("")).orElse("");
                        String title = item.getTitle().orElse("");
                        String author = item.getAuthor().orElse("");
                        Instant time = new DateTime().toInstant(item.getPubDate().orElse(""));
                        String guid = item.getGuid().orElse("");

                        if (SUBREDDIT_CACHE_MAP.computeIfAbsent(subreddit, key -> new ArrayList<>()).contains(guid))
                            return;

                        if (mediaUrl.endsWith(".jpg") || mediaUrl.endsWith("jpeg") || mediaUrl.endsWith(".png") || mediaUrl.endsWith(".gif")) {
                            var embed = new EmbedBuilder()
                                    .setTitle(title, link)
                                    .setAuthor(author)
                                    .setTimestamp(time)
                                    .setColor(0xFF4500)
                                    .setImage(mediaUrl)
                                    .build();

                            for (RedditNotifier redditNotifier : subredditNotifiers) {
                                Guild guild = jda.getGuildById(redditNotifier.getGuild());
                                if (guild == null)
                                    continue;

                                TextChannel channel = guild.getTextChannelById(redditNotifier.getChannel());
                                if (channel == null)
                                    continue;

                                channel.sendMessageEmbeds(embed).setContent(redditNotifier.getMention()).queue();
                            }
                        } else {
                            for (RedditNotifier redditNotifier : subredditNotifiers) {
                                Guild guild = jda.getGuildById(redditNotifier.getGuild());
                                if (guild == null)
                                    continue;

                                TextChannel channel = guild.getTextChannelById(redditNotifier.getChannel());
                                if (channel == null)
                                    continue;

                                channel.sendMessage(
                                        String.format("%s [%s](<%s>) by %s at %s\n%s",
                                                redditNotifier.getMention(), title, link, author, time, mediaUrl)
                                ).queue();
                            }
                        }

                        SUBREDDIT_CACHE_MAP.computeIfAbsent(subreddit, key -> new ArrayList<>()).add(guid);

                        if (SUBREDDIT_CACHE_MAP.get(subreddit).size() > 30)
                            SUBREDDIT_CACHE_MAP.get(subreddit).removeFirst();
                    });
                }
            } catch (final IOException exception) {
                Constants.LOGGER.error("Failed to read RSS feed!", exception);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private static Optional<String> findMediaUrl(String html) {
        if (html.isBlank())
            return Optional.empty();

        Document document = Jsoup.parse(html);
        Element anchor = document.select("td:eq(1) span a").first();
        if (anchor == null)
            return Optional.empty();

        String link = anchor.attr("href");
        if (link.isBlank())
            return Optional.empty();

        return Optional.of(link);
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }
}
