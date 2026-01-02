package dev.darealturtywurty.superturtybot.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EmojiReader {
    @Getter
    private static Path emojisPath = Path.of("emojis.json");
    private static final Map<String, Long> EMOJIS = new HashMap<>();

    public static void setEmojisPath(@NotNull Path path) {
        if (path == null)
            throw new IllegalArgumentException("Emoji path cannot be null!");

        if(Files.notExists(path))
            throw new IllegalArgumentException("Emoji path does not exist! " + path.toAbsolutePath());

        if(!path.toString().endsWith(".json"))
            throw new IllegalArgumentException("Emoji path must be a JSON file!");

        emojisPath = path;
    }

    public static Map<String, Long> getEmojis() {
        JsonObject json;
        if (EMOJIS.isEmpty()) {
            try {
                String emojisJsonContent = Files.readString(emojisPath);
                JsonObject fullJson = Constants.GSON.fromJson(emojisJsonContent, JsonObject.class);
                json = fullJson.getAsJsonObject(Environment.INSTANCE.isDevelopment() ? "dev" : "prod");
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to read emojis from file!", exception);
                return Map.of();
            }
        } else {
            return EMOJIS;
        }

        Map<String, Long> emojis = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            emojis.put(entry.getKey(), entry.getValue().getAsLong());
        }

        EMOJIS.putAll(emojis);

        return emojis;
    }

    public static long getEmoji(String name) {
        return getEmojis().get(name);
    }
}
