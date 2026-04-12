package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.commands.core.suggestion.SuggestionManager;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.SuggestionResponse;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SuggestionsDashboardService {
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 25;

    private final JDA jda;
    private final HttpClient previewClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ConcurrentMap<String, DashboardSuggestionMediaPreviewResponse> previewCache = new ConcurrentHashMap<>();

    public SuggestionsDashboardService(JDA jda) {
        this.jda = jda;
    }

    public DashboardSuggestionsPageResponse getSuggestions(long guildId, int page, int pageSize) {
        Guild guild = requireGuild(guildId);
        long totalCount = Database.getDatabase().suggestions.countDocuments(Filters.eq("guild", guild.getIdLong()));
        int safePageSize = clampPageSize(pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) safePageSize));
        int safePage = clampPage(page, totalPages);
        long offset = (long) (safePage - 1) * safePageSize;

        return new DashboardSuggestionsPageResponse(
                guildDataChannelId(guildId),
                safePage,
                safePageSize,
                totalCount,
                totalPages,
                listSuggestions(guild, offset, safePageSize, totalCount)
        );
    }

    public DashboardSuggestionsPageResponse moderateSuggestion(
            long guildId,
            String messageId,
            SuggestionActionRequest request,
            int page,
            int pageSize
    ) {
        Guild guild = requireGuild(guildId);
        Member actor = requireManager(guild, request == null ? null : request.getActorUserId());
        TextChannel suggestionChannel = requireSuggestionChannel(guild);
        Suggestion suggestion = requireSuggestion(guildId, messageId);
        int suggestionNumber = getSuggestionNumber(guildId, suggestion.getMessage());

        SuggestionResponse.Type actionType = parseAction(request == null ? null : request.getAction());
        String reason = request == null || request.getReason() == null || request.getReason().isBlank()
                ? "Unspecified"
                : request.getReason().trim();

        SuggestionManager.respondSuggestion(guild, suggestionChannel, actor, suggestionNumber, reason, actionType).join();
        return getSuggestions(guildId, page, pageSize);
    }

    public DashboardSuggestionsPageResponse deleteSuggestion(
            long guildId,
            String messageId,
            SuggestionActionRequest request,
            int page,
            int pageSize
    ) {
        Guild guild = requireGuild(guildId);
        Member actor = requireManager(guild, request == null ? null : request.getActorUserId());
        TextChannel suggestionChannel = requireSuggestionChannel(guild);
        Suggestion suggestion = requireSuggestion(guildId, messageId);
        int suggestionNumber = getSuggestionNumber(guildId, suggestion.getMessage());

        SuggestionManager.deleteSuggestion(guild, suggestionChannel, actor, suggestionNumber).join();
        return getSuggestions(guildId, page, pageSize);
    }

    private List<DashboardSuggestionRecord> listSuggestions(Guild guild, long offset, int pageSize, long totalCount) {
        List<Suggestion> suggestions = Database.getDatabase().suggestions.find(Filters.eq("guild", guild.getIdLong()))
                .sort(Sorts.descending("createdAt"))
                .skip((int) offset)
                .limit(pageSize)
                .into(new ArrayList<>());

        List<DashboardSuggestionRecord> records = new ArrayList<>(suggestions.size());
        for (int index = 0; index < suggestions.size(); index++) {
            records.add(toRecord(guild, suggestions.get(index), totalCount, offset, index));
        }

        return records;
    }

    private DashboardSuggestionRecord toRecord(Guild guild, Suggestion suggestion, long totalCount, long offset, int pageIndex) {
        var resolvedSuggestion = resolveSuggestionMessage(guild, suggestion);
        List<DashboardSuggestionResponseEntry> responses = suggestion.getResponses().stream()
                .map(response -> toResponseEntry(guild, response))
                .sorted(Comparator.comparingLong(DashboardSuggestionResponseEntry::respondedAt))
                .toList();

        String status = responses.isEmpty() ? "PENDING" : responses.getLast().type();
        int number = Math.toIntExact(totalCount - 1 - offset - pageIndex);
        String messageUrl = resolvedSuggestion.messageUrl();

        return new DashboardSuggestionRecord(
                number,
                Long.toString(suggestion.getMessage()),
                messageUrl,
                Long.toString(suggestion.getUser()),
                resolvedSuggestion.userDisplayName(),
                resolvedSuggestion.userAvatarUrl(),
                resolvedSuggestion.content(),
                resolvedSuggestion.mediaUrl(),
                resolvedSuggestion.mediaPreview(),
                suggestion.getCreatedAt(),
                status,
                responses
        );
    }

    private DashboardSuggestionResponseEntry toResponseEntry(Guild guild, SuggestionResponse response) {
        long responderId = response.getResponder();
        ResolvedUser responder = resolveUser(guild, responderId);
        return new DashboardSuggestionResponseEntry(
                response.getType(),
                response.getContent(),
                Long.toString(responderId),
                responder.displayName(),
                responder.avatarUrl(),
                response.getRespondedAt()
        );
    }

    private Suggestion requireSuggestion(long guildId, String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_suggestion_message_id",
                    "The suggestion message ID was missing.");
        }

        try {
            long parsedMessageId = Long.parseLong(messageId.trim());
            Suggestion suggestion = Database.getDatabase().suggestions.find(Filters.and(
                    Filters.eq("guild", guildId),
                    Filters.eq("message", parsedMessageId)
            )).first();
            if (suggestion == null) {
                throw new DashboardApiException(HttpStatus.NOT_FOUND, "suggestion_not_found",
                        "That suggestion could not be found.");
            }

            return suggestion;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_suggestion_message_id",
                    "The suggestion message ID was not a valid Discord snowflake.");
        }
    }

    private int getSuggestionNumber(long guildId, long messageId) {
        List<Suggestion> suggestions = Database.getDatabase().suggestions.find(Filters.eq("guild", guildId))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());

        List<Suggestion> orderedSuggestions = suggestions.stream()
                .sorted(Comparator.comparingLong(Suggestion::getCreatedAt))
                .toList();

        for (int index = 0; index < orderedSuggestions.size(); index++) {
            if (orderedSuggestions.get(index).getMessage() == messageId) {
                return index;
            }
        }

        throw new DashboardApiException(HttpStatus.NOT_FOUND, "suggestion_not_found",
                "That suggestion could not be found.");
    }

    private int clampPageSize(int pageSize) {
        return Math.clamp(pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize, 1, MAX_PAGE_SIZE);
    }

    private int clampPage(int page, int totalPages) {
        int safePage = page <= 0 ? 1 : page;
        return Math.clamp(safePage, 1, Math.max(1, totalPages));
    }

    private String guildDataChannelId(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return guildData.getSuggestions() == 0L ? null : Long.toString(guildData.getSuggestions());
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private Member requireManager(Guild guild, String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_actor_user_id",
                    "The dashboard user ID was missing.");
        }

        try {
            long parsedUserId = Long.parseLong(actorUserId.trim());
            Member member = guild.getMemberById(parsedUserId);
            if (member == null) {
                throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_member_not_found",
                        "The dashboard user is not a member of this guild.");
            }

            if (!member.hasPermission(Permission.MANAGE_SERVER) && !member.hasPermission(Permission.ADMINISTRATOR)) {
                throw new DashboardApiException(HttpStatus.FORBIDDEN, "dashboard_insufficient_permissions",
                        "You need Manage Server permission to moderate suggestions.");
            }

            return member;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_actor_user_id",
                    "The dashboard user ID was not a valid Discord snowflake.");
        }
    }

    private TextChannel requireSuggestionChannel(Guild guild) {
        TextChannel suggestionChannel = SuggestionManager.getSuggestionChannel(guild);
        if (suggestionChannel == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "suggestions_channel_not_found",
                    "The suggestions channel could not be resolved for this guild.");
        }

        return suggestionChannel;
    }

    private SuggestionResponse.Type parseAction(String action) {
        if (action == null || action.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_suggestion_action",
                    "The suggestion action was missing.");
        }

        try {
            return SuggestionResponse.Type.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_suggestion_action",
                    "The suggestion action was not recognized.");
        }
    }

    private ResolvedSuggestionMessage resolveSuggestionMessage(Guild guild, Suggestion suggestion) {
        ResolvedUser author = resolveUser(guild, suggestion.getUser());
        TextChannel suggestionChannel = SuggestionManager.getSuggestionChannel(guild);
        if (suggestionChannel == null) {
            return new ResolvedSuggestionMessage("Suggestion message unavailable.", null, null, null, author.displayName(), author.avatarUrl());
        }

        try {
            Message message = suggestionChannel.retrieveMessageById(suggestion.getMessage()).complete();

            String content = "Suggestion message unavailable.";
            String mediaUrl = null;
            DashboardSuggestionMediaPreviewResponse mediaPreview = null;
            if (!message.getEmbeds().isEmpty()) {
                var embed = message.getEmbeds().getFirst();
                if (embed.getDescription() != null && !embed.getDescription().isBlank()) {
                    content = stripMediaFallback(embed.getDescription());
                }

                if (embed.getImage() != null) {
                    mediaUrl = embed.getImage().getUrl();
                }
            } else if (message.getContentRaw() != null && !message.getContentRaw().isBlank()) {
                content = message.getContentRaw();
            }

            if (mediaUrl != null && !mediaUrl.isBlank()) {
                mediaPreview = previewForUrl(mediaUrl);
            }

            return new ResolvedSuggestionMessage(content, mediaUrl, mediaPreview,
                    messageUrl(guild.getIdLong(), suggestionChannel.getIdLong(), suggestion.getMessage()),
                    author.displayName(),
                    author.avatarUrl());
        } catch (Exception exception) {
            return new ResolvedSuggestionMessage(
                    "Suggestion message unavailable.",
                    null,
                    null,
                    messageUrl(guild.getIdLong(), suggestionChannel.getIdLong(), suggestion.getMessage()),
                    author.displayName(),
                    author.avatarUrl()
            );
        }
    }

    private String messageUrl(long guildId, long channelId, long messageId) {
        return "https://discord.com/channels/%s/%s/%s".formatted(guildId, channelId, messageId);
    }

    private DashboardSuggestionMediaPreviewResponse previewForUrl(String rawUrl) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl.isBlank()) {
            return null;
        }

        return this.previewCache.computeIfAbsent(normalizedUrl, this::loadPreview);
    }

    private DashboardSuggestionMediaPreviewResponse loadPreview(String url) {
        URI uri = parseSafeUri(url);
        if (uri == null) {
            return fallbackLinkPreview(url, null);
        }

        if (isDirectImageUrl(uri)) {
            return new DashboardSuggestionMediaPreviewResponse(
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
                return new DashboardSuggestionMediaPreviewResponse(
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

            return new DashboardSuggestionMediaPreviewResponse(
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

    private static DashboardSuggestionMediaPreviewResponse fallbackLinkPreview(String url, URI uri) {
        String host = hostFromUri(uri);
        String path = uri == null ? url : uri.getPath();
        String description = path == null || path.isBlank() ? null : path;
        return new DashboardSuggestionMediaPreviewResponse(
                url,
                host,
                description,
                host,
                null,
                "link"
        );
    }

    private String stripMediaFallback(String description) {
        final String marker = "\n\n**Media not showing? [Click Me](";
        final int markerIndex = description.indexOf(marker);
        if (markerIndex >= 0 && description.endsWith(")**")) {
            return description.substring(0, markerIndex);
        }

        return description;
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
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return null;
            }

            return uri;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static boolean isDirectImageUrl(URI uri) {
        String path = uri.getPath();
        return path != null && path.matches("(?i).+\\.(png|jpe?g|gif|webp|bmp|avif)$");
    }

    private static String extractMetaContent(String html, String property) {
        Pattern metaPattern = Pattern.compile("(?is)<meta[^>]+(?:property|name)=[\"']" + Pattern.quote(property) + "[\"'][^>]+content=[\"']([^\"']+)[\"']");
        Matcher matcher = metaPattern.matcher(html);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private static String resolvePreviewUrl(URI base, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            URI parsed = new URI(value);
            if (parsed.getScheme() != null) {
                return value;
            }
        } catch (URISyntaxException ignored) {
        }

        try {
            return base.resolve(value).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private static String hostFromUri(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getHost().isBlank()) {
            return "Link";
        }

        return uri.getHost().replaceFirst("^www\\.", "");
    }

    private static String fileNameFromUri(URI uri) {
        if (uri == null || uri.getPath() == null || uri.getPath().isBlank()) {
            return hostFromUri(uri);
        }

        String path = uri.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : hostFromUri(uri);
    }

    private ResolvedUser resolveUser(Guild guild, long userId) {
        Member member = guild.getMemberById(userId);
        if (member != null) {
            return new ResolvedUser(member.getEffectiveName(), member.getEffectiveAvatarUrl());
        }

        User user = this.jda.getUserById(userId);
        if (user != null) {
            return new ResolvedUser(user.getName(), user.getEffectiveAvatarUrl());
        }

        return new ResolvedUser("Unknown User", null);
    }

    private record ResolvedSuggestionMessage(String content, String mediaUrl, DashboardSuggestionMediaPreviewResponse mediaPreview, String messageUrl, String userDisplayName, String userAvatarUrl) {
    }

    private record ResolvedUser(String displayName, String avatarUrl) {
    }
}
