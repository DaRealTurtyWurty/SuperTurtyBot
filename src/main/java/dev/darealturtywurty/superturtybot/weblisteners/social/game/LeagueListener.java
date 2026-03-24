package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.mongodb.client.MongoCollection;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.LeagueNotifier;
import dev.darealturtywurty.superturtybot.weblisteners.social.NewsScraperUtils;
import net.dv8tion.jda.api.JDA;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LeagueListener extends AbstractScrapedGameListener<LeagueNotifier> {
    private static final String NEWS_URL = "https://www.leagueoflegends.com/en-us/news/tags/patch-notes/";
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?i)(?:League of Legends\\s+)?Patch\\s+[\\d.]+\\s+Notes");
    private static final LeagueListener INSTANCE = new LeagueListener();

    private LeagueListener() {
    }

    public static void initialize(JDA jda) {
        INSTANCE.start(jda);
    }

    public static boolean isInitialized() {
        return INSTANCE.hasStarted();
    }

    @Override
    protected MongoCollection<LeagueNotifier> notifierCollection() {
        return Database.getDatabase().leagueNotifier;
    }

    @Override
    protected long guildId(LeagueNotifier notifier) {
        return notifier.getGuild();
    }

    @Override
    protected long channelId(LeagueNotifier notifier) {
        return notifier.getChannel();
    }

    @Override
    protected String mention(LeagueNotifier notifier) {
        return notifier.getMention();
    }

    @Override
    protected List<String> storedArticleIds(LeagueNotifier notifier) {
        return notifier.getStoredArticleUrls();
    }

    @Override
    protected void setStoredArticleIds(LeagueNotifier notifier, List<String> storedArticleIds) {
        notifier.setStoredArticleUrls(storedArticleIds);
    }

    @Override
    protected String storedArticleFieldName() {
        return "storedArticleUrls";
    }

    @Override
    protected String newsUrl() {
        return NEWS_URL;
    }

    @Override
    protected String listingReferer() {
        return "https://www.leagueoflegends.com/";
    }

    @Override
    protected String sourceName() {
        return "League";
    }

    @Override
    protected String linkSelector() {
        return "a[href*=/news/game-updates/]";
    }

    @Override
    protected String resolveListingTitle(Element link, Element context) {
        String title = NewsScraperUtils.extractHeadingText(link);
        if (!title.isBlank())
            return title;

        title = NewsScraperUtils.extractHeadingText(context);
        if (!title.isBlank())
            return title;

        return extractFallbackTitle(link.text());
    }

    @Override
    protected boolean matchesTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("patch") && normalized.contains("notes");
    }

    @Override
    protected int embedColor() {
        return 0xC89B3C;
    }

    @Override
    protected String embedFooter() {
        return "League of Legends";
    }

    @Override
    protected String defaultDescription() {
        return "Latest League of Legends patch notes are live.";
    }

    private String extractFallbackTitle(String text) {
        if (text == null || text.isBlank())
            return "";

        Matcher matcher = TITLE_PATTERN.matcher(text);
        if (matcher.find())
            return matcher.group().trim();

        return text.trim();
    }
}
