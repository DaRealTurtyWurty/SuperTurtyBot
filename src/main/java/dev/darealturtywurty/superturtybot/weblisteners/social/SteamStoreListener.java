package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.apptasticsoftware.rssreader.DateTime;
import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SteamStoreNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SteamStoreListener {
    private static final String FEED_URL = "https://store.steampowered.com/feeds/news/app/593110";
    private static final String FEED_REFERER = "https://store.steampowered.com/news/";
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
                List<SteamStoreNotifier> notifiers = Database.getDatabase().steamStoreNotifier.find()
                        .into(new ArrayList<>());
                if (notifiers.isEmpty())
                    return;

                List<SteamStoreArticle> articles = readRelevantArticles();
                if (articles.isEmpty())
                    return;

                for (SteamStoreNotifier notifier : notifiers) {
                    try {
                        handleNotifier(jda, notifier, articles);
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to process Steam store notifier for guild {}",
                                notifier.getGuild(), exception);
                    }
                }
            } catch (Exception exception) {
                Constants.LOGGER.error("Steam store listener task failed", exception);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static List<SteamStoreArticle> readRelevantArticles() {
        try {
            byte[] bytes = NewsScraperUtils.fetchBytes(FEED_URL, FEED_REFERER, "Steam store RSS feed");
            if (bytes == null)
                return List.of();

            return READER.read(new ByteArrayInputStream(bytes))
                    .map(SteamStoreListener::toArticle)
                    .flatMap(Optional::stream)
                    .filter(article -> matchesTitle(article.title()))
                    .toList();
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read Steam store RSS feed", exception);
            return List.of();
        }
    }

    private static Optional<SteamStoreArticle> toArticle(Item item) {
        String title = item.getTitle().orElse("").trim();
        String link = item.getLink().orElse("").trim();
        if (title.isBlank() || link.isBlank())
            return Optional.empty();

        String articleId = normalizeArticleId(link);
        String legacyArticleId = item.getGuid()
                .filter(value -> !value.isBlank())
                .orElse(articleId);
        String contentHtml = item.getContent().or(item::getDescription).orElse("");
        Instant publishedAt = parseTimestamp(item);

        Document articleDocument = null;
        try {
            articleDocument = NewsScraperUtils.fetchDocument(link, FEED_REFERER, "Steam store");
        } catch (IOException exception) {
            Constants.LOGGER.warn("Failed to fetch Steam store article page {}", link, exception);
        }

        String description = resolveDescription(articleDocument, contentHtml);
        String imageUrl = resolveImageUrl(articleDocument, contentHtml);
        String youtubeUrl = extractYouTubeUrl(contentHtml, articleDocument).orElse("");

        return Optional.of(new SteamStoreArticle(articleId, legacyArticleId, title, link, description, imageUrl, publishedAt, youtubeUrl));
    }

    private static Instant parseTimestamp(Item item) {
        if (item.getPubDateZonedDateTime().isPresent())
            return item.getPubDateZonedDateTime().get().toInstant();

        if (item.getUpdatedZonedDateTime().isPresent())
            return item.getUpdatedZonedDateTime().get().toInstant();

        String raw = item.getPubDate().or(item::getUpdated).orElse("");
        if (raw.isBlank())
            return Instant.now();

        try {
            return new DateTime().toInstant(raw);
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private static boolean matchesTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("sale") || normalized.contains("fest");
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

        return "New Steam sale or fest announcement detected.";
    }

    private static String resolveImageUrl(Document articleDocument, String contentHtml) {
        String imageUrl = NewsScraperUtils.extractMetaImage(articleDocument);
        if (!imageUrl.isBlank())
            return imageUrl;

        return NewsScraperUtils.extractImageUrl(Jsoup.parse(contentHtml));
    }

    private static Optional<String> extractYouTubeUrl(String contentHtml, Document articleDocument) {
        String url = extractYouTubeUrl(Jsoup.parse(contentHtml));
        if (!url.isBlank())
            return Optional.of(url);

        if (articleDocument == null)
            return Optional.empty();

        url = extractYouTubeUrl(articleDocument);
        return url.isBlank() ? Optional.empty() : Optional.of(url);
    }

    private static String extractYouTubeUrl(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "iframe[src*='youtube.com/embed/']",
                "iframe[src*='youtube-nocookie.com/embed/']",
                "a[href*='youtube.com/watch']",
                "a[href*='youtu.be/']",
                "a[href*='youtube.com/shorts/']")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String raw = element.hasAttr("src") ? element.attr("src") : element.attr("href");
            String normalized = normalizeYouTubeUrl(raw);
            if (!normalized.isBlank())
                return normalized;
        }

        return "";
    }

    private static String normalizeYouTubeUrl(String url) {
        if (url == null || url.isBlank())
            return "";

        String normalized = url.trim().replace("&quot;", "");
        if (normalized.startsWith("//"))
            normalized = "https:" + normalized;

        try {
            URI uri = new URI(normalized);
            String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
            String path = Optional.ofNullable(uri.getPath()).orElse("");
            String query = Optional.ofNullable(uri.getQuery()).orElse("");

            if (host.contains("youtu.be")) {
                String videoId = trimSlashes(path);
                return videoId.isBlank() ? "" : "https://www.youtube.com/watch?v=" + videoId;
            }

            if (host.contains("youtube.com") || host.contains("youtube-nocookie.com")) {
                int embedIndex = path.indexOf("/embed/");
                if (embedIndex >= 0) {
                    String videoId = trimSlashes(path.substring(embedIndex + "/embed/".length()));
                    return videoId.isBlank() ? "" : "https://www.youtube.com/watch?v=" + videoId;
                }

                int shortsIndex = path.indexOf("/shorts/");
                if (shortsIndex >= 0) {
                    String videoId = trimSlashes(path.substring(shortsIndex + "/shorts/".length()));
                    return videoId.isBlank() ? "" : "https://www.youtube.com/watch?v=" + videoId;
                }

                if (path.equals("/watch") || path.equals("/watch/")) {
                    for (String parameter : query.split("&")) {
                        int separator = parameter.indexOf('=');
                        if (separator <= 0)
                            continue;

                        if (!parameter.substring(0, separator).equals("v"))
                            continue;

                        String videoId = URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
                        return videoId.isBlank() ? "" : "https://www.youtube.com/watch?v=" + videoId;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    private static String trimSlashes(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        int nextSlash = trimmed.indexOf('/');
        if (nextSlash >= 0)
            trimmed = trimmed.substring(0, nextSlash);

        return trimmed;
    }

    private static void handleNotifier(JDA jda, SteamStoreNotifier notifier, List<SteamStoreArticle> articles) {
        Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().steamStoreNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return;
        }

        StandardGuildMessageChannel channel = NotifierDeliverySupport.resolveChannel(guild, notifier.getChannel(),
                "Steam store");
        if (channel == null) {
            return;
        }

        List<String> storedArticleIds = notifier.getStoredArticleIds();
        if (storedArticleIds == null) {
            storedArticleIds = new ArrayList<>();
            notifier.setStoredArticleIds(storedArticleIds);
        }

        if (storedArticleIds.isEmpty()) {
            storedArticleIds.addAll(articles.stream().map(SteamStoreArticle::id).limit(STORED_ARTICLE_LIMIT).toList());
            persistStoredArticles(notifier);
            return;
        }

        if (isLegacyCacheMismatch(storedArticleIds, articles)) {
            storedArticleIds.clear();
            storedArticleIds.addAll(articles.stream().map(SteamStoreArticle::id).limit(STORED_ARTICLE_LIMIT).toList());
            persistStoredArticles(notifier);
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            SteamStoreArticle article = articles.get(index);
            if (matchesStoredArticleId(storedArticleIds, article))
                continue;

            if (!sendUpdate(channel, notifier, article))
                continue;

            storedArticleIds.add(article.id());
            while (storedArticleIds.size() > STORED_ARTICLE_LIMIT) {
                storedArticleIds.removeFirst();
            }

            changed = true;
        }

        if (changed)
            persistStoredArticles(notifier);
    }

    private static void persistStoredArticles(SteamStoreNotifier notifier) {
        Database.getDatabase().steamStoreNotifier.updateOne(
                Filters.and(
                        Filters.eq("guild", notifier.getGuild()),
                        Filters.eq("channel", notifier.getChannel())),
                Updates.set("storedArticleIds", notifier.getStoredArticleIds()));
    }

    private static boolean isLegacyCacheMismatch(List<String> storedArticleIds, List<SteamStoreArticle> articles) {
        if (storedArticleIds.isEmpty() || articles.isEmpty())
            return false;

        for (SteamStoreArticle article : articles) {
            if (matchesStoredArticleId(storedArticleIds, article))
                return false;
        }

        return true;
    }

    private static boolean matchesStoredArticleId(List<String> storedArticleIds, SteamStoreArticle article) {
        return storedArticleIds.contains(article.id()) || storedArticleIds.contains(article.legacyId());
    }

    private static String normalizeArticleId(String url) {
        return url == null ? "" : url.trim();
    }

    private static boolean sendUpdate(StandardGuildMessageChannel channel, SteamStoreNotifier notifier,
                                      SteamStoreArticle article) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(article.title(), article.url())
                .setDescription(NewsScraperUtils.truncate(
                        article.description(), MessageEmbed.DESCRIPTION_MAX_LENGTH,
                        "New Steam sale or fest announcement detected."))
                .setColor(0x1B4D6B)
                .setTimestamp(article.publishedAt())
                .setFooter("Steam");

        if (!article.imageUrl().isBlank())
            embed.setImage(article.imageUrl());

        if (!article.youtubeUrl().isBlank())
            embed.addField("Trailer", article.youtubeUrl(), false);

        String content = notifier.getMention() + " New Steam sale or fest: **" + article.title() + "**.";
        if (!article.youtubeUrl().isBlank())
            content += "\nTrailer: " + article.youtubeUrl();

        return NotifierDeliverySupport.sendAndWait(
                channel.sendMessageEmbeds(embed.build())
                        .setContent(content),
                "Steam store",
                channel);
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private record SteamStoreArticle(String id, String legacyId, String title, String url, String description, String imageUrl,
                                     Instant publishedAt, String youtubeUrl) {
    }
}
