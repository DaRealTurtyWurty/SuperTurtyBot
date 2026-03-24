package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.apptasticsoftware.rssreader.DateTime;
import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SteamUpcomingNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SteamUpcomingListener {
    private static final List<String> FEED_URLS = List.of(
            "https://rsshub.app/steam/search/filter=popularcomingsoon&os=win",
            "https://rsshub.rssforever.com/steam/search/filter=popularcomingsoon&os=win"
    );
    private static final String FEED_REFERER = "https://store.steampowered.com/explore/upcoming/";
    private static final int STORED_ARTICLE_LIMIT = 30;
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final RssReader READER = new RssReader();

    static {
        READER.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");
        READER.addHeader("Accept-Language", "en-US,en;q=0.9");
    }

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                List<SteamUpcomingNotifier> notifiers = Database.getDatabase().steamUpcomingNotifier.find()
                        .into(new ArrayList<>());
                if (notifiers.isEmpty())
                    return;

                List<SteamUpcomingArticle> articles = readArticles();
                if (articles.isEmpty())
                    return;

                for (SteamUpcomingNotifier notifier : notifiers) {
                    try {
                        handleNotifier(jda, notifier, articles);
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to process Steam upcoming notifier for guild {}",
                                notifier.getGuild(), exception);
                    }
                }
            } catch (Exception exception) {
                Constants.LOGGER.error("Steam upcoming listener task failed", exception);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static List<SteamUpcomingArticle> readArticles() {
        for (String feedUrl : FEED_URLS) {
            try {
                byte[] bytes = NewsScraperUtils.fetchBytes(feedUrl, FEED_REFERER, "Steam upcoming RSS feed");
                if (bytes == null)
                    continue;

                return READER.read(new java.io.ByteArrayInputStream(bytes))
                        .map(SteamUpcomingListener::toArticle)
                        .flatMap(Optional::stream)
                        .toList();
            } catch (IOException exception) {
                Constants.LOGGER.warn("Failed to read Steam upcoming RSS feed from {}", feedUrl, exception);
            }
        }

        Constants.LOGGER.error("Failed to read Steam upcoming RSS feed from all configured RSSHub instances.");
        return List.of();
    }

    private static Optional<SteamUpcomingArticle> toArticle(Item item) {
        String title = item.getTitle().orElse("").trim();
        String link = item.getLink().orElse("").trim();
        if (title.isBlank() || link.isBlank())
            return Optional.empty();

        String articleId = item.getGuid().filter(value -> !value.isBlank()).orElse(link);
        String contentHtml = item.getContent().or(() -> item.getDescription()).orElse("");
        Instant publishedAt = parseTimestamp(item);

        Document articleDocument = null;
        try {
            articleDocument = NewsScraperUtils.fetchDocument(link, FEED_REFERER, "Steam upcoming");
        } catch (IOException exception) {
            Constants.LOGGER.warn("Failed to fetch Steam upcoming article page {}", link, exception);
        }

        String description = resolveDescription(articleDocument, contentHtml);
        String imageUrl = resolveImageUrl(articleDocument, contentHtml);

        return Optional.of(new SteamUpcomingArticle(articleId, title, link, description, imageUrl, publishedAt));
    }

    private static Instant parseTimestamp(Item item) {
        if (item.getPubDateZonedDateTime().isPresent())
            return item.getPubDateZonedDateTime().get().toInstant();

        if (item.getUpdatedZonedDateTime().isPresent())
            return item.getUpdatedZonedDateTime().get().toInstant();

        String raw = item.getPubDate().or(() -> item.getUpdated()).orElse("");
        if (raw.isBlank())
            return Instant.now();

        try {
            return new DateTime().toInstant(raw);
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private static String resolveDescription(Document articleDocument, String contentHtml) {
        String description = NewsScraperUtils.extractPrimaryDescription(articleDocument);
        if (!description.isBlank())
            return description;

        Document contentDocument = Jsoup.parse(contentHtml);
        Element paragraph = contentDocument.selectFirst("p");
        if (paragraph != null) {
            String text = paragraph.text().trim();
            if (!text.isBlank())
                return text;
        }

        String text = contentDocument.text().trim();
        if (!text.isBlank())
            return text;

        return "New popular upcoming Steam release detected.";
    }

    private static String resolveImageUrl(Document articleDocument, String contentHtml) {
        String imageUrl = NewsScraperUtils.extractMetaImage(articleDocument);
        if (!imageUrl.isBlank())
            return imageUrl;

        return NewsScraperUtils.extractImageUrl(Jsoup.parse(contentHtml));
    }

    private static void handleNotifier(JDA jda, SteamUpcomingNotifier notifier, List<SteamUpcomingArticle> articles) {
        Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().steamUpcomingNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return;
        }

        TextChannel channel = guild.getTextChannelById(notifier.getChannel());
        if (channel == null) {
            Database.getDatabase().steamUpcomingNotifier.deleteMany(Filters.eq("channel", notifier.getChannel()));
            return;
        }

        if (!channel.canTalk())
            return;

        List<String> storedArticleIds = notifier.getStoredArticleIds();
        if (storedArticleIds == null) {
            storedArticleIds = new ArrayList<>();
            notifier.setStoredArticleIds(storedArticleIds);
        }

        if (storedArticleIds.isEmpty()) {
            SteamUpcomingArticle latestArticle = articles.getFirst();
            sendUpdate(channel, notifier, latestArticle);
            storedArticleIds.addAll(articles.stream().map(SteamUpcomingArticle::id).limit(STORED_ARTICLE_LIMIT).toList());
            persistStoredArticles(notifier);
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            SteamUpcomingArticle article = articles.get(index);
            if (storedArticleIds.contains(article.id()))
                continue;

            sendUpdate(channel, notifier, article);
            storedArticleIds.add(article.id());
            while (storedArticleIds.size() > STORED_ARTICLE_LIMIT) {
                storedArticleIds.removeFirst();
            }

            changed = true;
        }

        if (changed)
            persistStoredArticles(notifier);
    }

    private static void persistStoredArticles(SteamUpcomingNotifier notifier) {
        Database.getDatabase().steamUpcomingNotifier.updateOne(
                Filters.eq("guild", notifier.getGuild()),
                Updates.set("storedArticleIds", notifier.getStoredArticleIds()));
    }

    private static void sendUpdate(TextChannel channel, SteamUpcomingNotifier notifier, SteamUpcomingArticle article) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(article.title(), article.url())
                .setDescription(NewsScraperUtils.truncate(
                        article.description(), MessageEmbed.DESCRIPTION_MAX_LENGTH,
                        "New popular upcoming Steam release detected."))
                .setColor(0x66C0F4)
                .setTimestamp(article.publishedAt())
                .setFooter("Steam Upcoming");

        if (!article.imageUrl().isBlank())
            embed.setImage(article.imageUrl());

        channel.sendMessageEmbeds(embed.build())
                .setContent(notifier.getMention())
                .queue();
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private record SteamUpcomingArticle(String id, String title, String url, String description, String imageUrl,
                                        Instant publishedAt) {
    }
}
