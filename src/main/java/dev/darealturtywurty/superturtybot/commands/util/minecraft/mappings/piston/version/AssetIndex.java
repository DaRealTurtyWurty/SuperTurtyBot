package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;

public record AssetIndex(String id, String sha1, int size, int totalSize, String url) {
    public static AssetIndex fromJson(JsonObject json) {
        return Constants.GSON.fromJson(json, AssetIndex.class);
    }
}
