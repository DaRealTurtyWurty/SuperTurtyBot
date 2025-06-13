package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;

public record JavaVersion(String component, int majorVersion) {
    public static JavaVersion fromJson(JsonObject json) {
        return Constants.GSON.fromJson(json, JavaVersion.class);
    }
}