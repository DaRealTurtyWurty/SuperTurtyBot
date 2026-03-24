package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.weblisteners.social.NewsScraperUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractScrapedGameListener<N> {
    private static final int STORED_ARTICLE_LIMIT = 30;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    protected final void start(JDA jda) {
        if (hasStarted())
            return;

        this.initialized.set(true);
        this.executor.scheduleAtFixedRate(() -> poll(jda), 0, 5, TimeUnit.MINUTES);
    }

    protected final boolean hasStarted() {
        return this.initialized.get();
    }

    private void poll(JDA jda) {
        try {
            List<N> notifiers = notifierCollection().find().into(new ArrayList<>());
            if (notifiers.isEmpty())
                return;

            List<ScrapedArticle> articles = readRelevantArticles();
            if (articles.isEmpty())
                return;

            for (N notifier : notifiers) {
                try {
                    handleNotifier(jda, notifier, articles);
                } catch (Exception exception) {
                    Constants.LOGGER.error("Failed to process {} notifier for guild {}", sourceName(), guildId(notifier),
                            exception);
                }
            }
        } catch (Exception exception) {
            Constants.LOGGER.error("{} listener task failed", sourceName(), exception);
        }
    }

    private List<ScrapedArticle> readRelevantArticles() {
        try {
            Document document = NewsScraperUtils.fetchDocument(newsUrl(), listingReferer(), sourceName());
            if (document == null)
                return List.of();

            List<ScrapedArticle> articles = new ArrayList<>();
            Set<String> seenUrls = new HashSet<>();
            for (Element link : document.select(linkSelector())) {
                String url = resolveUrl(link);
                if (url.isBlank() || url.equals(newsUrl()) || !seenUrls.add(url))
                    continue;

                Element context = NewsScraperUtils.findContext(link);
                String listingTitle = resolveListingTitle(link, context);
                if (listingTitle.isBlank() || !matchesTitle(listingTitle))
                    continue;

                Document articleDocument = NewsScraperUtils.fetchDocument(url, newsUrl(), sourceName());
                String title = resolveArticleTitle(articleDocument, listingTitle);
                if (title.isBlank() || !matchesTitle(title))
                    continue;

                String description = resolveDescription(articleDocument, link, context, title, listingTitle);
                String imageUrl = resolveImageUrl(articleDocument, link, context, title, listingTitle);
                Instant publishedAt = resolvePublishedAt(articleDocument, link, context, title, listingTitle);

                String id = resolveArticleId(url, link, context, title, listingTitle);
                articles.add(new ScrapedArticle(id, title, url, imageUrl, publishedAt, description));
            }

            return articles;
        } catch (Exception exception) {
            Constants.LOGGER.error("Failed to scrape {} news page", sourceName(), exception);
            return List.of();
        }
    }

    private void handleNotifier(JDA jda, N notifier, List<ScrapedArticle> articles) {
        Guild guild = jda.getGuildById(guildId(notifier));
        if (guild == null) {
            notifierCollection().deleteMany(Filters.eq("guild", guildId(notifier)));
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId(notifier));
        if (channel == null) {
            notifierCollection().deleteMany(Filters.eq("channel", channelId(notifier)));
            return;
        }

        if (!channel.canTalk())
            return;

        List<String> storedArticleIds = storedArticleIds(notifier);
        if (storedArticleIds == null) {
            storedArticleIds = new ArrayList<>();
            setStoredArticleIds(notifier, storedArticleIds);
        }

        if (storedArticleIds.isEmpty()) {
            ScrapedArticle latestArticle = articles.getFirst();
            sendUpdate(channel, notifier, latestArticle);
            storedArticleIds.addAll(articles.stream().map(ScrapedArticle::id).limit(STORED_ARTICLE_LIMIT).toList());
            persistStoredArticles(notifier);
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            ScrapedArticle article = articles.get(index);
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

    private void persistStoredArticles(N notifier) {
        notifierCollection().updateOne(
                Filters.eq("guild", guildId(notifier)),
                Updates.set(storedArticleFieldName(), storedArticleIds(notifier)));
    }

    private void sendUpdate(TextChannel channel, N notifier, ScrapedArticle article) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(article.title(), article.url())
                .setDescription(NewsScraperUtils.truncate(article.description(), 4096, defaultDescription()))
                .setColor(embedColor())
                .setTimestamp(article.publishedAt())
                .setFooter(embedFooter());

        if (!article.imageUrl().isBlank()) {
            embed.setImage(article.imageUrl());
        }

        customizeEmbed(embed, article);

        channel.sendMessageEmbeds(embed.build())
                .setContent(mention(notifier))
                .queue();
    }

    protected String resolveUrl(Element link) {
        String url = link.absUrl("href").trim();
        if (!url.isBlank())
            return url;

        String href = link.attr("href").trim();
        if (href.startsWith("http://") || href.startsWith("https://"))
            return href;

        return href;
    }

    protected String resolveArticleTitle(Document articleDocument, String listingTitle) {
        String title = NewsScraperUtils.extractArticleTitle(articleDocument);
        return title.isBlank() ? listingTitle : title;
    }

    protected String resolveArticleId(String url, Element link, Element context, String title, String listingTitle) {
        return url;
    }

    protected String resolveDescription(Document articleDocument, Element link, Element context, String title,
                                        String listingTitle) {
        String description = NewsScraperUtils.extractPrimaryDescription(articleDocument);
        if (description.isBlank()) {
            description = NewsScraperUtils.extractListDescription(link.text(), title);
        }

        if (description.isBlank()) {
            description = NewsScraperUtils.extractContextDescription(context, title);
        }

        return description;
    }

    protected String resolveImageUrl(Document articleDocument, Element link, Element context, String title,
                                     String listingTitle) {
        String imageUrl = NewsScraperUtils.extractMetaImage(articleDocument);
        if (imageUrl.isBlank()) {
            imageUrl = NewsScraperUtils.extractImageUrl(context);
        }

        return imageUrl;
    }

    protected Instant resolvePublishedAt(Document articleDocument, Element link, Element context, String title,
                                         String listingTitle) {
        return NewsScraperUtils.extractPublishedAt(articleDocument, link.text());
    }

    protected void customizeEmbed(EmbedBuilder embed, ScrapedArticle article) {
        // NO-OP
    }

    protected abstract MongoCollection<N> notifierCollection();

    protected abstract long guildId(N notifier);

    protected abstract long channelId(N notifier);

    protected abstract String mention(N notifier);

    protected abstract List<String> storedArticleIds(N notifier);

    protected abstract void setStoredArticleIds(N notifier, List<String> storedArticleIds);

    protected abstract String storedArticleFieldName();

    protected abstract String newsUrl();

    protected abstract String listingReferer();

    protected abstract String sourceName();

    protected abstract String linkSelector();

    protected abstract String resolveListingTitle(Element link, Element context);

    protected abstract boolean matchesTitle(String title);

    protected abstract int embedColor();

    protected abstract String embedFooter();

    protected abstract String defaultDescription();

    protected record ScrapedArticle(String id, String title, String url, String imageUrl, Instant publishedAt,
                                    String description) {}
}
