package io.github.darealturtywurty.superturtybot.commands.core.config;

import java.util.List;
import java.util.function.BiPredicate;

import io.github.darealturtywurty.superturtybot.commands.core.config.ServerConfigOption.DataType;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import io.github.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ServerConfigRegistry {
    // TODO: Utility class
    private static final BiPredicate<SlashCommandInteractionEvent, String> TEXT_CHANNEL_VALIDATOR = (event, str) -> {
        final Guild guild = event.getGuild();
        TextChannel channel = guild.getTextChannelById(str);
        if (channel != null)
            return true;
        
        if (MentionType.CHANNEL.getPattern().matcher(str).matches()) {
            final String id = str.replace("<#", "").replace(">", "");
            channel = guild.getTextChannelById(id);
            return channel != null;
        }
        
        List<TextChannel> possibleMatches = guild.getTextChannelsByName(str, false);
        if (possibleMatches.isEmpty()) {
            possibleMatches = guild.getTextChannelsByName(str, true);
            if (possibleMatches.isEmpty())
                return false;
        }
        
        return true;
    };
    
    public static final Registry<ServerConfigOption> SERVER_CONFIG_OPTIONS = new Registry<>();

    public static final ServerConfigOption STARBOARD = SERVER_CONFIG_OPTIONS.register("starboard",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setStarboard(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getStarboard).validator(TEXT_CHANNEL_VALIDATOR).build());

    public static final ServerConfigOption IS_STARBOARD_ENABLED = SERVER_CONFIG_OPTIONS.register("starboard_enabled",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setStarboardEnabled(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isStarboardEnabled).build());

    public static final ServerConfigOption MINIMUM_STARS = SERVER_CONFIG_OPTIONS.register("minimum_stars",
        new ServerConfigOption.Builder().dataType(DataType.INTEGER)
            .serializer((config, value) -> config.setMinimumStars(Integer.parseInt(value)))
            .valueFromConfig(GuildConfig::getMinimumStars).validator((event, str) -> {
                final int input = Integer.parseInt(str);
                return input >= 1 && input <= event.getGuild().getMemberCount();
            }).build());

    public static final ServerConfigOption DO_BOT_STARS_COUNT = SERVER_CONFIG_OPTIONS.register("bot_stars_count",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setBotStarsCount(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isBotStarsCount).build());

    public static final ServerConfigOption MOD_LOGGING = SERVER_CONFIG_OPTIONS.register("mod_logging",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setModLogging(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getModLogging).validator(TEXT_CHANNEL_VALIDATOR).build());
}
