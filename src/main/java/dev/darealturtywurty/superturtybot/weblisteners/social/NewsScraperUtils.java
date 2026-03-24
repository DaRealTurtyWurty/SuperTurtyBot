package dev.darealturtywurty.superturtybot.weblisteners.social;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NewsScraperUtils {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36";
    private static final Pattern ISO_TIMESTAMP_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z\\b");

    private NewsScraperUtils() {
    }

    public static Document fetchDocument(String url, String referer, String logPrefix) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Referer", referer)
                .header("Upgrade-Insecure-Requests", "1")
                .build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.error("Failed to fetch {} page {}. HTTP {}", logPrefix, url, response.code());
                return null;
            }

            ResponseBody body = response.body();
            if (body == null)
                return null;

            return Jsoup.parse(body.string(), url);
        }
    }

    public static String extractArticleTitle(Document document) {
        if (document == null)
            return "";

        Element heading = document.selectFirst("h1");
        if (heading != null) {
            String text = heading.text().trim();
            if (!text.isBlank())
                return text;
        }

        for (String selector : List.of("meta[property=og:title]", "meta[name=twitter:title]", "title")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String text = element.hasAttr("content") ? element.attr("content").trim() : element.text().trim();
            if (!text.isBlank())
                return text;
        }

        return "";
    }

    public static String extractPrimaryDescription(Document document) {
        String description = extractMetaDescription(document);
        if (!description.isBlank())
            return description;

        if (document == null)
            return "";

        Element paragraph = document.selectFirst("article p, main p, p");
        return paragraph == null ? "" : paragraph.text().trim();
    }

    public static String extractListDescription(String text, String title) {
        if (text == null || text.isBlank())
            return "";

        String cleaned = text.trim();
        String isoTimestamp = extractIsoTimestamp(cleaned);
        if (!isoTimestamp.isBlank()) {
            int timestampIndex = cleaned.indexOf(isoTimestamp);
            cleaned = cleaned.substring(timestampIndex + isoTimestamp.length()).trim();
        }

        if (title != null && !title.isBlank()) {
            int titleIndex = cleaned.indexOf(title);
            if (titleIndex >= 0) {
                cleaned = cleaned.substring(titleIndex + title.length()).trim();
            }
        }

        return cleaned;
    }

    public static String extractContextDescription(Element context, String title) {
        if (context == null)
            return "";

        for (Element candidate : context.select("p, div, span")) {
            String text = candidate.text().trim();
            if (text.isBlank() || text.equals(title) || text.equalsIgnoreCase("Read More"))
                continue;

            if (text.contains(title))
                continue;

            return text;
        }

        return "";
    }

    public static String extractHeadingText(Element element) {
        if (element == null)
            return "";

        Element heading = element.selectFirst("h1, h2, h3, h4, h5, h6");
        if (heading == null)
            return "";

        return heading.text().trim();
    }

    public static String extractMetaDescription(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "meta[property=og:description]",
                "meta[name=description]",
                "meta[name=twitter:description]")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String content = element.attr("content").trim();
            if (!content.isBlank())
                return content;
        }

        return "";
    }

    public static String extractMetaImage(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "meta[property=og:image]",
                "meta[name=twitter:image]",
                "meta[name=twitter:image:src]",
                "link[rel=image_src]")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String url = element.hasAttr("content") ? element.absUrl("content") : element.absUrl("href");
            if (!url.isBlank())
                return url;

            String raw = element.hasAttr("content") ? element.attr("content").trim() : element.attr("href").trim();
            if (raw.startsWith("//"))
                return "https:" + raw;

            if (!raw.isBlank())
                return raw;
        }

        return "";
    }

    public static String extractMetaPublishedTime(Document document) {
        if (document == null)
            return "";

        for (String selector : List.of(
                "meta[property=article:published_time]",
                "meta[name=article:published_time]",
                "meta[property=og:published_time]",
                "meta[name=publish-date]",
                "time[datetime]")) {
            Element element = document.selectFirst(selector);
            if (element == null)
                continue;

            String value = element.hasAttr("content") ? element.attr("content").trim() : element.attr("datetime").trim();
            if (!value.isBlank())
                return value;
        }

        return "";
    }

    public static Instant extractPublishedAt(Document articleDocument, String fallbackText) {
        String value = extractMetaPublishedTime(articleDocument);
        if (!value.isBlank()) {
            Instant parsed = parseFlexibleInstant(value);
            if (parsed != null)
                return parsed;
        }

        String isoValue = extractIsoTimestamp(fallbackText);
        if (!isoValue.isBlank()) {
            Instant parsed = parseFlexibleInstant(isoValue);
            if (parsed != null)
                return parsed;
        }

        return Instant.now();
    }

    public static String extractIsoTimestamp(String text) {
        if (text == null || text.isBlank())
            return "";

        Matcher matcher = ISO_TIMESTAMP_PATTERN.matcher(text);
        if (matcher.find())
            return matcher.group();

        return "";
    }

    public static Instant parseFlexibleInstant(String value) {
        if (value == null || value.isBlank())
            return null;

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return java.time.OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return java.time.ZonedDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    public static String extractImageUrl(Element context) {
        if (context == null)
            return "";

        Element image = context.selectFirst("img[src], img[data-src], img[data-lazy-src], source[srcset]");
        if (image == null)
            return "";

        String srcSet = image.absUrl("srcset");
        if (!srcSet.isBlank())
            return firstUrlFromSrcSet(srcSet);

        srcSet = image.attr("srcset");
        if (!srcSet.isBlank())
            return firstUrlFromSrcSet(srcSet);

        String url = image.absUrl("src");
        if (!url.isBlank())
            return url;

        url = image.absUrl("data-src");
        if (!url.isBlank())
            return url;

        url = image.absUrl("data-lazy-src");
        if (!url.isBlank())
            return url;

        String raw = image.attr("src");
        if (raw.startsWith("//"))
            return "https:" + raw;

        raw = image.attr("data-lazy-src");
        if (raw.startsWith("//"))
            return "https:" + raw;

        if (!raw.isBlank())
            return raw;

        raw = image.attr("srcset");
        if (!raw.isBlank())
            return firstUrlFromSrcSet(raw);

        return raw;
    }

    public static Element findContext(Element link) {
        Element current = link;
        for (int depth = 0; depth < 6 && current != null; depth++) {
            if (!extractImageUrl(current).isBlank() || hasHeading(current)) {
                return current;
            }

            current = current.parent();
        }

        return link.parent();
    }

    private static boolean hasHeading(Element element) {
        return element != null && element.selectFirst("h1, h2, h3, h4, h5, h6") != null;
    }

    public static String firstUrlFromSrcSet(String srcSet) {
        if (srcSet == null || srcSet.isBlank())
            return "";

        String candidate = srcSet.split(",")[0].trim();
        int spaceIndex = candidate.indexOf(' ');
        if (spaceIndex > 0) {
            candidate = candidate.substring(0, spaceIndex).trim();
        }

        if (candidate.startsWith("//"))
            return "https:" + candidate;

        return candidate;
    }

    public static String truncate(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank())
            return fallback;

        if (value.length() <= maxLength)
            return value;

        return value.substring(0, maxLength - 3) + "...";
    }
}
