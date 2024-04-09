package dev.darealturtywurty.superturtybot.commands.util.roblox;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

import java.util.List;

@Data
public class RobloxUsernameInfo {
    private List<RobloxPlayerData> data;

    public static RobloxUsernameInfo fromJsonString(String json) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object, RobloxUsernameInfo.class);
    }
}