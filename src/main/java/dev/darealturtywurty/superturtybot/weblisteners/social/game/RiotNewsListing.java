package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.darealturtywurty.superturtybot.weblisteners.social.NewsScraperUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class RiotNewsListing {
    private static final int INITIAL_CARD_LIMIT = 12;

    private RiotNewsListing() {
    }

    public static Optional<List<AbstractScrapedGameListener.ScrapedArticle>> parse(
        Document document,
        String listingUrl,
        Predicate<String> matchesTitle
    ) {
        Element script = document.selectFirst("script#__NEXT_DATA__[type=application/json]");
        if (script == null)
            return Optional.empty();

        JsonElement root;
        try {
            root = JsonParser.parseString(script.data());
        } catch (JsonParseException exception) {
            return Optional.empty();
        }
        JsonObject page = objectAt(root, "props", "pageProps", "page");
        if (page == null || !page.has("blades") || !page.get("blades").isJsonArray())
            return Optional.empty();

        URI base = URI.create(listingUrl);
        List<AbstractScrapedGameListener.ScrapedArticle> articles = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean foundGrid = false;
        for (JsonElement blade : page.getAsJsonArray("blades")) {
            JsonObject grid = objectAt(blade);
            if (grid == null || !"articleCardGrid".equals(text(grid, "type")))
                continue;
            if (!grid.has("items") || !grid.get("items").isJsonArray())
                throw new IllegalStateException("Riot article grid is missing its items array");
            foundGrid = true;
            JsonArray items = grid.getAsJsonArray("items");
            for (int index = 0; index < Math.min(items.size(), INITIAL_CARD_LIMIT); index++) {
                JsonElement item = items.get(index);
                JsonObject card = objectAt(item);
                if (card == null)
                    throw new IllegalStateException("Riot article card is not an object");
                String title = text(card, "title");
                if (title.isBlank())
                    throw new IllegalStateException("Riot article card is missing its title");
                if (!matchesTitle.test(title))
                    continue;
                String href = text(objectAt(card, "action", "payload"), "url");
                if (href.isBlank())
                    throw new IllegalStateException("Riot patch article is missing its URL");
                URI url = base.resolve(href);
                if (!"https".equalsIgnoreCase(url.getScheme()) || !base.getHost().equalsIgnoreCase(url.getHost())
                    || !url.getPath().startsWith("/en-us/news/game-updates/"))
                    continue;
                if (!seen.add(url.toString()))
                    continue;

                String published = text(card, "publishedAt");
                if (published.isBlank()) {
                    published = text(objectAt(card, "analytics"), "publishDate");
                }
                Instant publishedAt = NewsScraperUtils.parseFlexibleInstant(published);
                if (publishedAt == null)
                    throw new IllegalStateException("Riot patch article is missing a valid publication date");
                String description = Jsoup.parse(text(objectAt(card, "description"), "body")).text();
                String image = text(objectAt(card, "imageMedia"), "url");
                if (image.isBlank()) {
                    image = text(objectAt(card, "media"), "url");
                }
                articles.add(new AbstractScrapedGameListener.ScrapedArticle(url.toString(),
                    NewsScraperUtils.truncate(title, MessageEmbed.TITLE_MAX_LENGTH, "Patch notes"), url.toString(),
                    image, publishedAt, description));
            }
        }
        articles.sort(Comparator.comparing(AbstractScrapedGameListener.ScrapedArticle::publishedAt).reversed());
        return foundGrid ? Optional.of(articles) : Optional.empty();
    }

    private static JsonObject objectAt(JsonElement element, String... path) {
        for (String key : path) {
            if (element == null || !element.isJsonObject())
                return null;
            element = element.getAsJsonObject().get(key);
        }
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String text(JsonObject object, String key) {
        if (object == null)
            return "";
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString().strip()
            : "";
    }
}
