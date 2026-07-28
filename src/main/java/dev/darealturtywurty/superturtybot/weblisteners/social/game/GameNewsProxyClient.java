package dev.darealturtywurty.superturtybot.weblisteners.social.game;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class GameNewsProxyClient {
    private static final String PROXY_LIST_URL =
            "https://cdn.jsdelivr.net/gh/proxyscrape/free-proxy-list@main/proxies/protocols/https/data.json";
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(10);
    private static final Duration FAILED_REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final int MAX_PROXY_ATTEMPTS = 3;
    private static final double MIN_UPTIME_PERCENT = 80.0;
    private static final double MAX_LATENCY_MILLIS = 1_500.0;
    private static final Object REFRESH_LOCK = new Object();

    private static volatile List<ProxyEndpoint> proxyEndpoints = List.of();
    private static volatile long nextRefreshAt;

    private GameNewsProxyClient() {
    }

    public static Response execute(Request request, String sourceName) throws IOException {
        if (!Environment.INSTANCE.gameNewsProxiesEnabled().orElse(false) || !request.url().isHttps())
            return Constants.HTTP_CLIENT.newCall(request).execute();

        List<ProxyEndpoint> candidates = new ArrayList<>(getProxyEndpoints());
        Collections.shuffle(candidates, ThreadLocalRandom.current());

        IOException lastProxyFailure = null;
        int attempts = Math.min(MAX_PROXY_ATTEMPTS, candidates.size());
        for (int index = 0; index < attempts; index++) {
            ProxyEndpoint endpoint = candidates.get(index);
            try {
                Response response = clientFor(endpoint).newCall(request).execute();
                if (!shouldRetryWithAnotherProxy(response.code()))
                    return response;

                lastProxyFailure = new IOException(
                        "Proxy %s returned HTTP %d".formatted(endpoint, response.code()));
                response.close();
            } catch (IOException exception) {
                lastProxyFailure = exception;
            }
        }

        if (attempts > 0) {
            Constants.LOGGER.debug("All {} sampled proxies failed for {}; falling back to a direct request.",
                    attempts, sourceName);
        }

        try {
            return Constants.HTTP_CLIENT.newCall(request).execute();
        } catch (IOException directFailure) {
            if (lastProxyFailure != null) {
                directFailure.addSuppressed(lastProxyFailure);
            }

            throw directFailure;
        }
    }

    private static List<ProxyEndpoint> getProxyEndpoints() {
        long now = System.currentTimeMillis();
        if (now < nextRefreshAt)
            return proxyEndpoints;

        synchronized (REFRESH_LOCK) {
            now = System.currentTimeMillis();
            if (now < nextRefreshAt)
                return proxyEndpoints;

            boolean refreshed = refreshProxyEndpoints();
            Duration nextInterval = refreshed ? REFRESH_INTERVAL : FAILED_REFRESH_INTERVAL;
            nextRefreshAt = now + nextInterval.toMillis();
            return proxyEndpoints;
        }
    }

    private static boolean refreshProxyEndpoints() {
        Request request = new Request.Builder()
                .url(PROXY_LIST_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "SuperTurtyBot/1.0")
                .build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Constants.LOGGER.warn("Failed to refresh the game-news proxy list. HTTP {}", response.code());
                return false;
            }

            ResponseBody body = response.body();
            if (body == null) {
                Constants.LOGGER.warn("Failed to refresh the game-news proxy list: empty response body.");
                return false;
            }

            JsonArray entries = Constants.GSON.fromJson(body.charStream(), JsonArray.class);
            List<ProxyEndpoint> allValidEndpoints = new ArrayList<>();
            List<ProxyEndpoint> preferredEndpoints = new ArrayList<>();
            if (entries != null) {
                for (JsonElement entry : entries) {
                    ProxyEndpoint endpoint = parseEndpoint(entry);
                    if (endpoint == null)
                        continue;

                    allValidEndpoints.add(endpoint);
                    JsonObject object = entry.getAsJsonObject();
                    if (isPreferred(object)) {
                        preferredEndpoints.add(endpoint);
                    }
                }
            }

            List<ProxyEndpoint> refreshedEndpoints =
                    preferredEndpoints.isEmpty() ? allValidEndpoints : preferredEndpoints;
            if (refreshedEndpoints.isEmpty()) {
                Constants.LOGGER.warn("The game-news proxy list contained no usable HTTP proxies.");
                return false;
            }

            proxyEndpoints = List.copyOf(refreshedEndpoints);
            return true;
        } catch (Exception exception) {
            Constants.LOGGER.warn("Failed to refresh the game-news proxy list.", exception);
            return false;
        }
    }

    private static ProxyEndpoint parseEndpoint(JsonElement entry) {
        if (entry == null || !entry.isJsonObject())
            return null;

        JsonObject object = entry.getAsJsonObject();
        String protocol = getString(object, "protocol");
        String host = getString(object, "ip");
        int port = getInt(object, "port");
        if (!"http".equals(protocol.toLowerCase(Locale.ROOT))
                || host.isBlank()
                || !isPublicAddress(host)
                || port < 1
                || port > 65_535)
            return null;

        return new ProxyEndpoint(host, port);
    }

    private static boolean isPublicAddress(String host) {
        final InetAddress address;
        try {
            address = InetAddresses.forString(host);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress())
            return false;

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first != 0
                    && first < 224
                    && !(first == 100 && second >= 64 && second <= 127);
        }

        if (address instanceof Inet6Address)
            return (Byte.toUnsignedInt(bytes[0]) & 0xFE) != 0xFC;

        return false;
    }

    private static boolean isPreferred(JsonObject object) {
        return getBoolean(object, "ssl")
                && getDouble(object, "uptime_percent", 0.0) >= MIN_UPTIME_PERCENT
                && getDouble(object, "latency_ms", Double.MAX_VALUE) <= MAX_LATENCY_MILLIS;
    }

    private static OkHttpClient clientFor(ProxyEndpoint endpoint) {
        var proxy = new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved(endpoint.host(), endpoint.port()));
        return Constants.HTTP_CLIENT.newBuilder()
                .proxy(proxy)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(40, TimeUnit.SECONDS)
                .build();
    }

    private static boolean shouldRetryWithAnotherProxy(int statusCode) {
        return statusCode == 403
                || statusCode == 407
                || statusCode == 408
                || statusCode == 425
                || statusCode == 429
                || statusCode >= 500;
    }

    private static String getString(JsonObject object, String property) {
        JsonElement value = object.get(property);
        return value == null || value.isJsonNull() ? "" : value.getAsString().trim();
    }

    private static int getInt(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || value.isJsonNull())
            return -1;

        try {
            return value.getAsInt();
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static boolean getBoolean(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || value.isJsonNull())
            return false;

        try {
            return value.getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static double getDouble(JsonObject object, String property, double fallback) {
        JsonElement value = object.get(property);
        if (value == null || value.isJsonNull())
            return fallback;

        try {
            return value.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private record ProxyEndpoint(String host, int port) {
        @Override
        public @NotNull String toString() {
            return host + ':' + port;
        }
    }
}
