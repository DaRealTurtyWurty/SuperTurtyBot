package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SiegeNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiegeListener {
    private static final String NEWS_URL = "https://www.ubisoft.com/en-us/game/rainbow-six/siege/news-updates";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";
    private static final int STORED_ARTICLE_LIMIT = 30;
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b");

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                List<SiegeNotifier> notifiers = Database.getDatabase().siegeNotifier.find().into(new ArrayList<>());
                if (notifiers.isEmpty())
                    return;

                List<SiegeArticle> articles = readRelevantArticles();
                if (articles.isEmpty())
                    return;

                for (SiegeNotifier notifier : notifiers) {
                    try {
                        handleNotifier(jda, notifier, articles);
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to process Siege notifier for guild {}", notifier.getGuild(),
                                exception);
                    }
                }
            } catch (Exception exception) {
                Constants.LOGGER.error("Siege listener task failed", exception);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static List<SiegeArticle> readRelevantArticles() {
        try {
            Document document = fetchNewsDocument();
            if (document == null)
                return List.of();

            List<SiegeArticle> articles = new ArrayList<>();
            Set<String> seenUrls = new HashSet<>();
            for (Element link : document.select("a[href*=/game/rainbow-six/siege/news-updates/]")) {
                String title = extractTitle(link);
                if (title.isBlank() || !matchesTitle(title))
                    continue;

                String url = link.absUrl("href").trim();
                if (url.isBlank()) {
                    String href = link.attr("href").trim();
                    if (href.startsWith("/")) {
                        url = "https://www.ubisoft.com" + href;
                    } else if (href.startsWith("http://") || href.startsWith("https://")) {
                        url = href;
                    }
                }

                if (url.isBlank() || !seenUrls.add(url))
                    continue;

                Element context = findContext(link);
                Document articleDocument = fetchDocument(url, NEWS_URL);
                String description = extractMetaDescription(articleDocument);
                if (description.isBlank()) {
                    description = extractDescription(context, title);
                }

                String thumbnailUrl = extractMetaImage(articleDocument);
                if (thumbnailUrl.isBlank()) {
                    thumbnailUrl = extractImageUrl(context);
                }

                Instant publishedAt = extractPublishedAt(articleDocument, context);
                String articleType = determineArticleType(title);

                articles.add(new SiegeArticle(url, title, description, url, thumbnailUrl, publishedAt, articleType,
                        0, articleType));
            }

            return articles;
        } catch (Exception exception) {
            Constants.LOGGER.error("Failed to scrape Siege news page", exception);
            return List.of();
        }
    }

    private static Document fetchNewsDocument() throws IOException {
        return fetchDocument(NEWS_URL, "https://www.ubisoft.com/");
    }

    private static Document fetchDocument(String url, String referer) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Referer", referer)
                .header("Upgrade-Insecure-Requests", "1")
                .build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.error("Failed to fetch Siege page {}. HTTP {}", url, response.code());
                return null;
            }

            ResponseBody body = response.body();
            if (body == null)
                return null;

            return Jsoup.parse(body.string(), url);
        }
    }

    private static Instant parseTimestamp(String value) {
        if (value == null || value.isBlank())
            return Instant.now();

        try {
            return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException exception) {
            return Instant.now();
        }
    }

    private static Instant extractPublishedAt(Document articleDocument, Element context) {
        String value = extractMetaPublishedTime(articleDocument);
        if (!value.isBlank()) {
            Instant parsed = parseFlexibleInstant(value);
            if (parsed != null)
                return parsed;
        }

        return parseTimestamp(extractDate(context));
    }

    private static boolean matchesTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("patch notes") || normalized.contains("designer");
    }

    private static void handleNotifier(JDA jda, SiegeNotifier notifier, List<SiegeArticle> articles) {
        Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().siegeNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return;
        }

        TextChannel channel = guild.getTextChannelById(notifier.getChannel());
        if (channel == null) {
            Database.getDatabase().siegeNotifier.deleteMany(Filters.eq("channel", notifier.getChannel()));
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
            SiegeArticle latestArticle = articles.getFirst();
            sendUpdate(channel, notifier, latestArticle);
            storedArticleIds.addAll(articles.stream().map(SiegeArticle::id).limit(STORED_ARTICLE_LIMIT).toList());
            persistStoredArticles(notifier);
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            SiegeArticle article = articles.get(index);
            if (storedArticleIds.contains(article.id()))
                continue;

            sendUpdate(channel, notifier, article);
            storedArticleIds.add(article.id());
            trimStoredArticles(storedArticleIds);
            changed = true;
        }

        if (changed)
            persistStoredArticles(notifier);
    }

    private static void persistStoredArticles(SiegeNotifier notifier) {
        Database.getDatabase().siegeNotifier.updateOne(
                Filters.eq("guild", notifier.getGuild()),
                Updates.set("storedArticleIds", notifier.getStoredArticleIds()));
    }

    private static void trimStoredArticles(List<String> storedArticleIds) {
        while (storedArticleIds.size() > STORED_ARTICLE_LIMIT) {
            storedArticleIds.removeFirst();
        }
    }

    private static void sendUpdate(TextChannel channel, SiegeNotifier notifier, SiegeArticle article) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(article.title(),
                        article.url().isBlank() ? null : article.url())
                .setDescription(truncate(article.description(), MessageEmbed.DESCRIPTION_MAX_LENGTH))
                .setColor(0xD98324)
                .setTimestamp(article.publishedAt())
                .setFooter("Ubisoft");

        if (article.readTime() > 0) {
            embed.addField("Read Time", article.readTime() + " min", true);
        }

        if (!article.thumbnailUrl().isBlank()) {
            embed.setImage(article.thumbnailUrl());
        }

        channel.sendMessageEmbeds(embed.build())
                .setContent(notifier.getMention())
                .queue();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank())
            return "New Rainbow Six Siege patch or designer notes article detected.";

        if (value.length() <= maxLength)
            return value;

        return value.substring(0, maxLength - 3) + "...";
    }

    private static String extractTitle(Element link) {
        Element heading = link.selectFirst("h1, h2, h3, h4, h5, h6");
        if (heading != null) {
            String text = heading.text().trim();
            if (!text.isBlank())
                return text;
        }

        Element context = findContext(link);
        if (context != null) {
            heading = context.selectFirst("h1, h2, h3, h4, h5, h6");
            if (heading != null) {
                String text = heading.text().trim();
                if (!text.isBlank())
                    return text;
            }
        }

        String fullText = link.text().trim();
        if (fullText.isBlank())
            return "";

        return cleanTitle(fullText);
    }

    private static @NotNull String cleanTitle(String fullText) {
        String cleaned = fullText
                .replace("Read More Arrow RightBlack arrow pointing right", "")
                .replace("Read More", "")
                .trim();

        Matcher matcher = DATE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            cleaned = cleaned.substring(matcher.end()).trim();
        }

        int summaryIndex = cleaned.toLowerCase(Locale.ROOT).indexOf(" see ");
        if (summaryIndex > 0) {
            cleaned = cleaned.substring(0, summaryIndex).trim();
        }
        return cleaned;
    }

    private static Element findContext(Element link) {
        Element current = link;
        for (int depth = 0; depth < 6 && current != null; depth++) {
            if (!extractDate(current).isBlank() || !extractImageUrl(current).isBlank() || hasHeading(current)) {
                return current;
            }

            current = current.parent();
        }

        return link.parent();
    }

    private static String extractDescription(Element context, String title) {
        if (context == null)
            return "";

        for (Element candidate : context.select("p, div, span")) {
            String text = candidate.text().trim();
            if (text.isBlank() || text.equals(title) || text.equalsIgnoreCase("Read More"))
                continue;

            if (DATE_PATTERN.matcher(text).matches())
                continue;

            if (text.contains(title))
                continue;

            return text;
        }

        return "";
    }

    private static String extractImageUrl(Element context) {
        if (context == null)
            return "";

        Element image = context.selectFirst("img[src], img[data-src], img[data-lazy-src], source[srcset]");
        if (image == null)
            return "";

        String srcSet = image.absUrl("srcset");
        if (!srcSet.isBlank())
            return firstUrlFromSrcSet(srcSet);

        srcSet = image.attr("srcset");
        if (!srcSet.isBlank())
            return firstUrlFromSrcSet(srcSet);

        String url = image.absUrl("src");
        if (!url.isBlank())
            return url;

        url = image.absUrl("data-src");
        if (!url.isBlank())
            return url;

        url = image.absUrl("data-lazy-src");
        if (!url.isBlank())
            return url;

        String raw = image.attr("src");
        if (raw.startsWith("//"))
            return "https:" + raw;

        raw = image.attr("data-lazy-src");
        if (raw.startsWith("//"))
            return "https:" + raw;

        if (!raw.isBlank())
            return raw;

        raw = image.attr("srcset");
        if (!raw.isBlank())
            return firstUrlFromSrcSet(raw);

        return raw;
    }

    private static String extractMetaDescription(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "meta[property=og:description]",
                "meta[name=description]",
                "meta[name=twitter:description]")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String content = element.attr("content").trim();
            if (!content.isBlank())
                return content;
        }

        return "";
    }

    private static String extractMetaImage(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "meta[property=og:image]",
                "meta[name=twitter:image]",
                "meta[name=twitter:image:src]",
                "link[rel=image_src]")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String url = element.hasAttr("content") ? element.absUrl("content") : element.absUrl("href");
            if (!url.isBlank())
                return url;

            String raw = element.hasAttr("content") ? element.attr("content").trim() : element.attr("href").trim();
            if (raw.startsWith("//"))
                return "https:" + raw;

            if (!raw.isBlank())
                return raw;
        }

        return "";
    }

    private static String extractMetaPublishedTime(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "meta[property=article:published_time]",
                "meta[name=article:published_time]",
                "meta[property=og:published_time]",
                "meta[name=publish-date]",
                "time[datetime]")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String value = element.hasAttr("content") ? element.attr("content").trim() : element.attr("datetime").trim();
            if (!value.isBlank())
                return value;
        }

        return "";
    }

    private static Instant parseFlexibleInstant(String value) {
        if (value == null || value.isBlank())
            return null;

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return java.time.ZonedDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private static String firstUrlFromSrcSet(String srcSet) {
        if (srcSet == null || srcSet.isBlank())
            return "";

        String candidate = srcSet.split(",")[0].trim();
        int spaceIndex = candidate.indexOf(' ');
        if (spaceIndex > 0) {
            candidate = candidate.substring(0, spaceIndex).trim();
        }

        if (candidate.startsWith("//"))
            return "https:" + candidate;

        return candidate;
    }

    private static String extractDate(Element context) {
        if (context == null)
            return "";

        Matcher matcher = DATE_PATTERN.matcher(context.text());
        if (matcher.find())
            return matcher.group();

        return "";
    }

    private static boolean hasHeading(Element element) {
        return element != null && element.selectFirst("h1, h2, h3, h4, h5, h6") != null;
    }

    private static String determineArticleType(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        if (normalized.contains("designer"))
            return "Designer Notes";

        if (normalized.contains("patch notes"))
            return "Patch Notes";

        return "Update";
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private record SiegeArticle(String id, String title, String description, String url, String thumbnailUrl,
                                Instant publishedAt, String articleType, int readTime, String categories) {
    }
}
