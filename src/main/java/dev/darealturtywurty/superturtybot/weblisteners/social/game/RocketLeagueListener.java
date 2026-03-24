package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.mongodb.client.MongoCollection;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RocketLeagueNotifier;
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

public final class RocketLeagueListener extends AbstractScrapedGameListener<RocketLeagueNotifier> {
    private static final String NEWS_URL = "https://www.rocketleague.com/en/news";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b[A-Z][a-z]{2} \\d{1,2}, \\d{4}\\b");
    private static final RocketLeagueListener INSTANCE = new RocketLeagueListener();

    private RocketLeagueListener() {
    }

    public static void initialize(JDA jda) {
        INSTANCE.start(jda);
    }

    public static boolean isInitialized() {
        return INSTANCE.hasStarted();
    }

    @Override
    protected MongoCollection<RocketLeagueNotifier> notifierCollection() {
        return Database.getDatabase().rocketLeagueNotifier;
    }

    @Override
    protected long guildId(RocketLeagueNotifier notifier) {
        return notifier.getGuild();
    }

    @Override
    protected long channelId(RocketLeagueNotifier notifier) {
        return notifier.getChannel();
    }

    @Override
    protected String mention(RocketLeagueNotifier notifier) {
        return notifier.getMention();
    }

    @Override
    protected List<String> storedArticleIds(RocketLeagueNotifier notifier) {
        return notifier.getStoredArticleSlugs();
    }

    @Override
    protected void setStoredArticleIds(RocketLeagueNotifier notifier, List<String> storedArticleIds) {
        notifier.setStoredArticleSlugs(storedArticleIds);
    }

    @Override
    protected String storedArticleFieldName() {
        return "storedArticleSlugs";
    }

    @Override
    protected String newsUrl() {
        return NEWS_URL;
    }

    @Override
    protected String listingReferer() {
        return "https://www.rocketleague.com/";
    }

    @Override
    protected String sourceName() {
        return "Rocket League";
    }

    @Override
    protected String linkSelector() {
        return "a[href*=/news/]";
    }

    @Override
    protected String resolveUrl(Element link) {
        String url = super.resolveUrl(link).trim();
        if (url.equals("https://www.rocketleague.com/news"))
            return NEWS_URL;

        return url;
    }

    @Override
    protected String resolveArticleId(String url, Element link, Element context, String title, String listingTitle) {
        int index = url.lastIndexOf('/');
        if (index < 0 || index == url.length() - 1)
            return url;

        return url.substring(index + 1);
    }

    @Override
    protected String resolveListingTitle(Element link, Element context) {
        String title = NewsScraperUtils.extractHeadingText(link);
        if (!title.isBlank())
            return title;

        title = NewsScraperUtils.extractHeadingText(context);
        if (!title.isBlank())
            return title;

        return link.text().replace("Read More", "").trim();
    }

    @Override
    protected boolean matchesTitle(String title) {
        return title.toLowerCase(Locale.ROOT).contains("patch notes");
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
        return 0xF47B20;
    }

    @Override
    protected String embedFooter() {
        return "Rocket League";
    }

    @Override
    protected String defaultDescription() {
        return "Latest Rocket League patch notes are live.";
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
