package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConfig {
    private long guild;
    private long user;

    // Might need this in the future? Discord doesn't want to state how it works.
    // private boolean canStoreUserData;

    private List<Long> optInChannels;
    private String leaderboardColor;

    public UserConfig(long guildId, long userId, String leaderboardColor) {
        this(guildId, userId, new ArrayList<>(), leaderboardColor);
    }

    public UserConfig(long guildId, long userId) {
        this(guildId, userId, toHex(Color.PINK));
    }

    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
