package dev.darealturtywurty.superturtybot.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Message;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.SocketException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for loading, caching, and keeping scam-domain data in sync with phish.sinking.yachts.
 */
public class ScamDomainDetector {
    private static final String ALL_URL = "https://phish.sinking.yachts/v2/all";
    private static final String RECENT_URL_TEMPLATE = "https://phish.sinking.yachts/v2/recent/%d";
    private static final String WEBSOCKET_URL = "wss://phish.sinking.yachts/feed";
    private static final Duration CACHE_EXPIRY = Duration.ofDays(7);
    private static final Duration WEBSOCKET_PING_INTERVAL = Duration.ofMinutes(4);
    private static final Duration WEBSOCKET_RECONNECT_BASE_DELAY = Duration.ofSeconds(5);
    private static final Duration WEBSOCKET_RECONNECT_MAX_DELAY = Duration.ofMinutes(5);
    private static final String USER_AGENT = "SuperTurtyBot/1.0 (https://github.com/darealturtywurty/SuperTurtyBot)";
    private static final Path CACHE_FILE = Path.of("cache", "scam_domains_cache.json");
    private static final Pattern URL_PATTERN = Pattern
        .compile("(?i)\\b((?:https?://)?[\\w.-]+\\.[a-z]{2,})(?:/[^\\s]*)?");

