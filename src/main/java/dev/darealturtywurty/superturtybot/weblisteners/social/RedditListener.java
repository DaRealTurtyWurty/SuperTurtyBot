package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.mongodb.MongoWriteException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.apptasticsoftware.rssreader.DateTime;
import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RedditNotifier;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RedditPostCache;
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

public class RedditListener {
    private static final RssReader READER = new RssReader();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, Long> SUBREDDIT_BACKOFF_UNTIL = new HashMap<>();

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                List<RedditNotifier> notifiers = Database.getDatabase().redditNotifier.find().into(new ArrayList<>());
                if (notifiers.isEmpty())
                    return;

                Map<String, List<RedditNotifier>> notifiersBySubreddit = new HashMap<>();
                for (RedditNotifier redditNotifier : notifiers) {
                    String subreddit = redditNotifier.getSubreddit();
                    if (subreddit == null || subreddit.isBlank())
                        continue;
                    notifiersBySubreddit.computeIfAbsent(subreddit, key -> new ArrayList<>()).add(redditNotifier);
                }

                for (Map.Entry<String, List<RedditNotifier>> entry : notifiersBySubreddit.entrySet()) {
                    String subreddit = entry.getKey();
                    List<RedditNotifier> subredditNotifiers = entry.getValue();
                    pruneSubredditCache(subreddit);

                    long now = System.currentTimeMillis();
                    long backoffUntil = SUBREDDIT_BACKOFF_UNTIL.getOrDefault(subreddit, 0L);
                    if (now < backoffUntil)
                        continue;

                    Stream<Item> items;
                    try {
                        items = READER.read("https://www.reddit.com/r/" + subreddit + "/new/.rss");
                    } catch (final IOException exception) {
                        Integer status = extractHttpStatus(exception);
                        if (status != null && status >= 500 && status < 600) {
                            SUBREDDIT_BACKOFF_UNTIL.put(subreddit, now + TimeUnit.MINUTES.toMillis(5));
                            Constants.LOGGER.warn("Reddit RSS feed returned HTTP {} for r/{}. Backing off for 5 minutes.",
                                    status, subreddit);
                            continue;
                        }

                        Constants.LOGGER.error("Failed to read RSS feed for r/{}", subreddit, exception);
                        continue;
                    }

                    items.forEach(item -> {
                        try {
                            final String link = item.getLink().orElse("");
                            String mediaUrl = findMediaUrl(item.getDescription().orElse("")).orElse("");
                            String title = item.getTitle().orElse("");
                            String author = item.getAuthor().orElse("");
                            Instant time = new DateTime().toInstant(item.getPubDate().orElse(""));
                            String guid = item.getGuid().orElse("");
                            if (guid.isBlank())
                                guid = link;
                            if (guid.isBlank())
                                return;

                            if (!markAsSeen(subreddit, guid))
                                return;

                            if (mediaUrl.endsWith(".jpg") || mediaUrl.endsWith("jpeg") || mediaUrl.endsWith(".png")
                                    || mediaUrl.endsWith(".gif")) {
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
                                    if (channel == null || !channel.canTalk())
                                        continue;

                                    channel.sendMessageEmbeds(embed).setContent(redditNotifier.getMention()).queue();
                                }
                            } else {
                                for (RedditNotifier redditNotifier : subredditNotifiers) {
                                    Guild guild = jda.getGuildById(redditNotifier.getGuild());
                                    if (guild == null)
                                        continue;

                                    TextChannel channel = guild.getTextChannelById(redditNotifier.getChannel());
                                    if (channel == null || !channel.canTalk())
                                        continue;

                                    channel.sendMessage(
                                            String.format("%s [%s](<%s>) by %s at %s\n%s",
                                                    redditNotifier.getMention(), title, link, author, time, mediaUrl)
                                    ).queue();
                                }
                            }
                        } catch (final Exception exception) {
                            Constants.LOGGER.error("Failed to process Reddit item for r/{}", subreddit, exception);
                        }
                    });
                }
            } catch (final Exception exception) {
                Constants.LOGGER.error("Reddit listener task failed", exception);
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

    private static Integer extractHttpStatus(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                int idx = message.indexOf("HTTP status code:");
                if (idx >= 0) {
                    String tail = message.substring(idx + "HTTP status code:".length()).trim();
                    var digits = new StringBuilder();
                    for (int i = 0; i < tail.length(); i++) {
                        char c = tail.charAt(i);
                        if (Character.isDigit(c)) {
                            digits.append(c);
                        } else if (!digits.isEmpty())
                            break;
                    }

                    if (!digits.isEmpty()) {
                        try {
                            return Integer.parseInt(digits.toString());
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                }
            }

            current = current.getCause();
        }

        return null;
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private static boolean markAsSeen(String subreddit, String guid) {
        try {
            Database.getDatabase().redditPostCache.insertOne(new RedditPostCache(
                    subreddit,
                    guid,
                    System.currentTimeMillis()));
            return true;
        } catch (MongoWriteException exception) {
            if (exception.getError() != null && exception.getError().getCode() == 11000)
                return false;

            throw exception;
        }
    }

    private static void pruneSubredditCache(String subreddit) {
        List<RedditPostCache> cachedPosts = Database.getDatabase().redditPostCache.find(Filters.eq("subreddit", subreddit))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());
        if (cachedPosts.size() <= 30)
            return;

        List<String> staleGuids = cachedPosts.stream()
                .skip(30)
                .map(RedditPostCache::getGuid)
                .filter(guid -> guid != null && !guid.isBlank())
                .toList();
        if (staleGuids.isEmpty())
            return;

        Database.getDatabase().redditPostCache.deleteMany(Filters.and(
                Filters.eq("subreddit", subreddit),
                Filters.in("guid", staleGuids)));
    }
}
