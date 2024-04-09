package dev.darealturtywurty.superturtybot.commands.util.roblox;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

import java.util.List;

@Data
public class RobloxPlayerProfile {
    private Scale scale;
    private String playerAvatarType;
    private BodyColors bodyColors;
    private List<Asset> assets;
    private boolean defaultShirtApplied;
    private boolean defaultPantsApplied;
    private List<Emote> emotes;

    public static RobloxPlayerProfile fromJsonString(String json) {
        JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
        return Constants.GSON.fromJson(object, RobloxPlayerProfile.class);
    }

    @Data
    public static class Scale {
        private double height;
        private double width;
        private double head;
        private double depth;
        private double proportion;
        private double bodyType;
    }

    @Data
    public static class BodyColors {
        private int headColorId;
        private int torsoColorId;
        private int rightArmColorId;
        private int leftArmColorId;
        private int rightLegColorId;
        private int leftLegColorId;
    }

    @Data
    public static class Asset {
        private long id;
        private String name;
        private AssetType assetType;
        private long currentVersionId;
        private Meta meta;
    }

    @Data
    public static class AssetType {
        private long id;
        private String name;
    }

    @Data
    public static class Meta {
        private int order;
        private int version;
    }

    @Data
    public static class Emote {
        private long assetId;
        private String assetName;
        private int position;
    }
}