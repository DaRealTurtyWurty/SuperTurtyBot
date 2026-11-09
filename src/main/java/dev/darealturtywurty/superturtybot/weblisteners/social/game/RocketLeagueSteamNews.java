package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darealturtywurty.superturtybot.weblisteners.social.NewsScraperUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RocketLeagueSteamNews {
    private static final String ID_PREFIX = "steam:";
    private static final Pattern VERSION = Pattern.compile("\\bv(\\d{1,9})[.-](\\d{1,9})\\b|\\bv(\\d)(\\d{2,8})\\b",
        Pattern.CASE_INSENSITIVE);

    private RocketLeagueSteamNews() {
    }

    public static List<AbstractScrapedGameListener.ScrapedArticle> parse(String json) {
        JsonObject news = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("appnews");
        if (news == null || news.get("appid").getAsInt() != 252950 || !news.has("newsitems")
            || !news.get("newsitems").isJsonArray())
            throw new IllegalArgumentException("Unexpected Rocket League Steam feed format");

        List<AbstractScrapedGameListener.ScrapedArticle> articles = new ArrayList<>();
        for (JsonElement element : news.getAsJsonArray("newsitems")) {
            JsonObject item = element.getAsJsonObject();
            if (!"steam_community_announcements".equals(item.get("feedname").getAsString()))
                continue;
            String title = item.get("title").getAsString().strip();
            if (!title.toLowerCase(Locale.ROOT).contains("patch notes"))
                continue;

            String url = item.get("url").getAsString();
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException("Unexpected Rocket League Steam article URL");

            String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            String id = ID_PREFIX + item.get("gid").getAsString() + ":" + slug;
            Instant publishedAt = Instant.ofEpochSecond(item.get("date").getAsBigDecimal().longValueExact());
            articles.add(new AbstractScrapedGameListener.ScrapedArticle(id,
                NewsScraperUtils.truncate(title, MessageEmbed.TITLE_MAX_LENGTH, "Rocket League patch notes"),
                url, "", publishedAt, "Official Rocket League patch notes from the Steam announcements feed."));
        }
        articles.sort(Comparator.comparing(AbstractScrapedGameListener.ScrapedArticle::publishedAt).reversed());
        Set<Object> seen = new HashSet<>();
        return articles.stream().filter(article -> {
            PatchVersion version = version(article.id());
            return seen.add(version == null ? article.id() : version);
        }).toList();
    }

    public static boolean alreadyStored(List<String> storedIds, AbstractScrapedGameListener.ScrapedArticle article) {
        PatchVersion version = version(article.id());
        if (version == null)
            return false;

        for (String storedId : storedIds) {
            PatchVersion storedVersion = version(storedId);
            if (storedVersion == null)
                continue;
            if (article.id().startsWith(ID_PREFIX) && storedVersion.compareTo(version) >= 0)
                return true;
            if (storedId.startsWith(ID_PREFIX) && storedVersion.equals(version))
                return true;
        }
        return false;
    }

    private static PatchVersion version(String id) {
        if (id == null)
            return null;
        Matcher matcher = VERSION.matcher(id);
        if (!matcher.find())
            return null;
        return matcher.group(1) != null
            ? new PatchVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)))
            : new PatchVersion(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
    }

    private record PatchVersion(int major, int minor) implements Comparable<PatchVersion> {
        @Override
        public int compareTo(PatchVersion other) {
            int result = Integer.compare(major, other.major);
            return result == 0 ? Integer.compare(minor, other.minor) : result;
        }
    }
}
