package dev.darealturtywurty.superturtybot.commands.core.config;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import dev.darealturtywurty.superturtybot.commands.core.config.ServerConfigOption.DataType;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class ServerConfigRegistry {
    public static final Registry<ServerConfigOption> SERVER_CONFIG_OPTIONS = new Registry<>();
    
    public static final ServerConfigOption STARBOARD = SERVER_CONFIG_OPTIONS.register("starboard",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setStarboard(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getStarboard).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());
    
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
            .valueFromConfig(GuildConfig::getModLogging).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());
    
    public static final ServerConfigOption LEVEL_ROLES = SERVER_CONFIG_OPTIONS.register("level_roles",
        new ServerConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setLevelRoles)
            .valueFromConfig(GuildConfig::getLevelRoles).validator((event, value) -> {
                final String[] split = value.split("[\s;]");
                for (final String val : split) {
                    final String[] roleToChannel = val.split("->");
                    if (roleToChannel.length != 2 || Ints.tryParse(roleToChannel[0].trim()) == null)
                        return false;
                    final Long roleId = Longs.tryParse(roleToChannel[1].trim());
                    if (roleId == null)
                        return false;
                    final Role role = event.getGuild().getRoleById(roleId);
                    if (role == null)
                        return false;
                }
                return true;
            }).build());
    
    public static final ServerConfigOption LEVELLING_COOLDOWN = SERVER_CONFIG_OPTIONS.register("level_cooldown",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setLevelCooldown(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getLevelCooldown).validator((event, value) -> Long.parseLong(value) > 0)
            .build());
    
    public static final ServerConfigOption MINIMUM_XP = SERVER_CONFIG_OPTIONS.register("min_xp",
        new ServerConfigOption.Builder().dataType(DataType.INTEGER)
            .serializer((config, value) -> config.setMinXP(Integer.parseInt(value)))
            .valueFromConfig(GuildConfig::getMinXP)
            .validator((event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());
    
    public static final ServerConfigOption MAXIMUM_XP = SERVER_CONFIG_OPTIONS.register("max_xp",
        new ServerConfigOption.Builder().dataType(DataType.INTEGER)
            .serializer((config, value) -> config.setMaxXP(Integer.parseInt(value)))
            .valueFromConfig(GuildConfig::getMaxXP)
            .validator((event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());
    
    public static final ServerConfigOption LEVELLING_ITEM_CHANCE = SERVER_CONFIG_OPTIONS.register(
        "levelling_item_chance",
        new ServerConfigOption.Builder().dataType(DataType.INTEGER)
            .serializer((config, value) -> config.setLevellingItemChance(Integer.parseInt(value)))
            .valueFromConfig(GuildConfig::getLevellingItemChance)
            .validator((event, value) -> Integer.parseInt(value) >= 0 && Integer.parseInt(value) <= 100).build());
    
    public static final ServerConfigOption LEVELLING_ENABLED = SERVER_CONFIG_OPTIONS.register("levelling_enabled",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setLevellingEnabled(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isLevellingEnabled).build());
    
    public static final ServerConfigOption DISABLED_LEVELLING_CHANNELS = SERVER_CONFIG_OPTIONS
        .register("disabled_levelling_channels",
            new ServerConfigOption.Builder().dataType(DataType.STRING)
                .serializer(GuildConfig::setDisabledLevellingChannels)
                .valueFromConfig(GuildConfig::getDisabledLevellingChannels).validator((event, value) -> {
                    final String[] channels = value.split("[\s;]");
                    for (final String channelStr : channels) {
                        if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                            return false;
                    }
                    
                    return true;
                }).build());
    
    public static final ServerConfigOption SHOWCASE_CHANNELS = SERVER_CONFIG_OPTIONS.register("showcase_channels",
        new ServerConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setShowcaseChannels)
            .valueFromConfig(GuildConfig::getShowcaseChannels).validator((event, value) -> {
                final String[] channels = value.split("[\s;]");
                for (final String channelStr : channels) {
                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                        return false;
                }
                
                return true;
            }).build());
    
    public static final ServerConfigOption IS_STARBOARD_MEDIA = SERVER_CONFIG_OPTIONS.register(
        "is_starboard_media_only",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setStarboardMediaOnly(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isStarboardMediaOnly).build());

    public static final ServerConfigOption STAR_EMOJI = SERVER_CONFIG_OPTIONS.register("star_emoji",
        new ServerConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setStarEmoji)
            .valueFromConfig(GuildConfig::getStarEmoji)
            .validator((event, value) -> (MentionType.EMOJI.getPattern().matcher(value).matches()
                || Emoji.fromUnicode(value) != null || Emoji.fromFormatted(value) != null))
            .build());

    public static final ServerConfigOption SUGGESTIONS = SERVER_CONFIG_OPTIONS.register("suggestions",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setSuggestions(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getSuggestions).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());
    
    public static final ServerConfigOption DISABLE_LEVEL_UP_MESSAGES = SERVER_CONFIG_OPTIONS.register(
        "disable_level_up_messages",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setDisableLevelUpMessages(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isDisableLevelUpMessages).build());
    
    public static final ServerConfigOption HAS_LEVEL_UP_CHANNEL = SERVER_CONFIG_OPTIONS.register("has_level_up_channel",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setHasLevelUpChannel(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isHasLevelUpChannel).build());

    public static final ServerConfigOption LEVEL_UP_MESSAGE_CHANNEL = SERVER_CONFIG_OPTIONS.register(
        "level_up_message_channel",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setLevelUpMessageChannel(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getLevelUpMessageChannel).validator(Validators.TEXT_CHANNEL_VALIDATOR)
            .build());
    
    public static final ServerConfigOption SHOULD_EMBED_LEVEL_UP_MESSAGES = SERVER_CONFIG_OPTIONS.register(
        "should_embed_level_up_message",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setShouldEmbedLevelUpMessage(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isShouldEmbedLevelUpMessage).build());

    public static final ServerConfigOption SHOULD_MODERATORS_JOIN_THREADS = SERVER_CONFIG_OPTIONS.register(
        "should_moderators_join_threads",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setShouldModeratorsJoinThreads(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isShouldModeratorsJoinThreads).build());

    public static final ServerConfigOption AUTO_THREAD_CHANNELS = SERVER_CONFIG_OPTIONS.register("auto_thread_channels",
        new ServerConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setAutoThreadChannels)
            .valueFromConfig(GuildConfig::getAutoThreadChannels).validator((event, value) -> {
                final String[] channels = value.split("[\s;]");
                for (final String channelStr : channels) {
                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                        return false;
                }
                
                return true;
            }).build());
    
    public static final ServerConfigOption SHOULD_CREATE_GISTS = SERVER_CONFIG_OPTIONS.register("should_create_gists",
        new ServerConfigOption.Builder().dataType(DataType.BOOLEAN)
            .serializer((config, value) -> config.setShouldCreateGists(Boolean.parseBoolean(value)))
            .valueFromConfig(GuildConfig::isShouldCreateGists).build());

    public static final ServerConfigOption LOGGING_CHANNEL = SERVER_CONFIG_OPTIONS.register("logging_channel",
        new ServerConfigOption.Builder().dataType(DataType.LONG)
            .serializer((config, value) -> config.setLoggingChannel(Long.parseLong(value)))
            .valueFromConfig(GuildConfig::getLoggingChannel).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());
}
