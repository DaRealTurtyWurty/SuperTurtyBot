package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.apptasticsoftware.rssreader.DateTime;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MinecraftNotifier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MinecraftListener {
    private static final String FEED_URL = "https://www.minecraft.net/en-us/feeds/community-content/rss";
    private static final int STORED_ARTICLE_LIMIT = 30;
    private static final List<String> TITLE_KEYWORDS = List.of("release", "snapshot");
    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("\\b\\d{2}w\\d{2}[a-z]\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_WITH_SUFFIX_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+){1,3}(?:\\s+(?:pre-release|release candidate)\\s+\\d+)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+){1,3}\\b");
    private static final DocumentBuilderFactory DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    static {
        DOCUMENT_FACTORY.setNamespaceAware(true);
    }

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
        try (InputStream stream = new URL(FEED_URL).openStream()) {
            DocumentBuilder documentBuilder = DOCUMENT_FACTORY.newDocumentBuilder();
            Document document = documentBuilder.parse(stream);
            return parseArticles(document);
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            Constants.LOGGER.error("Failed to read Minecraft RSS feed", exception);
            return List.of();
        }
    }

    private static List<MinecraftArticle> parseArticles(Document document) {
        List<MinecraftArticle> articles = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            String localName = getLocalName(node);
            if (!"item".equals(localName) && !"entry".equals(localName))
                continue;

            toArticle(node)
                    .filter(article -> matchesTitle(article.title()))
                    .ifPresent(articles::add);
        }

        return articles;
    }

    private static Optional<MinecraftArticle> toArticle(Node node) {
        String title = getChildText(node, "title").orElse("").trim();
        if (title.isBlank())
            return Optional.empty();

        String link = getLink(node).orElse("").trim();
        String guid = getChildText(node, "guid", "id")
                .filter(value -> !value.isBlank())
                .orElse(link.isBlank() ? title : link);
        String description = getChildText(node, "description", "summary", "content")
                .map(value -> Jsoup.parse(value).text().trim())
                .orElse("");
        String author = getChildText(node, "author", "creator")
                .filter(value -> !value.isBlank())
                .orElse("Minecraft");
        Instant publishedAt = parseTimestamp(getChildText(node, "pubDate", "published", "updated").orElse(""));

        return Optional.of(new MinecraftArticle(guid, title, link, description, author, publishedAt));
    }

    private static Instant parseTimestamp(String value) {
        if (value == null || value.isBlank())
            return Instant.now();

        try {
            return new DateTime().toInstant(value);
        } catch (Exception exception) {
            return Instant.now();
        }
    }

    private static boolean matchesTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        for (String keyword : TITLE_KEYWORDS) {
            if (normalized.contains(keyword))
                return true;
        }

        return false;
    }

    private static void handleNotifier(JDA jda, MinecraftNotifier notifier, List<MinecraftArticle> articles) {
        Guild guild = jda.getGuildById(notifier.getGuild());
        if (guild == null) {
            Database.getDatabase().minecraftNotifier.deleteMany(Filters.eq("guild", notifier.getGuild()));
            return;
        }

        TextChannel channel = guild.getTextChannelById(notifier.getChannel());
        if (channel == null) {
            Database.getDatabase().minecraftNotifier.deleteMany(Filters.eq("channel", notifier.getChannel()));
            return;
        }

        if (!channel.canTalk())
            return;

        List<String> storedArticles = notifier.getStoredArticles();
        if (storedArticles == null) {
            storedArticles = new ArrayList<>();
            notifier.setStoredArticles(storedArticles);
        }

        // Seed new subscriptions from the current feed and announce the latest matching article once.
        if (storedArticles.isEmpty()) {
            MinecraftArticle latestArticle = articles.get(0);
            sendUpdate(channel, notifier, latestArticle);
            storedArticles.addAll(articles.stream()
                    .map(MinecraftArticle::id)
                    .limit(STORED_ARTICLE_LIMIT)
                    .toList());
            persistStoredArticles(notifier);
            return;
        }

        boolean changed = false;
        for (int index = articles.size() - 1; index >= 0; index--) {
            MinecraftArticle article = articles.get(index);
            if (storedArticles.contains(article.id()))
                continue;

            sendUpdate(channel, notifier, article);
            storedArticles.add(article.id());
            trimStoredArticles(storedArticles);
            changed = true;
        }

        if (changed)
            persistStoredArticles(notifier);
    }

    private static void persistStoredArticles(MinecraftNotifier notifier) {
        Database.getDatabase().minecraftNotifier.updateOne(
                Filters.eq("guild", notifier.getGuild()),
                Updates.set("storedArticles", notifier.getStoredArticles()));
    }

    private static void trimStoredArticles(List<String> storedArticles) {
        while (storedArticles.size() > STORED_ARTICLE_LIMIT) {
            storedArticles.remove(0);
        }
    }

    private static void sendUpdate(TextChannel channel, MinecraftNotifier notifier, MinecraftArticle article) {
        String versionLabel = extractVersionLabel(article.title());
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("New Minecraft Version: " + versionLabel, article.link().isBlank() ? null : article.link())
                .setDescription(truncate(article.description(), MessageEmbed.DESCRIPTION_MAX_LENGTH))
                .setAuthor(article.author())
                .setColor(0x6CC349)
                .setTimestamp(article.publishedAt())
                .setFooter("Minecraft.net");

        channel.sendMessageEmbeds(embed.build())
                .setContent(notifier.getMention())
                .queue();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank())
            return "New Minecraft update article detected.";

        if (value.length() <= maxLength)
            return value;

        return value.substring(0, maxLength - 3) + "...";
    }

    private static Optional<String> getLink(Node node) {
        return findChild(node, "link").map(linkNode -> {
            NamedNodeMap attributes = linkNode.getAttributes();
            if (attributes != null) {
                Node href = attributes.getNamedItem("href");
                if (href != null && !href.getTextContent().isBlank())
                    return href.getTextContent();
            }

            return linkNode.getTextContent();
        }).filter(value -> value != null && !value.isBlank());
    }

    private static Optional<String> getChildText(Node parent, String... names) {
        return findChild(parent, names)
                .map(Node::getTextContent)
                .filter(value -> value != null && !value.isBlank());
    }

    private static Optional<Node> findChild(Node parent, String... names) {
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            String localName = getLocalName(child);
            for (String name : names) {
                if (name.equals(localName))
                    return Optional.of(child);
            }
        }

        return Optional.empty();
    }

    private static String getLocalName(Node node) {
        if (node == null)
            return "";

        String localName = node.getLocalName();
        if (localName != null)
            return localName;

        String nodeName = node.getNodeName();
        int colonIndex = nodeName.indexOf(':');
        return colonIndex >= 0 ? nodeName.substring(colonIndex + 1) : nodeName;
    }

    private static String extractVersionLabel(String title) {
        Matcher snapshotMatcher = SNAPSHOT_PATTERN.matcher(title);
        if (snapshotMatcher.find())
            return snapshotMatcher.group();

        Matcher versionWithSuffixMatcher = VERSION_WITH_SUFFIX_PATTERN.matcher(title);
        if (versionWithSuffixMatcher.find())
            return versionWithSuffixMatcher.group();

        Matcher versionMatcher = VERSION_PATTERN.matcher(title);
        if (versionMatcher.find())
            return versionMatcher.group();

        return title;
    }

    public static boolean isInitialized() {
        return IS_INITIALIZED.get();
    }

    private record MinecraftArticle(String id, String title, String link, String description, String author,
                                    Instant publishedAt) {}
}
