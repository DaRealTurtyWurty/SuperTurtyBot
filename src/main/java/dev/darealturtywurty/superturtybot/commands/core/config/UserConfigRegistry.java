package dev.darealturtywurty.superturtybot.commands.core.config;

import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import dev.darealturtywurty.superturtybot.registry.Registry;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserConfigRegistry {
    public static final Registry<UserConfigOption> USER_CONFIG_OPTIONS = new Registry<>();

    public static final UserConfigOption LEADERBOARD_COLOR = USER_CONFIG_OPTIONS.register("leaderboard_color",
            new UserConfigOption.Builder()
                    .dataType(UserConfigOption.DataType.COLOR)
                    .serializer((config, value) -> config.setLeaderboardColor(UserConfig.toHex(Color.decode(value))))
                    .valueFromConfig(UserConfig::getLeaderboardColor)
                    .autoCompleteColors()
                    .build());

    public static final UserConfigOption LEVEL_UP_MESSAGE_TYPE = USER_CONFIG_OPTIONS.register("level_up_message_type",
            new UserConfigOption.Builder()
                    .dataType(UserConfigOption.DataType.STRING)
                    .serializer((config, str) -> config.setLevelUpMessageType(UserConfig.LevelUpMessageType.valueOf(str.toUpperCase(Locale.ROOT))))
                    .validator((event, str) -> {
                        try {
                            UserConfig.LevelUpMessageType.valueOf(str.toUpperCase(Locale.ROOT));
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .valueFromConfig(config -> config.getLevelUpMessageType().name())
                    .enumAutoComplete(UserConfig.LevelUpMessageType.values())
                    .build());

    public static final UserConfigOption TAX_MESSAGE_TYPE = USER_CONFIG_OPTIONS.register("tax_message_type",
            new UserConfigOption.Builder()
                    .dataType(UserConfigOption.DataType.STRING)
                    .serializer((config, str) -> config.setTaxMessageType(UserConfig.TaxMessageType.valueOf(str.toUpperCase(Locale.ROOT))))
                    .validator((event, str) -> {
                        try {
                            UserConfig.TaxMessageType.valueOf(str.toUpperCase(Locale.ROOT));
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .valueFromConfig(config -> config.getTaxMessageType().name())
                    .enumAutoComplete(UserConfig.TaxMessageType.values())
                    .build());
}
