package dev.darealturtywurty.superturtybot.commands.util.steam;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

@Data
public class SteamAppDetailsResponse {
    private boolean success;
    private SteamAppDetailsData data;

    public static SteamAppDetailsResponse fromJsonString(String json, String appid) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object.get(appid), SteamAppDetailsResponse.class);
    }
}