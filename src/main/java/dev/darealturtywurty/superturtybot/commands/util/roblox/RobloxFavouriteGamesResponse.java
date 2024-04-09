package dev.darealturtywurty.superturtybot.commands.util.roblox;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

import java.util.List;

@Data
public class RobloxFavouriteGamesResponse {
    private List<RobloxFavouriteGameData> data;

    public static RobloxFavouriteGamesResponse fromJsonString(String json) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object, RobloxFavouriteGamesResponse.class);
    }
}