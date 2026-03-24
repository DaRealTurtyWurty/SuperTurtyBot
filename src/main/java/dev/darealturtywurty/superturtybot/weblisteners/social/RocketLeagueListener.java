package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RocketLeagueNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RocketLeagueListener {
    private static final String NEWS_URL = "https://www.rocketleague.com/en/news";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";
    private static final int STORED_ARTICLE_LIMIT = 30;
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b[A-Z][a-z]{2} \\d{1,2}, \\d{4}\\b");

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                List<RocketLeagueNotifier> notifiers = Database.getDatabase().rocketLeagueNotifier.find()
                        .into(new ArrayList<>());
                if (notifiers.isEmpty())
                    return;

                List<RocketLeagueArticle> articles = readRelevantArticles();
                if (articles.isEmpty())
                    return;

                for (RocketLeagueNotifier notifier : notifiers) {
                    try {
                        handleNotifier(jda, notifier, articles);
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to process Rocket League notifier for guild {}",
                                notifier.getGuild(), exception);
                    }
                }
            } catch (Exception exception) {
                Constants.LOGGER.error("Rocket League listener task failed", exception);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static List<RocketLeagueArticle> readRelevantArticles() {
        try {
            Document document = fetchNewsDocument();
            if (document == null)
                return List.of();

            List<RocketLeagueArticle> articles = new ArrayList<>();
            Set<String> seenUrls = new HashSet<>();
            for (Element link : document.select("a[href*=/news/]")) {
                String url = link.absUrl("href").trim();
                if (url.isBlank())
                    continue;

                if (url.equals("https://www.rocketleague.com/news") || url.equals("https://www.rocketleague.com/en/news"))
                    continue;

                String title = extractTitle(link);
                if (title.isBlank() || !title.toLowerCase(Locale.ROOT).contains("patch notes"))
                    continue;

                if (!seenUrls.add(url))
                    continue;

                Element context = findContext(link);
                Document articleDocument = fetchDocument(url, NEWS_URL);
                String description = extractMetaDescription(articleDocument);
                if (description.isBlank()) {
                    description = extractDescription(context, title);
                }

                String imageUrl = extractMetaImage(articleDocument);
                if (imageUrl.isBlank()) {
                    imageUrl = extractImageUrl(context);
                }

                Instant publishedAt = extractPublishedAt(articleDocument, context);

                articles.add(new RocketLeagueArticle(url, title, url, imageUrl, publishedAt, description));
            }

            return articles;
        } catch (Exception exception) {
            Constants.LOGGER.error("Failed to scrape Rocket League news page", exception);
            return List.of();
        }
    }

    private static Document fetchNewsDocument() throws IOException {
        return fetchDocument(NEWS_URL, "https://www.rocketleague.com/");
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
                Constants.LOGGER.error("Failed to fetch Rocket League page {}. HTTP {}", url, response.code());
                return null;
            }

            ResponseBody body = response.body();
            if (body == null)
                return null;

            return Jsoup.parse(body.string(), url);
        }
    }

    private static Instant parseDate(String value) {
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

        return parseDate(extractDate(context));
    }

    private static void handleNotifier(JDA jda, RocketLeagueNotifier notifier, List<RocketLeagueArticle> articles) {
        Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().rocketLeagueNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return;
        }

        TextChannel channel = guild.getTextChannelById(notifier.getChannel());
        if (channel == null) {
            Database.getDatabase().rocketLeagueNotifier.deleteMany(Filters.eq("channel", notifier.getChannel()));
            return;
        }

        if (!channel.canTalk())
            return;

        List<String> storedArticleSlugs = notifier.getStoredArticleSlugs();
        if (storedArticleSlugs == null) {
            storedArticleSlugs = new ArrayList<>();
            notifier.setStoredArticleSlugs(storedArticleSlugs);
        }

        if (storedArticleSlugs.isEmpty()) {
            RocketLeagueArticle latestArticle = articles.getFirst();
            sendUpdate(channel, notifier, latestArticle);
            storedArticleSlugs.addAll(articles.stream()
                    .map(RocketLeagueArticle::id)
                    .limit(STORED_ARTICLE_LIMIT)
                    .toList());
            persistStoredArticles(notifier);
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            RocketLeagueArticle article = articles.get(index);
            if (storedArticleSlugs.contains(article.id()))
                continue;

            sendUpdate(channel, notifier, article);
            storedArticleSlugs.add(article.id());
            trimStoredArticles(storedArticleSlugs);
            changed = true;
        }

        if (changed)
            persistStoredArticles(notifier);
    }

    private static void persistStoredArticles(RocketLeagueNotifier notifier) {
        Database.getDatabase().rocketLeagueNotifier.updateOne(
                Filters.eq("guild", notifier.getGuild()),
                Updates.set("storedArticleSlugs", notifier.getStoredArticleSlugs()));
    }

    private static void trimStoredArticles(List<String> storedArticleSlugs) {
        while (storedArticleSlugs.size() > STORED_ARTICLE_LIMIT) {
            storedArticleSlugs.removeFirst();
        }
    }

    private static void sendUpdate(TextChannel channel, RocketLeagueNotifier notifier, RocketLeagueArticle article) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(article.title(), article.url())
                .setDescription(article.description().isBlank() ? "Latest Rocket League patch notes are live."
                        : truncate(article.description(), 4096))
                .setColor(0xF47B20)
                .setTimestamp(article.publishedAt())
                .setFooter("Rocket League");

        if (!article.imageUrl().isBlank()) {
            embed.setImage(article.imageUrl());
        }

        channel.sendMessageEmbeds(embed.build())
                .setContent(notifier.getMention())
                .queue();
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

        String text = link.text().trim();
        return text.replace("Read More", "").trim();
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
        } catch (DateTimeParseException ignored) {}

        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {}

        try {
            return java.time.ZonedDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {}

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

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank())
            return "";

        if (value.length() <= maxLength)
            return value;

        return value.substring(0, maxLength - 3) + "...";
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private record RocketLeagueArticle(String id, String title, String url, String imageUrl, Instant publishedAt,
                                       String description) {}
}
