package dev.darealturtywurty.superturtybot.dashboard.service.modmail.tickets;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.ModmailTranscriptEntry;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ModmailTicket;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ModmailTranscriptChunk;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModmailTicketsService {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>()\\[\\]{}\"']+");
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

    private final JDA jda;
    private final HttpClient previewClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ConcurrentMap<String, ModmailLinkPreviewResponse> previewCache = new ConcurrentHashMap<>();

    public ModmailTicketsService(JDA jda) {
        this.jda = jda;
    }

    public ModmailTicketsResponse listTickets(long guildId, String status) {
        Guild guild = requireGuild(guildId);
        boolean openOnly = "open".equalsIgnoreCase(status);
        boolean closedOnly = "closed".equalsIgnoreCase(status);

        List<ModmailTicketSummaryResponse> tickets = Database.getDatabase().modmailTickets.find(Filters.eq("guild", guildId))
                .sort(Sorts.descending("ticketNumber"))
                .into(new ArrayList<>())
                .stream()
                .filter(ticket -> !openOnly || ticket.isOpen())
                .filter(ticket -> !closedOnly || !ticket.isOpen())
                .map(ticket -> toSummary(guild, ticket))
                .toList();

        return new ModmailTicketsResponse(tickets);
    }

    public ModmailTicketDetailResponse getTicket(long guildId, long ticketNumber) {
        Guild guild = requireGuild(guildId);
        ModmailTicket ticket = Database.getDatabase().modmailTickets.find(
                Filters.and(Filters.eq("guild", guildId), Filters.eq("ticketNumber", ticketNumber))
        ).first();

        if (ticket == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "modmail_ticket_not_found",
                    "The requested modmail ticket could not be found.");
        }

        List<ModmailTranscriptEntryResponse> transcript = Database.getDatabase().modmailTranscriptChunks.find(
                        Filters.and(Filters.eq("guild", guildId), Filters.eq("ticketChannel", ticket.getChannel())))
                .sort(Sorts.ascending("chunkIndex"))
                .into(new ArrayList<>())
                .stream()
                .flatMap(chunk -> safeEntries(chunk).stream())
                .map(entry -> toTranscriptEntry(guild, entry))
                .toList();

        return new ModmailTicketDetailResponse(
                toSummary(guild, ticket),
                ticket.getOpenerMessage(),
                transcript
        );
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private ModmailTicketSummaryResponse toSummary(Guild guild, ModmailTicket ticket) {
        String userDisplayName = resolveMemberName(guild, ticket.getUser());
        String channelName = resolveChannelName(guild, ticket.getChannel());
        String categoryName = resolveCategoryName(guild, ticket.getCategory());
        String closedByName = ticket.getClosedBy() == 0L ? "" : resolveMemberName(guild, ticket.getClosedBy());

        return new ModmailTicketSummaryResponse(
                ticket.getTicketNumber(),
                ticket.getUser(),
                userDisplayName,
                resolveAvatarUrl(guild, ticket.getUser()),
                ticket.getChannel(),
                channelName,
                ticket.getCategory(),
                categoryName,
                ticket.isOpen(),
                ticket.getSource(),
                ticket.getOpenedAt(),
                ticket.getClosedAt(),
                ticket.getClosedBy(),
                closedByName,
                ticket.getCloseReason(),
                ticket.getTranscriptChunkCount(),
                ticket.getTranscriptMessageCount()
        );
    }

    private ModmailTranscriptEntryResponse toTranscriptEntry(Guild guild, ModmailTranscriptEntry entry) {
        return new ModmailTranscriptEntryResponse(
                entry.getMessageId(),
                entry.getAuthorId(),
                entry.getAuthorTag(),
                resolveAvatarUrl(guild, entry.getAuthorId()),
                entry.isBot(),
                entry.getContent(),
                buildPreviews(entry),
                copyList(entry.getAttachments()),
                copyList(entry.getEmbeds()),
                copyList(entry.getStickers()),
                entry.getCreatedAt(),
                entry.getEditedAt()
        );
    }

    private List<ModmailLinkPreviewResponse> buildPreviews(ModmailTranscriptEntry entry) {
        Set<String> urls = new LinkedHashSet<>(extractUrls(entry.getContent()));

        for (String attachment : copyList(entry.getAttachments())) {
            String attachmentUrl = parseAttachmentUrl(attachment);
            if (!attachmentUrl.isBlank()) {
                urls.add(attachmentUrl);
            }
        }

        return urls.stream()
                .limit(3)
                .map(this::previewForUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    private ModmailLinkPreviewResponse previewForUrl(String rawUrl) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl.isBlank()) {
            return null;
        }

        return this.previewCache.computeIfAbsent(normalizedUrl, this::loadPreview);
    }

    private ModmailLinkPreviewResponse loadPreview(String url) {
        URI uri = parseSafeUri(url);
        if (uri == null) {
            return fallbackLinkPreview(url, null);
        }

        if (isDirectImageUrl(uri)) {
            return new ModmailLinkPreviewResponse(
                    url,
                    fileNameFromUri(uri),
                    hostFromUri(uri),
                    hostFromUri(uri),
                    url,
                    "image"
            );
        }

        try {
            HttpResponse<Void> headResponse = this.previewClient.send(HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0 (compatible; TurtyBot/1.0)")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build(), HttpResponse.BodyHandlers.discarding());

            String contentType = headResponse.headers().firstValue("content-type").orElse("");
            if (contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return new ModmailLinkPreviewResponse(
                        url,
                        fileNameFromUri(uri),
                        hostFromUri(uri),
                        hostFromUri(uri),
                        url,
                        "image"
                );
            }

            if (!contentType.toLowerCase(Locale.ROOT).contains("html") && !contentType.isBlank()
                    && headResponse.statusCode() < 400) {
                return fallbackLinkPreview(url, uri);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return fallbackLinkPreview(url, uri);
        } catch (IOException | IllegalArgumentException ignored) {
            return fallbackLinkPreview(url, uri);
        }

        try {
            HttpResponse<String> response = this.previewClient.send(HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "Mozilla/5.0 (compatible; TurtyBot/1.0)")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                return fallbackLinkPreview(url, uri);
            }

            String html = response.body();
            if (html == null || html.isBlank()) {
                return fallbackLinkPreview(url, uri);
            }

            String title = firstNonBlank(
                    extractMetaContent(html, "og:title"),
                    extractMetaContent(html, "twitter:title"),
                    extractTitle(html),
                    fileNameFromUri(uri),
                    hostFromUri(uri)
            );
            String description = firstNonBlank(
                    extractMetaContent(html, "og:description"),
                    extractMetaContent(html, "twitter:description"),
                    extractMetaContent(html, "description")
            );
            String siteName = firstNonBlank(
                    extractMetaContent(html, "og:site_name"),
                    hostFromUri(uri)
            );
            String imageUrl = firstNonBlank(
                    resolvePreviewUrl(uri, extractMetaContent(html, "og:image")),
                    resolvePreviewUrl(uri, extractMetaContent(html, "twitter:image"))
            );
            String type = firstNonBlank(
                    extractMetaContent(html, "og:type"),
                    "link"
            );

            if (title == null && description == null && imageUrl == null) {
                return fallbackLinkPreview(url, uri);
            }

            return new ModmailLinkPreviewResponse(
                    url,
                    title,
                    description,
                    siteName,
                    imageUrl,
                    type
            );
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return fallbackLinkPreview(url, uri);
        } catch (IOException | IllegalArgumentException ignored) {
            return fallbackLinkPreview(url, uri);
        }
    }

    private static ModmailLinkPreviewResponse fallbackLinkPreview(String url, URI uri) {
        String host = hostFromUri(uri);
        String path = uri == null ? url : uri.getPath();
        String description = path == null || path.isBlank() ? null : path;
        return new ModmailLinkPreviewResponse(
                url,
                host,
                description,
                host,
                null,
                "link"
        );
    }

    private static List<String> extractUrls(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = normalizeUrl(matcher.group());
            if (!candidate.isBlank() && !urls.contains(candidate)) {
                urls.add(candidate);
            }
        }

        return urls;
    }

    private static String parseAttachmentUrl(String attachment) {
        if (attachment == null || attachment.isBlank()) {
            return "";
        }

        int index = attachment.indexOf(" (");
        if (index > 0) {
            return normalizeUrl(attachment.substring(0, index));
        }

        return normalizeUrl(attachment);
    }

    private static String normalizeUrl(String value) {
        if (value == null) {
            return "";
        }

        String result = value.trim();
        while (!result.isBlank() && ".,!?:;)]}".indexOf(result.charAt(result.length() - 1)) >= 0) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private static URI parseSafeUri(String url) {
        try {
            URI uri = new URI(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }

            if ("localhost".equalsIgnoreCase(host)) {
                return null;
            }

            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    return null;
                }

                if (address instanceof Inet6Address inet6Address && inet6Address.isIPv4CompatibleAddress()) {
                    return null;
                }
            }

            return uri;
        } catch (URISyntaxException | UnknownHostException ignored) {
            return null;
        }
    }

    private static boolean isDirectImageUrl(URI uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }

        return path.matches("(?i).+\\.(png|jpe?g|gif|webp|bmp|avif)$");
    }

    private static String extractMetaContent(String html, String property) {
        String quotedProperty = Pattern.quote(property);
        Pattern propertyFirst = Pattern.compile("(?is)<meta\\b[^>]*(?:property|name)=[\"']" + quotedProperty + "[\"'][^>]*content=[\"']([^\"']*)[\"'][^>]*>");
        Matcher matcher = propertyFirst.matcher(html);
        if (matcher.find()) {
            return decodeHtml(matcher.group(1));
        }

        Pattern contentFirst = Pattern.compile("(?is)<meta\\b[^>]*content=[\"']([^\"']*)[\"'][^>]*(?:property|name)=[\"']" + quotedProperty + "[\"'][^>]*>");
        matcher = contentFirst.matcher(html);
        if (matcher.find()) {
            return decodeHtml(matcher.group(1));
        }

        return null;
    }

    private static String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }

        return decodeHtml(matcher.group(1)).replaceAll("\\s+", " ").trim();
    }

    private static String resolvePreviewUrl(URI base, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(value);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
        } catch (URISyntaxException ignored) {
            return null;
        }

        return base.resolve(value).toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }

    private static String hostFromUri(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getHost().isBlank()) {
            return "Link";
        }

        return uri.getHost().startsWith("www.") ? uri.getHost().substring(4) : uri.getHost();
    }

    private static String fileNameFromUri(URI uri) {
        if (uri == null || uri.getPath() == null || uri.getPath().isBlank() || "/".equals(uri.getPath())) {
            return hostFromUri(uri);
        }

        String path = uri.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : hostFromUri(uri);
    }

    private static String decodeHtml(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .trim();
    }

    private static List<ModmailTranscriptEntry> safeEntries(ModmailTranscriptChunk chunk) {
        if (chunk == null || chunk.getEntries() == null) {
            return List.of();
        }

        return chunk.getEntries();
    }

    private static <T> List<T> copyList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return List.copyOf(values);
    }

    private static String resolveMemberName(Guild guild, long userId) {
        Member member = guild.getMemberById(userId);
        if (member != null) {
            return member.getEffectiveName();
        }

        var user = guild.getJDA().getUserById(userId);
        if (user != null) {
            if (user.getGlobalName() != null && !user.getGlobalName().isBlank()) {
                return user.getGlobalName();
            }

            return user.getName();
        }

        return "Unknown User";
    }

    private static String resolveChannelName(Guild guild, long channelId) {
        GuildChannel channel = guild.getGuildChannelById(channelId);
        if (channel != null) {
            return "#" + channel.getName();
        }

        return "";
    }

    private static String resolveCategoryName(Guild guild, long categoryId) {
        if (categoryId == 0L) {
            return "No Category";
        }

        var category = guild.getCategoryById(categoryId);
        if (category != null) {
            return category.getName();
        }

        return "Deleted Category";
    }

    private String resolveAvatarUrl(Guild guild, long userId) {
        Member member = guild.getMemberById(userId);
        if (member != null) {
            return member.getEffectiveAvatarUrl();
        }

        var user = this.jda.getUserById(userId);
        return user == null ? null : user.getEffectiveAvatarUrl();
    }
}