    private final Set<String> scamDomains = ConcurrentHashMap.newKeySet();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "ScamDomainDetector-WebSocket");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger consecutiveWebSocketFailures = new AtomicInteger();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private final AtomicReference<WebSocket> activeWebSocket = new AtomicReference<>();

    private volatile long lastUpdatedEpochSecond = 0L;
    private volatile ScheduledFuture<?> pingTask;

    public void start() {
        ensureCacheDirectory();
        loadCache();

        final Instant now = Instant.now();
        if (shouldRefreshAll(now)) {
            fetchAllDomains();
        } else if (shouldFetchRecent(now)) {
            fetchRecentUpdates();
        }

        startWebSocket();
    }

    public void handleMessage(Message message) {
        if (!message.isFromGuild())
            return;

        final GuildData config = Database.getDatabase().guildData
            .find(Filters.eq("guild", message.getGuild().getIdLong())).first();
        if (config == null || !config.isScamDetectionEnabled())
            return;

        final String content = message.getContentRaw();
        if (content.isBlank())
            return;

        if (containsScamDomain(content)) {
            message.delete().flatMap(success -> message.getChannel()
                .sendMessage(message.getAuthor().getAsMention() + ", do NOT send scam links! " +
                    "If this was not you, then your account has been compromised. " +
                    "Please make sure to reset your login details to get your token changed. " +
                    "In the future, be more careful what URLs you are opening, always check that it's the real one."))
                .queue();
        }
    }

    private boolean shouldRefreshAll(Instant now) {
        if (lastUpdatedEpochSecond <= 0 || scamDomains.isEmpty())
            return true;

        final Duration age = Duration.between(Instant.ofEpochSecond(lastUpdatedEpochSecond), now);
        return age.compareTo(CACHE_EXPIRY) >= 0;
    }

    private boolean shouldFetchRecent(Instant now) {
        if (lastUpdatedEpochSecond <= 0)
            return false;

        final Duration age = Duration.between(Instant.ofEpochSecond(lastUpdatedEpochSecond), now);
        return age.isPositive() && age.compareTo(CACHE_EXPIRY) < 0;
    }

    private void fetchAllDomains() {
        try {
            final HttpRequest request = HttpRequest.newBuilder(URI.create(ALL_URL))
                .header("User-Agent", USER_AGENT)
                .header("X-Identity", USER_AGENT)
                .GET()
                .build();
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Constants.LOGGER.warn("Failed to fetch scam domains. Status: {}", response.statusCode());
                return;
            }

            final String[] responseDomains = Constants.GSON.fromJson(response.body(), String[].class);
            if (responseDomains == null) {
                Constants.LOGGER.warn("Scam domain response was empty.");
                return;
            }

            this.scamDomains.clear();
            for (final String domain : responseDomains) {
                final String normalized = normalizeDomain(domain);
                if (normalized != null) {
                    this.scamDomains.add(normalized);
                }
            }

            this.lastUpdatedEpochSecond = Instant.now().getEpochSecond();
            saveCache();
            Constants.LOGGER.info("Loaded {} scam domains from full dataset.", this.scamDomains.size());
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            Constants.LOGGER.error("Failed to fetch scam domains!", exception);
        } catch (final IOException exception) {
            Constants.LOGGER.error("Failed to fetch scam domains!", exception);
        }
    }

    private void fetchRecentUpdates() {
        if (this.lastUpdatedEpochSecond <= 0)
            return;

        try {
            final String url = RECENT_URL_TEMPLATE.formatted(recentLookbackSeconds(Instant.now()));
            final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("X-Identity", USER_AGENT)
                .GET()
                .build();
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Constants.LOGGER.warn("Failed to fetch recent scam domains. Status: {}", response.statusCode());
                return;
            }

            final ScamUpdate[] updates = parseUpdates(response.body());
            if (updates == null || updates.length == 0) {
                Constants.LOGGER.debug("No recent scam domain updates found.");
                return;
            }

            applyUpdates(updates);
            this.lastUpdatedEpochSecond = Instant.now().getEpochSecond();
            saveCache();
            Constants.LOGGER.info("Applied {} recent scam-domain updates.", updates.length);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            Constants.LOGGER.error("Failed to fetch recent scam-domain updates!", exception);
        } catch (final IOException | JsonParseException exception) {
            Constants.LOGGER.error("Failed to fetch recent scam-domain updates!", exception);
        }
    }

    private long recentLookbackSeconds(Instant now) {
        return Math.clamp(now.getEpochSecond() - this.lastUpdatedEpochSecond, 1L, CACHE_EXPIRY.getSeconds());
    }

    private static ScamUpdate[] parseUpdates(String payload) {
        JsonElement root = JsonParser.parseString(payload);
        if (root.isJsonNull())
            return new ScamUpdate[0];

        List<ScamUpdate> updates = new ArrayList<>();
        if (root.isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray()) {
                parseUpdate(element, updates);
            }
        } else {
            parseUpdate(root, updates);
        }
        return updates.toArray(ScamUpdate[]::new);
    }

    private static void parseUpdate(JsonElement element, List<ScamUpdate> updates) {
        if (element.isJsonNull())
            return;
        if (!element.isJsonObject())
            throw new JsonParseException("Expected a scam-domain update object or array of update objects");

        JsonObject object = element.getAsJsonObject();
        JsonElement type = object.get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString())
            return;
        String action = type.getAsString();
        if (!"add".equalsIgnoreCase(action) && !"delete".equalsIgnoreCase(action))
            return;

        JsonElement domains = object.get("domains");
        if (domains == null || !domains.isJsonArray())
            throw new JsonParseException("Scam-domain update is missing a domains array");

        List<String> values = new ArrayList<>();
        for (JsonElement domain : domains.getAsJsonArray()) {
            if (domain.isJsonNull())
                continue;
            if (!domain.isJsonPrimitive() || !domain.getAsJsonPrimitive().isString())
                throw new JsonParseException("Scam-domain update contains a non-string domain");
            values.add(domain.getAsString());
        }
        updates.add(new ScamUpdate(action, values));
    }

    private void applyUpdates(ScamUpdate[] updates) {
        for (final ScamUpdate update : updates) {
            if (update == null || update.domains() == null)
                continue;

            if ("delete".equalsIgnoreCase(update.type())) {
                update.domains().stream()
                    .map(this::normalizeDomain)
                    .filter(Objects::nonNull)
                    .forEach(this.scamDomains::remove);
            } else if ("add".equalsIgnoreCase(update.type())) {
                update.domains().stream()
                    .map(this::normalizeDomain)
                    .filter(Objects::nonNull)
                    .forEach(this.scamDomains::add);
            }
        }
    }

    private void startWebSocket() {
        this.httpClient.newWebSocketBuilder()
            .header("User-Agent", USER_AGENT)
            .header("X-Identity", USER_AGENT)
            .buildAsync(URI.create(WEBSOCKET_URL), createWebSocketListener()).exceptionally(error -> {
                logWebSocketFailure("Failed to connect to scam-domain websocket feed", error);
                scheduleReconnect();
                return null;
            });
    }

    private WebSocket.Listener createWebSocketListener() {
        return new WebSocket.Listener() {
            private final StringBuilder textBuffer = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket) {
                final WebSocket previousWebSocket = activeWebSocket.getAndSet(webSocket);
                if (previousWebSocket != null && previousWebSocket != webSocket) {
                    previousWebSocket.abort();
                }

                webSocket.request(1);
                consecutiveWebSocketFailures.set(0);
                startPing(webSocket);
                // Constants.LOGGER.info("Connected to scam-domain websocket feed.");
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    if (activeWebSocket.get() != webSocket)
                        return null;
                    this.textBuffer.append(data);
                    if (!last)
                        return null;

                    String payload = this.textBuffer.toString();
                    this.textBuffer.setLength(0);
                    ScamUpdate[] updates = parseUpdates(payload);

                    if (updates != null && updates.length > 0) {
                        applyUpdates(updates);
                        lastUpdatedEpochSecond = Instant.now().getEpochSecond();
                        saveCache();
                    }
                } catch (final Exception exception) {
                    this.textBuffer.setLength(0);
                    Constants.LOGGER.error("Failed to process scam-domain websocket payload!", exception);
                } finally {
                    webSocket.request(1);
                }

                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                this.textBuffer.setLength(0);
                if (!activeWebSocket.compareAndSet(webSocket, null))
                    return null;

                cancelPing();
                if (statusCode == 1000) {
                    Constants.LOGGER.info("Scam-domain websocket closed normally: {} - {}", statusCode, reason);
                } else if (statusCode != 1006) {
                    Constants.LOGGER.warn("Scam-domain websocket closed unexpectedly: {} - {}", statusCode, reason);
                }
                scheduleReconnect();
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                this.textBuffer.setLength(0);
                if (!activeWebSocket.compareAndSet(webSocket, null))
                    return;

                cancelPing();
                logWebSocketFailure("Scam-domain websocket disconnected", error);
                scheduleReconnect();
            }
        };
    }

    private void scheduleReconnect() {
        if (!this.reconnectScheduled.compareAndSet(false, true))
            return;

        final int attempt = this.consecutiveWebSocketFailures.incrementAndGet();
        final long exponentialDelaySeconds = WEBSOCKET_RECONNECT_BASE_DELAY.getSeconds()
            * (1L << Math.min(attempt - 1, 6)); // cap exponent to avoid overflow
        final long delaySeconds = Math.min(exponentialDelaySeconds, WEBSOCKET_RECONNECT_MAX_DELAY.getSeconds());
        this.scheduler.schedule(() -> {
            this.reconnectScheduled.set(false);
            startWebSocket();
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private static void logWebSocketFailure(String message, Throwable error) {
        final Throwable cause = unwrapCompletionException(error);
        if (cause instanceof SocketException) {
            Constants.LOGGER.warn("{}: {}. Reconnecting.", message, cause.getMessage());
        } else {
            Constants.LOGGER.error(message + ". Reconnecting.", cause);
        }
    }

    private static Throwable unwrapCompletionException(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
            && cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause;
    }

    private void ensureCacheDirectory() {
        try {
            final Path parent = CACHE_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (final IOException exception) {
            Constants.LOGGER.warn("Failed to create cache directory for scam domains.", exception);
        }
    }

    private void loadCache() {
        if (!Files.exists(CACHE_FILE))
            return;

        try (Reader reader = Files.newBufferedReader(CACHE_FILE, StandardCharsets.UTF_8)) {
            final CachePayload payload = Constants.GSON.fromJson(reader, CachePayload.class);
            if (payload == null)
                return;

            this.scamDomains.clear();
            if (payload.domains() != null) {
                payload.domains().stream()
                    .map(this::normalizeDomain)
                    .filter(Objects::nonNull)
                    .forEach(this.scamDomains::add);
            }

            this.lastUpdatedEpochSecond = payload.lastUpdatedEpochSecond();
            Constants.LOGGER.info("Loaded {} scam domains from cache.", this.scamDomains.size());
        } catch (final IOException exception) {
            Constants.LOGGER.warn("Failed to load scam-domain cache.", exception);
        }
    }

    private void saveCache() {
        final var payload = new CachePayload(this.lastUpdatedEpochSecond, this.scamDomains);
        try (Writer writer = Files.newBufferedWriter(CACHE_FILE, StandardCharsets.UTF_8)) {
            Constants.GSON.toJson(payload, writer);
        } catch (final IOException exception) {
            Constants.LOGGER.warn("Failed to write scam-domain cache.", exception);
        }
    }

    private void startPing(WebSocket webSocket) {
        cancelPing();

        this.pingTask = this.scheduler.scheduleAtFixedRate(() -> {
            webSocket.sendPing(ByteBuffer.wrap(new byte[]{1})).exceptionally(error -> {
                if (activeWebSocket.compareAndSet(webSocket, null)) {
                    Constants.LOGGER.debug("Failed to send scam-domain websocket ping, reconnecting.", error);
                    cancelPing();
                    webSocket.abort();
                    scheduleReconnect();
                }
                return null;
            });
        }, WEBSOCKET_PING_INTERVAL.getSeconds(), WEBSOCKET_PING_INTERVAL.getSeconds(), TimeUnit.SECONDS);
    }

    private void cancelPing() {
        final ScheduledFuture<?> existing = this.pingTask;
        if (existing != null) {
            existing.cancel(false);
        }

        this.pingTask = null;
    }

    private boolean containsScamDomain(String content) {
        final Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            final String url = matcher.group(1);
            final String host = extractHost(url);
            if (isScamDomainHost(host))
                return true;
        }

        return false;
    }

    private String extractHost(String url) {
        try {
            final String normalized = url.startsWith("http") ? url : "https://" + url;
            final URI uri = URI.create(normalized);
            return uri.getHost();
        } catch (final Exception _) {
            return null;
        }
    }

    private boolean isScamDomainHost(String host) {
        if (host == null)
            return false;

        String normalized = host.toLowerCase(Locale.ROOT);
        if (this.scamDomains.contains(normalized))
            return true;

        int dotIndex = normalized.indexOf('.');
        while (dotIndex > 0) {
            normalized = normalized.substring(dotIndex + 1);
            if (this.scamDomains.contains(normalized))
                return true;

            dotIndex = normalized.indexOf('.');
        }

        return false;
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank())
            return null;

        return domain.trim().toLowerCase(Locale.ROOT);
    }

    private record CachePayload(long lastUpdatedEpochSecond, Set<String> domains) {
    }

    private record ScamUpdate(String type, List<String> domains) {
    }
}
