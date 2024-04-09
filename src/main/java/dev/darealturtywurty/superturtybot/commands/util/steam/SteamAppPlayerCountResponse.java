package dev.darealturtywurty.superturtybot.commands.util.steam;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

@Data
public class SteamAppPlayerCountResponse {
    private int player_count;
    private int result;

    public static SteamAppPlayerCountResponse fromJsonString(String json) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object.get("response"), SteamAppPlayerCountResponse.class);
    }
}