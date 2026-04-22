package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SiegeNotifier;
import dev.darealturtywurty.superturtybot.weblisteners.social.NewsScraperUtils;
import net.dv8tion.jda.api.JDA;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SiegeListener extends AbstractScrapedGameListener<SiegeNotifier> {
    private static final String NEWS_URL = "https://www.ubisoft.com/en-us/game/rainbow-six/siege/news-updates";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.ENGLISH);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b");
    private static final SiegeListener INSTANCE = new SiegeListener();

    private SiegeListener() {
    }

    public static void initialize(JDA jda) {
        INSTANCE.start(jda);
    }

    public static boolean isInitialized() {
        return INSTANCE.hasStarted();
    }

    @Override
    protected MongoCollection<SiegeNotifier> notifierCollection() {
        return Database.getDatabase().siegeNotifier;
    }

    @Override
    protected long guildId(SiegeNotifier notifier) {
        return notifier.getGuild();
    }

    @Override
    protected long channelId(SiegeNotifier notifier) {
        return notifier.getChannel();
    }

    @Override
    protected String mention(SiegeNotifier notifier) {
        return notifier.getMention();
    }

    @Override
    protected List<String> storedArticleIds(SiegeNotifier notifier) {
        return notifier.getStoredArticleIds();
    }

    @Override
    protected void setStoredArticleIds(SiegeNotifier notifier, List<String> storedArticleIds) {
        notifier.setStoredArticleIds(storedArticleIds);
    }

    @Override
    protected String storedArticleFieldName() {
        return "storedArticleIds";
    }

    @Override
    protected String newsUrl() {
        return NEWS_URL;
    }

    @Override
    protected String listingReferer() {
        return "https://www.ubisoft.com/";
    }

    @Override
    protected String sourceName() {
        return "Siege";
    }

    @Override
    protected String linkSelector() {
        return "a[href*=/game/rainbow-six/siege/news-updates/]";
    }

    @Override
    protected List<ScrapedArticle> readRelevantArticles() {
        try {
            Document document = NewsScraperUtils.fetchDocument(newsUrl(), listingReferer(), sourceName());
            if (document == null)
                return List.of();

            String html = document.html();
            int startIndex = html.indexOf("window.__INITIAL_STATE__ = {");
            if (startIndex == -1) return List.of();
            
            startIndex += 27;
            int endIndex = html.indexOf("};</script>", startIndex);
            if (endIndex == -1) return List.of();
            
            String jsonString = html.substring(startIndex, endIndex + 1);
            JsonElement root = Constants.GSON.fromJson(jsonString, JsonElement.class);
            
            List<ScrapedArticle> articles = new ArrayList<>();
            collectSiegeArticles(root, articles);
            return articles;
        } catch (Exception exception) {
            Constants.LOGGER.error("Failed to scrape {} news page", sourceName(), exception);
            return List.of();
        }
    }

    private void collectSiegeArticles(JsonElement element, List<ScrapedArticle> articles) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectSiegeArticles(child, articles);
            }
            return;
        }
        if (!element.isJsonObject()) return;
        
        JsonObject object = element.getAsJsonObject();
        if (object.has("id") && object.has("title") && object.has("date")) {
            String title = object.get("title").getAsString();
            if (matchesTitle(title)) {
                String id = object.get("id").getAsString();
                String dateStr = object.get("date").getAsString();
                Instant publishedAt = parseDate(dateStr);
                if (publishedAt == null) publishedAt = Instant.now();
                
                String abstractText = object.has("abstract") ? object.get("abstract").getAsString() : defaultDescription();
                String url = newsUrl();
                if (object.has("button") && object.getAsJsonObject("button").has("buttonUrl")) {
                    url = "https://www.ubisoft.com/en-us/game/rainbow-six/siege/news-updates" + object.getAsJsonObject("button").get("buttonUrl").getAsString();
                }
                
                String imageUrl = "";
                if (object.has("thumbnail") && object.getAsJsonObject("thumbnail").has("url")) {
                    imageUrl = object.getAsJsonObject("thumbnail").get("url").getAsString();
                }
                
                articles.add(new ScrapedArticle(id, title, url, imageUrl, publishedAt, abstractText));
            }
        }
        
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectSiegeArticles(entry.getValue(), articles);
        }
    }

    @Override
    protected String resolveUrl(Element link) {
        return "";
    }

    @Override
    protected String resolveListingTitle(Element link, Element context) {
        return "";
    }

    @Override
    protected boolean matchesTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("patch notes") || normalized.contains("designer");
    }

    @Override
    protected int embedColor() {
        return 0xD98324;
    }

    @Override
    protected String embedFooter() {
        return "Ubisoft";
    }

    @Override
    protected String defaultDescription() {
        return "New Rainbow Six Siege patch or designer notes article detected.";
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank())
            return null;

        try {
            if (value.contains("GMT")) {
                int gmtIndex = value.indexOf("GMT");
                String toParse = value.substring(0, gmtIndex + 8).trim();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.ENGLISH);
                return java.time.ZonedDateTime.parse(toParse, dtf).toInstant();
            }
            return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }
}
