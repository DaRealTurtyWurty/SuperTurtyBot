package dev.darealturtywurty.superturtybot.weblisteners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.AutoModerator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;

public final class ScamSocket extends WebSocketListener {
    private static final WebSocket SOCKET = new OkHttpClient().newWebSocket(
        new Request.Builder().url("wss://phish.sinking.yachts/feed").addHeader("X-Identity", "TurtyBot#8108").build(),
        new ScamSocket());
    
    private ScamSocket() {
        ShutdownHooks.register(() -> SOCKET.close(1001, "Bot shutting down"));
    }
    
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        final JsonObject response = Constants.GSON.fromJson(text, JsonObject.class);
        final String type = response.has("type") ? response.get("type").getAsString() : "none";
        if ("none".equals(type))
            return;
        
        final JsonArray domains = response.has("domains") ? response.getAsJsonArray("domains") : new JsonArray();
        for (final JsonElement elem : domains) {
            final String domain = elem.getAsString();
            if ("add".equals(type)) {
                AutoModerator.SCAM_DOMAINS.add(domain);
                Constants.LOGGER.debug("Scam link added: {}", domain);
            } else if ("remove".equals(type)) {
                AutoModerator.SCAM_DOMAINS.remove(domain);
                Constants.LOGGER.debug("Scam link removed: {}", domain);
            } else {
                Constants.LOGGER.warn("Unknown scam message type found! Type: \"{}\" for domain: {}", type, domain);
            }
        }
    }
}
