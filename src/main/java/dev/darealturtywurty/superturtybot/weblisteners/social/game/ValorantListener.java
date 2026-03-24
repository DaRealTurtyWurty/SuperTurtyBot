package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.mongodb.client.MongoCollection;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ValorantNotifier;
import dev.darealturtywurty.superturtybot.weblisteners.social.NewsScraperUtils;
import net.dv8tion.jda.api.JDA;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ValorantListener extends AbstractScrapedGameListener<ValorantNotifier> {
    private static final String NEWS_URL = "https://playvalorant.com/en-us/news/game-updates/";
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?i)VALORANT\\s+Patch\\s+Notes\\s+[\\d.]+");
    private static final ValorantListener INSTANCE = new ValorantListener();

    private ValorantListener() {
    }

    public static void initialize(JDA jda) {
        INSTANCE.start(jda);
    }

    public static boolean isInitialized() {
        return INSTANCE.hasStarted();
    }

    @Override
    protected MongoCollection<ValorantNotifier> notifierCollection() {
        return Database.getDatabase().valorantNotifier;
    }

    @Override
    protected long guildId(ValorantNotifier notifier) {
        return notifier.getGuild();
    }

    @Override
    protected long channelId(ValorantNotifier notifier) {
        return notifier.getChannel();
    }

    @Override
    protected String mention(ValorantNotifier notifier) {
        return notifier.getMention();
    }

    @Override
    protected List<String> storedArticleIds(ValorantNotifier notifier) {
        return notifier.getStoredArticleUrls();
    }

    @Override
    protected void setStoredArticleIds(ValorantNotifier notifier, List<String> storedArticleIds) {
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
        return "https://playvalorant.com/";
    }

    @Override
    protected String sourceName() {
        return "VALORANT";
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
        return title.toLowerCase(Locale.ROOT).contains("patch notes");
    }

    @Override
    protected int embedColor() {
        return 0xFA4454;
    }

    @Override
    protected String embedFooter() {
        return "VALORANT";
    }

    @Override
    protected String defaultDescription() {
        return "Latest VALORANT patch notes are live.";
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
