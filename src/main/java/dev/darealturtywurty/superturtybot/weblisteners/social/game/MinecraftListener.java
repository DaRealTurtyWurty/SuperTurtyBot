package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MinecraftNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.darealturtywurty.superturtybot.weblisteners.social.NotifierDeliverySupport;

public class MinecraftListener {
    private static final String FEED_URL = "https://net-secondary.web.minecraft-services.net/api/v1.0/en-us/search?page=1&pageSize=24&sortType=Recent&category=News&newsOnly=true&geography=GB&filter%5Bsubscription%5D=Minecraft%3A+Java";
    private static final String MINECRAFT_BASE_URL = "https://www.minecraft.net";
    private static final int STORED_ARTICLE_LIMIT = 30;
    private static final List<String> VERSION_CONTEXT_KEYWORDS = List.of(
            "snapshot",
            "pre-release",
            "prerelease",
            "release candidate",
            "release",
            "update",
            "available now",
            "now available",
            "out now",
            "out today",
            "drops today",
            "launches today");
    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("\\b\\d{2}w\\d{2}[a-z]\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_WITH_SUFFIX_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+){1,3}(?:\\s+(?:pre-release|release candidate|rc)\\s+\\d+)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+){1,3}\\b");
    private static final Pattern EPOCH_PATTERN = Pattern.compile("^\\d{10,17}$");
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static void initialize(JDA jda) {
        if (isInitialized())
            return;

        IS_INITIALIZED.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                List<MinecraftNotifier> notifiers = Database.getDatabase().minecraftNotifier.find()
                        .into(new ArrayList<>());
                if (notifiers.isEmpty())
                    return;

                List<MinecraftArticle> articles = readRelevantArticles();
                if (articles.isEmpty())
                    return;

                for (MinecraftNotifier notifier : notifiers) {
                    try {
                        handleNotifier(jda, notifier, articles);
                    } catch (Exception exception) {
                        Constants.LOGGER.error("Failed to process Minecraft notifier for guild {}",
                                notifier.getGuild(), exception);
                    }
                }
            } catch (Exception exception) {
                Constants.LOGGER.error("Minecraft listener task failed", exception);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private static List<MinecraftArticle> readRelevantArticles() {
        Request request = new Request.Builder()
                .url(FEED_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "SuperTurtyBot/1.0")
                .build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.error("Failed to read Minecraft news feed! Status code: {}", response.code());
                return List.of();
            }

            ResponseBody body = response.body();
            if (body == null || body.contentLength() == 0)
                return List.of();

            JsonElement root = Constants.GSON.fromJson(body.charStream(), JsonElement.class);
            return parseArticles(root);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read Minecraft news feed", exception);
            return List.of();
        }
    }

    private static List<MinecraftArticle> parseArticles(JsonElement root) {
        Map<String, MinecraftArticle> articles = new LinkedHashMap<>();
        collectArticles(root, articles);
        return articles.values().stream()
                .filter(MinecraftListener::isVersionArticle)
                .sorted(Comparator.comparing(MinecraftArticle::publishedAt).reversed())
                .toList();
    }

    private static void collectArticles(JsonElement element, Map<String, MinecraftArticle> articles) {
        if (element == null || element.isJsonNull())
            return;

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectArticles(child, articles);
            }

            return;
        }

        if (!element.isJsonObject())
            return;

        JsonObject object = element.getAsJsonObject();
        toArticle(object).ifPresent(article -> articles.putIfAbsent(article.id(), article));
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectArticles(entry.getValue(), articles);
        }
    }

    private static Optional<MinecraftArticle> toArticle(JsonObject object) {
        String title = findString(object, "title", "headline", "name").orElse("").trim();
        if (title.isBlank())
            return Optional.empty();

        String link = findString(object, "url", "link", "articleUrl", "canonicalUrl", "defaultUrl", "pagePath", "path")
                .map(MinecraftListener::normalizeLink)
                .orElse("");
        Instant publishedAt = findInstant(object, "publishDate", "publishedAt", "published", "date",
                "updated", "lastUpdated", "startDate", "createdAt")
                .orElse(Instant.EPOCH);

        if (link.isBlank() && publishedAt.equals(Instant.EPOCH))
            return Optional.empty();

        String guid = findString(object, "id", "guid", "articleId", "contentId", "slug", "key")
                .filter(value -> !value.isBlank())
                .orElse(link.isBlank() ? title : link);
        String description = findString(object, "description", "summary", "excerpt", "shortDescription", "body", "text")
                .map(value -> Jsoup.parse(value).text().trim())
                .orElse("");
        String author = findString(object, "author", "creator", "byline")
                .filter(value -> !value.isBlank())
                .orElse("Minecraft");

        return Optional.of(new MinecraftArticle(guid, title, link, description, author, publishedAt));
    }

    private static Instant parseTimestamp(String value) {
        if (value == null || value.isBlank())
            return Instant.EPOCH;

        String trimmed = value.trim();
        if (EPOCH_PATTERN.matcher(trimmed).matches()) {
            try {
                long epoch = Long.parseLong(trimmed);
                return trimmed.length() <= 10 ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
            } catch (NumberFormatException ignored) {
                // Fall through to the string-based parsers below.
            }
        }

        try {
            return Instant.parse(trimmed);
        } catch (Exception exception) {
            try {
                return OffsetDateTime.parse(trimmed).toInstant();
            } catch (Exception ignored) {
                try {
                    return ZonedDateTime.parse(trimmed).toInstant();
                } catch (Exception ignoredAgain) {
                    try {
                        return ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                    } catch (Exception ignoredYetAgain) {
                        return Instant.EPOCH;
                    }
                }
            }
        }
    }

    private static boolean isVersionArticle(MinecraftArticle article) {
        if (containsSnapshot(article.title()) || containsSnapshot(article.description()))
            return true;

        if (VERSION_WITH_SUFFIX_PATTERN.matcher(article.title()).find())
            return true;

        boolean titleHasVersion = VERSION_PATTERN.matcher(article.title()).find();
        boolean descriptionHasVersion = VERSION_PATTERN.matcher(article.description()).find();
        if (!titleHasVersion && !descriptionHasVersion)
            return false;

        String normalizedTitle = article.title().toLowerCase(Locale.ROOT);
        String normalizedDescription = article.description().toLowerCase(Locale.ROOT);
        return containsAny(normalizedTitle, VERSION_CONTEXT_KEYWORDS)
                || containsAny(normalizedDescription, VERSION_CONTEXT_KEYWORDS);
    }

    private static void handleNotifier(JDA jda, MinecraftNotifier notifier, List<MinecraftArticle> articles) {
        Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            return;
        }

        StandardGuildMessageChannel channel = NotifierDeliverySupport.resolveChannel(guild, notifier.getChannel(),
                "Minecraft");
        if (channel == null) {
            return;
        }

        List<String> storedArticles = notifier.getStoredArticles();
        if (storedArticles == null) {
            storedArticles = new ArrayList<>();
            notifier.setStoredArticles(storedArticles);
        }

        if (storedArticles.isEmpty()) {
            var failedRecentArticles = new HashSet<String>();
            if (notifier.getCreatedAt() > 0L) {
                for (int index = articles.size() - 1; index >= 0; index--) {
                    MinecraftArticle article = articles.get(index);
                    if (article.publishedAt().toEpochMilli() < notifier.getCreatedAt())
                        continue;

                    if (!sendUpdate(channel, notifier, article)) {
                        failedRecentArticles.add(article.id());
                    }
                }
            }

            storedArticles.addAll(articles.stream()
                    .filter(article -> !failedRecentArticles.contains(article.id()))
                    .map(MinecraftArticle::id)
                    .limit(STORED_ARTICLE_LIMIT)
                    .toList());
            if (!storedArticles.isEmpty()) {
                persistStoredArticles(notifier);
            }
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            MinecraftArticle article = articles.get(index);
            if (storedArticles.contains(article.id()))
                continue;

            if (!sendUpdate(channel, notifier, article))
                continue;

            storedArticles.add(article.id());
            trimStoredArticles(storedArticles);
            changed = true;
        }

        if (changed)
            persistStoredArticles(notifier);
    }

    private static void persistStoredArticles(MinecraftNotifier notifier) {
        Database.getDatabase().minecraftNotifier.updateOne(
                Filters.and(Filters.eq("guild", notifier.getGuild()), Filters.eq("channel", notifier.getChannel())),
                Updates.set("storedArticles", notifier.getStoredArticles()));
    }

    private static void trimStoredArticles(List<String> storedArticles) {
        while (storedArticles.size() > STORED_ARTICLE_LIMIT) {
            storedArticles.remove(0);
        }
    }

    private static boolean sendUpdate(StandardGuildMessageChannel channel, MinecraftNotifier notifier,
                                      MinecraftArticle article) {
        String versionLabel = extractVersionLabel(article);
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("New Minecraft Version: " + versionLabel, article.link().isBlank() ? null : article.link())
                .setDescription(truncate(article.description(), MessageEmbed.DESCRIPTION_MAX_LENGTH))
                .setAuthor(article.author())
                .setColor(0x6CC349)
                .setTimestamp(article.publishedAt())
                .setFooter("Minecraft.net");

        return NotifierDeliverySupport.sendAndWait(
                channel.sendMessageEmbeds(embed.build())
                        .setContent(notifier.getMention()),
                "Minecraft",
                channel);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank())
            return "New Minecraft update article detected.";

        if (value.length() <= maxLength)
            return value;

        return value.substring(0, maxLength - 3) + "...";
    }

    private static Optional<String> findString(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = getFieldIgnoreCase(object, key);
            if (element == null)
                continue;

            Optional<String> value = extractString(element);
            if (value.isPresent())
                return value;
        }

        return Optional.empty();
    }

    private static Optional<Instant> findInstant(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = getFieldIgnoreCase(object, key);
            if (element == null)
                continue;

            Optional<Instant> instant = extractInstant(element);
            if (instant.isPresent())
                return instant;
        }

        return Optional.empty();
    }

    private static JsonElement getFieldIgnoreCase(JsonObject object, String key) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key))
                return entry.getValue();
        }

        return null;
    }

    private static Optional<String> extractString(JsonElement element) {
        if (element == null || element.isJsonNull())
            return Optional.empty();

        if (element.isJsonPrimitive()) {
            String value = element.getAsString().trim();
            return value.isBlank() ? Optional.empty() : Optional.of(value);
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                Optional<String> value = extractString(child);
                if (value.isPresent())
                    return value;
            }

            return Optional.empty();
        }

        JsonObject object = element.getAsJsonObject();
        for (String preferredKey : List.of("default", "value", "text", "content", "name", "label", "title",
                "url", "href", "path")) {
            JsonElement preferred = getFieldIgnoreCase(object, preferredKey);
            if (preferred == null)
                continue;

            Optional<String> value = extractString(preferred);
            if (value.isPresent())
                return value;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Optional<String> value = extractString(entry.getValue());
            if (value.isPresent())
                return value;
        }

        return Optional.empty();
    }

    private static Optional<Instant> extractInstant(JsonElement element) {
        Optional<String> value = extractString(element);
        if (value.isEmpty())
            return Optional.empty();

        Instant instant = parseTimestamp(value.get());
        return instant.equals(Instant.EPOCH) ? Optional.empty() : Optional.of(instant);
    }

    private static String normalizeLink(String value) {
        String link = value.trim();
        if (link.isBlank())
            return "";

        if (link.startsWith("//"))
            return "https:" + link;

        if (link.startsWith("/"))
            return MINECRAFT_BASE_URL + link;

        if (link.startsWith("http://") || link.startsWith("https://"))
            return link;

        if (link.startsWith("minecraft.net/") || link.startsWith("www.minecraft.net/"))
            return "https://" + link;

        return "";
    }

    private static boolean containsSnapshot(String value) {
        return value != null && SNAPSHOT_PATTERN.matcher(value).find();
    }

    private static boolean containsAny(String text, List<String> values) {
        for (String value : values) {
            if (text.contains(value))
                return true;
        }

        return false;
    }

    private static String extractVersionLabel(MinecraftArticle article) {
        Matcher snapshotMatcher = SNAPSHOT_PATTERN.matcher(article.title());
        if (snapshotMatcher.find())
            return snapshotMatcher.group();

        snapshotMatcher = SNAPSHOT_PATTERN.matcher(article.description());
        if (snapshotMatcher.find())
            return snapshotMatcher.group();

        Matcher versionWithSuffixMatcher = VERSION_WITH_SUFFIX_PATTERN.matcher(article.title());
        if (versionWithSuffixMatcher.find())
            return versionWithSuffixMatcher.group();

        versionWithSuffixMatcher = VERSION_WITH_SUFFIX_PATTERN.matcher(article.description());
        if (versionWithSuffixMatcher.find())
            return versionWithSuffixMatcher.group();

        Matcher versionMatcher = VERSION_PATTERN.matcher(article.title());
        if (versionMatcher.find())
            return versionMatcher.group();

        versionMatcher = VERSION_PATTERN.matcher(article.description());
        if (versionMatcher.find())
            return versionMatcher.group();

        return article.title();
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private record MinecraftArticle(String id, String title, String link, String description, String author,
                                    Instant publishedAt) {}
}
