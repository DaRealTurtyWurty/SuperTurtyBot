package dev.darealturtywurty.superturtybot.commands.core.config;

import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import dev.darealturtywurty.superturtybot.registry.Registry;

import java.awt.*;

public class UserConfigRegistry {
    public static final Registry<UserConfigOption> USER_CONFIG_OPTIONS = new Registry<>();

    public static final UserConfigOption LEADERBOARD_COLOR = USER_CONFIG_OPTIONS.register("leaderboard_color",
            new UserConfigOption.Builder()
                    .dataType(UserConfigOption.DataType.COLOR)
                    .serializer((config, value) -> config.setLeaderboardColor(UserConfig.toHex(Color.decode(value))))
                    .valueFromConfig(UserConfig::getLeaderboardColor)
                    .build());
}
