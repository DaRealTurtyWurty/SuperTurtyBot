package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConfig {
    private long user;

    // Might need this in the future? Discord doesn't want to state how it works.
    // private boolean canStoreUserData;

    private List<Long> optInChannels = new ArrayList<>();
    private String leaderboardColor = toHex(Color.PINK);
    private LevelUpMessageType levelUpMessageType = LevelUpMessageType.EMBED;
    private TaxMessageType taxMessageType = TaxMessageType.ON;

    public UserConfig(long userId) {
        this.user = userId;
    }

    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static final Map<String, String> COLOR_HEX_MAP = new HashMap<>();

    static {
        COLOR_HEX_MAP.put("Black", "#000000");
        COLOR_HEX_MAP.put("Dark Gray", "#A9A9A9");
        COLOR_HEX_MAP.put("Gray", "#808080");
        COLOR_HEX_MAP.put("Light Gray", "#D3D3D3");
        COLOR_HEX_MAP.put("White", "#FFFFFF");
        COLOR_HEX_MAP.put("Red", "#FF0000");
        COLOR_HEX_MAP.put("Pink", "#FFC0CB");
        COLOR_HEX_MAP.put("Orange", "#FFA500");
        COLOR_HEX_MAP.put("Yellow", "#FFFF00");
        COLOR_HEX_MAP.put("Green", "#008000");
        COLOR_HEX_MAP.put("Magenta", "#FF00FF");
        COLOR_HEX_MAP.put("Cyan", "#00FFFF");
        COLOR_HEX_MAP.put("Blue", "#0000FF");
    }

    public enum LevelUpMessageType {
        EMBED, NORMAL, DM, NONE
    }

    public enum TaxMessageType {
        ON, SILENT, OFF
    }
}
