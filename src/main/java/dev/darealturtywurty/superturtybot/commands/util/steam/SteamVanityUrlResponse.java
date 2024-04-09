package dev.darealturtywurty.superturtybot.commands.util.steam;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

@Data
public class SteamVanityUrlResponse {
    private String steamid;
    private int success;

    public static SteamVanityUrlResponse fromJsonString(String json) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object.get("response"), SteamVanityUrlResponse.class);
    }
}