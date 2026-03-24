package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.mongodb.client.MongoCollection;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
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
    protected String resolveUrl(Element link) {
        String url = link.absUrl("href").trim();
        if (!url.isBlank())
            return url;

        String href = link.attr("href").trim();
        if (href.startsWith("/"))
            return "https://www.ubisoft.com" + href;

        if (href.startsWith("http://") || href.startsWith("https://"))
            return href;

        return "";
    }

    @Override
    protected String resolveListingTitle(Element link, Element context) {
        String title = NewsScraperUtils.extractHeadingText(link);
        if (!title.isBlank())
            return title;

        title = NewsScraperUtils.extractHeadingText(context);
        if (!title.isBlank())
            return title;

        return cleanTitle(link.text());
    }

    @Override
    protected boolean matchesTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("patch notes") || normalized.contains("designer");
    }

    @Override
    protected Instant resolvePublishedAt(Document articleDocument, Element link, Element context, String title,
                                         String listingTitle) {
        String value = NewsScraperUtils.extractMetaPublishedTime(articleDocument);
        if (!value.isBlank()) {
            Instant parsed = NewsScraperUtils.parseFlexibleInstant(value);
            if (parsed != null)
                return parsed;
        }

        Instant listingDate = parseDate(extractDate(context));
        if (listingDate != null)
            return listingDate;

        return super.resolvePublishedAt(articleDocument, link, context, title, listingTitle);
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

    private String cleanTitle(String fullText) {
        if (fullText == null || fullText.isBlank())
            return "";

        String cleaned = fullText
                .replace("Read More Arrow RightBlack arrow pointing right", "")
                .replace("Read More", "")
                .trim();

        Matcher matcher = DATE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            cleaned = cleaned.substring(matcher.end()).trim();
        }

        int summaryIndex = cleaned.toLowerCase(Locale.ROOT).indexOf(" see ");
        if (summaryIndex > 0)
            cleaned = cleaned.substring(0, summaryIndex).trim();

        return cleaned;
    }

    private String extractDate(Element context) {
        if (context == null)
            return "";

        Matcher matcher = DATE_PATTERN.matcher(context.text());
        if (matcher.find())
            return matcher.group();

        return "";
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank())
            return null;

        try {
            return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
