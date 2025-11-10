package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Message;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Responsible for loading, caching, and keeping scam-domain data in sync with phish.sinking.yachts.
 */
public class ScamDomainDetector {
    private static final String ALL_URL = "https://phish.sinking.yachts/v2/all";
    private static final String RECENT_URL_TEMPLATE = "https://phish.sinking.yachts/v2/recent/%d";
    private static final String WEBSOCKET_URL = "wss://phish.sinking.yachts/feed";
    private static final Duration CACHE_EXPIRY = Duration.ofSeconds(604800); // 7 days
    private static final Path CACHE_FILE = Path.of("cache", "scam_domains_cache.json");

    private final Set<String> scamDomains = ConcurrentHashMap.newKeySet();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "ScamDomainDetector-WebSocket");
        thread.setDaemon(true);
        return thread;
    });

    private volatile long lastUpdatedEpochSecond = 0L;

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

        final GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", message.getGuild().getIdLong())).first();
        if (config == null || !config.isScamDetectionEnabled())
            return;

        final String content = message.getContentRaw();
        if (content.isBlank())
            return;

        for (final String domain : scamDomains) {
            if (content.contains(domain) && (!content.contains("." + domain) && !content.contains("https://" + domain))) {
                message.delete().flatMap(success ->
                        message.getChannel().sendMessage(message.getAuthor().getAsMention() + ", do NOT send scam links! " +
                                "If this was not you, then your account has been compromised. " +
                                "Please make sure to reset your login details to get your token changed. " +
                                "In the future, be more careful what URLs you are opening, always check that it's the real one.")).queue();
                break;
            }
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
            final HttpRequest request = HttpRequest.newBuilder(URI.create(ALL_URL)).GET().build();
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
            Collections.addAll(this.scamDomains, responseDomains);
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
            final String url = RECENT_URL_TEMPLATE.formatted(this.lastUpdatedEpochSecond);
            final HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            final HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Constants.LOGGER.warn("Failed to fetch recent scam domains. Status: {}", response.statusCode());
                return;
            }

            final ScamUpdate[] updates = Constants.GSON.fromJson(response.body(), ScamUpdate[].class);
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
        } catch (final IOException exception) {
            Constants.LOGGER.error("Failed to fetch recent scam-domain updates!", exception);
        }
    }

    private void applyUpdates(ScamUpdate[] updates) {
        for (final ScamUpdate update : updates) {
            if (update == null || update.domains() == null)
                continue;

            if ("delete".equalsIgnoreCase(update.type())) {
                update.domains().forEach(this.scamDomains::remove);
            } else {
                this.scamDomains.addAll(update.domains());
            }
        }
    }

    private void startWebSocket() {
        this.httpClient.newWebSocketBuilder().buildAsync(URI.create(WEBSOCKET_URL), new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
                Constants.LOGGER.info("Connected to scam-domain websocket feed.");
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    ScamUpdate[] updates = Constants.GSON.fromJson(data.toString(), ScamUpdate[].class);
                    if ((updates == null || updates.length == 0)) {
                        final ScamUpdate single = Constants.GSON.fromJson(data.toString(), ScamUpdate.class);
                        if (single != null)
                            updates = new ScamUpdate[]{single};
                    }

                    if (updates != null && updates.length > 0) {
                        applyUpdates(updates);
                        lastUpdatedEpochSecond = Instant.now().getEpochSecond();
                        saveCache();
                    }
                } catch (final Exception exception) {
                    Constants.LOGGER.error("Failed to process scam-domain websocket payload!", exception);
                } finally {
                    webSocket.request(1);
                }

                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                Constants.LOGGER.warn("Scam-domain websocket closed: {} - {}", statusCode, reason);
                scheduleReconnect();
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                Constants.LOGGER.error("Scam-domain websocket error!", error);
                scheduleReconnect();
            }
        }).exceptionally(error -> {
            Constants.LOGGER.error("Failed to connect to scam-domain websocket feed!", error);
            scheduleReconnect();
            return null;
        });
    }

    private void scheduleReconnect() {
        this.scheduler.schedule(this::startWebSocket, 30, TimeUnit.SECONDS);
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
            if (payload.domains() != null)
                this.scamDomains.addAll(payload.domains());

            this.lastUpdatedEpochSecond = payload.lastUpdatedEpochSecond();
            Constants.LOGGER.info("Loaded {} scam domains from cache.", this.scamDomains.size());
        } catch (final IOException exception) {
            Constants.LOGGER.warn("Failed to load scam-domain cache.", exception);
        }
    }

    private void saveCache() {
        final CachePayload payload = new CachePayload(this.lastUpdatedEpochSecond, this.scamDomains);
        try (Writer writer = Files.newBufferedWriter(CACHE_FILE, StandardCharsets.UTF_8)) {
            Constants.GSON.toJson(payload, writer);
        } catch (final IOException exception) {
            Constants.LOGGER.warn("Failed to write scam-domain cache.", exception);
        }
    }

    private record CachePayload(long lastUpdatedEpochSecond, Set<String> domains) {
    }

    private record ScamUpdate(String type, List<String> domains) {
    }
}
