package dev.darealturtywurty.superturtybot.commands.core.config;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigOption.DataType;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class GuildConfigRegistry {
    public static final Registry<GuildConfigOption> GUILD_CONFIG_OPTIONS = new Registry<>();

    public static final GuildConfigOption STARBOARD = GUILD_CONFIG_OPTIONS.register("starboard",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                                            .serializer((config, value) -> config.setStarboard(Long.parseLong(value)))
                                            .valueFromConfig(GuildConfig::getStarboard)
                                            .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    public static final GuildConfigOption IS_STARBOARD_ENABLED = GUILD_CONFIG_OPTIONS.register("starboard_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                    (config, value) -> config.setStarboardEnabled(Boolean.parseBoolean(value)))
                                            .valueFromConfig(GuildConfig::isStarboardEnabled).build());

    public static final GuildConfigOption MINIMUM_STARS = GUILD_CONFIG_OPTIONS.register("minimum_stars",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                                                    (config, value) -> config.setMinimumStars(Integer.parseInt(value)))
                                            .valueFromConfig(GuildConfig::getMinimumStars).validator((event, str) -> {
                                                final int input = Integer.parseInt(str);
                                                return input >= 1 && input <= event.getGuild().getMemberCount();
                                            }).build());

    public static final GuildConfigOption DO_BOT_STARS_COUNT = GUILD_CONFIG_OPTIONS.register("bot_stars_count",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                    (config, value) -> config.setBotStarsCount(Boolean.parseBoolean(value)))
                                            .valueFromConfig(GuildConfig::isBotStarsCount).build());

    public static final GuildConfigOption MOD_LOGGING = GUILD_CONFIG_OPTIONS.register("mod_logging",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                                            .serializer((config, value) -> config.setModLogging(Long.parseLong(value)))
                                            .valueFromConfig(GuildConfig::getModLogging)
                                            .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    public static final GuildConfigOption LEVEL_ROLES = GUILD_CONFIG_OPTIONS.register("level_roles",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setLevelRoles)
                                            .valueFromConfig(GuildConfig::getLevelRoles).validator((event, value) -> {
                                                final String[] split = value.split("[\s;]");
                                                for (final String val : split) {
                                                    final String[] roleToChannel = val.split("->");
                                                    if (roleToChannel.length != 2 || Ints.tryParse(roleToChannel[0].trim()) == null) return false;
                                                    final Long roleId = Longs.tryParse(roleToChannel[1].trim());
                                                    if (roleId == null) return false;
                                                    final Role role = event.getGuild().getRoleById(roleId);
                                                    if (role == null) return false;
                                                }
                                                return true;
                                            }).build());

    public static final GuildConfigOption LEVELLING_COOLDOWN = GUILD_CONFIG_OPTIONS.register("level_cooldown",
            new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                                                    (config, value) -> config.setLevelCooldown(Long.parseLong(value)))
                                            .valueFromConfig(GuildConfig::getLevelCooldown)
                                            .validator((event, value) -> Long.parseLong(value) > 0).build());

    public static final GuildConfigOption MINIMUM_XP = GUILD_CONFIG_OPTIONS.register("min_xp",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                                            .serializer((config, value) -> config.setMinXP(Integer.parseInt(value)))
                                            .valueFromConfig(GuildConfig::getMinXP).validator(
                                                    (event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());

    public static final GuildConfigOption MAXIMUM_XP = GUILD_CONFIG_OPTIONS.register("max_xp",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                                            .serializer((config, value) -> config.setMaxXP(Integer.parseInt(value)))
                                            .valueFromConfig(GuildConfig::getMaxXP).validator(
                                                    (event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());

    public static final GuildConfigOption LEVELLING_ITEM_CHANCE = GUILD_CONFIG_OPTIONS.register(
            "levelling_item_chance", new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                    (config, value) -> config.setLevellingItemChance(Integer.parseInt(value))).valueFromConfig(
                    GuildConfig::getLevellingItemChance).validator(
                    (event, value) -> Integer.parseInt(value) >= 0 && Integer.parseInt(value) <= 100).build());

    public static final GuildConfigOption LEVELLING_ENABLED = GUILD_CONFIG_OPTIONS.register("levelling_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                    (config, value) -> config.setLevellingEnabled(Boolean.parseBoolean(value)))
                                            .valueFromConfig(GuildConfig::isLevellingEnabled).build());

    public static final GuildConfigOption DISABLED_LEVELLING_CHANNELS = GUILD_CONFIG_OPTIONS.register(
            "disabled_levelling_channels", new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(
                    GuildConfig::setDisabledLevellingChannels).valueFromConfig(
                    GuildConfig::getDisabledLevellingChannels).validator((event, value) -> {
                final String[] channels = value.split("[\s;]");
                for (final String channelStr : channels) {
                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr)) return false;
                }

                return true;
            }).build());

    public static final GuildConfigOption SHOWCASE_CHANNELS = GUILD_CONFIG_OPTIONS.register("showcase_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setShowcaseChannels)
                                            .valueFromConfig(GuildConfig::getShowcaseChannels)
                                            .validator((event, value) -> {
                                                final String[] channels = value.split("[\s;]");
                                                for (final String channelStr : channels) {
                                                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                                        return false;
                                                }

                                                return true;
                                            }).build());

    public static final GuildConfigOption IS_STARBOARD_MEDIA = GUILD_CONFIG_OPTIONS.register(
            "is_starboard_media_only", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setStarboardMediaOnly(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildConfig::isStarboardMediaOnly).build());

    public static final GuildConfigOption STAR_EMOJI = GUILD_CONFIG_OPTIONS.register("star_emoji",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setStarEmoji)
                                            .valueFromConfig(GuildConfig::getStarEmoji).validator(
                                                    (event, value) -> (MentionType.EMOJI.getPattern().matcher(value).matches() || Emoji.fromUnicode(
                                                            value) != null || Emoji.fromFormatted(value) != null)).build());

    public static final GuildConfigOption SUGGESTIONS = GUILD_CONFIG_OPTIONS.register("suggestions",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                                            .serializer((config, value) -> config.setSuggestions(Long.parseLong(value)))
                                            .valueFromConfig(GuildConfig::getSuggestions)
                                            .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    public static final GuildConfigOption DISABLE_LEVEL_UP_MESSAGES = GUILD_CONFIG_OPTIONS.register(
            "disable_level_up_messages", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setDisableLevelUpMessages(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildConfig::isDisableLevelUpMessages).build());

    public static final GuildConfigOption HAS_LEVEL_UP_CHANNEL = GUILD_CONFIG_OPTIONS.register("has_level_up_channel",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                    (config, value) -> config.setHasLevelUpChannel(Boolean.parseBoolean(value)))
                                            .valueFromConfig(GuildConfig::isHasLevelUpChannel).build());

    public static final GuildConfigOption LEVEL_UP_MESSAGE_CHANNEL = GUILD_CONFIG_OPTIONS.register(
            "level_up_message_channel", new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                    (config, value) -> config.setLevelUpMessageChannel(Long.parseLong(value))).valueFromConfig(
                    GuildConfig::getLevelUpMessageChannel).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    public static final GuildConfigOption SHOULD_EMBED_LEVEL_UP_MESSAGES = GUILD_CONFIG_OPTIONS.register(
            "should_embed_level_up_message", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                                                     (config, value) -> config.setShouldEmbedLevelUpMessage(Boolean.parseBoolean(value)))
                                                                             .valueFromConfig(
                                                                                     GuildConfig::isShouldEmbedLevelUpMessage)
                                                                             .build());

    public static final GuildConfigOption SHOULD_MODERATORS_JOIN_THREADS = GUILD_CONFIG_OPTIONS.register(
            "should_moderators_join_threads", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                                                      (config, value) -> config.setShouldModeratorsJoinThreads(Boolean.parseBoolean(value)))
                                                                              .valueFromConfig(
                                                                                      GuildConfig::isShouldModeratorsJoinThreads)
                                                                              .build());

    public static final GuildConfigOption AUTO_THREAD_CHANNELS = GUILD_CONFIG_OPTIONS.register("auto_thread_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setAutoThreadChannels)
                                            .valueFromConfig(GuildConfig::getAutoThreadChannels)
                                            .validator((event, value) -> {
                                                final String[] channels = value.split("[\s;]");
                                                for (final String channelStr : channels) {
                                                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                                        return false;
                                                }

                                                return true;
                                            }).build());

    public static final GuildConfigOption SHOULD_CREATE_GISTS = GUILD_CONFIG_OPTIONS.register("should_create_gists",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                                    (config, value) -> config.setShouldCreateGists(Boolean.parseBoolean(value)))
                                            .valueFromConfig(GuildConfig::isShouldCreateGists).build());

    public static final GuildConfigOption LOGGING_CHANNEL = GUILD_CONFIG_OPTIONS.register("logging_channel",
            new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                                                    (config, value) -> config.setLoggingChannel(Long.parseLong(value)))
                                            .valueFromConfig(GuildConfig::getLoggingChannel)
                                            .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    public static final GuildConfigOption OPT_IN_CHANNELS = GUILD_CONFIG_OPTIONS.register("opt_in_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setOptInChannels)
                                            .valueFromConfig(GuildConfig::getOptInChannels)
                                            .validator((event, value) -> {
                                                final String[] channels = value.split("[\s;]");
                                                for (final String channelStr : channels) {
                                                    if (!Validators.GUILD_CHANNEL_VALIDATOR.test(event, channelStr))
                                                        return false;
                                                }

                                                return true;
                                            }).build());

    public static final GuildConfigOption NSFW_CHANNELS = GUILD_CONFIG_OPTIONS.register("nsfw_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildConfig::setNsfwChannels)
                                            .valueFromConfig(GuildConfig::getNsfwChannels).validator((event, value) -> {
                                                final String[] channels = value.split("[\s;]");
                                                for (final String channelStr : channels) {
                                                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr)) return false;
                                                }

                                                return true;
                                            }).build());

    public static final GuildConfigOption ARE_WARNINGS_MODERATOR_ONLY = GUILD_CONFIG_OPTIONS.register(
            "warnings_moderator_only", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setWarningsModeratorOnly(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildConfig::isWarningsModeratorOnly).build());

    public static final GuildConfigOption MAX_COUNTING_SUCCESSION = GUILD_CONFIG_OPTIONS.register(
            "max_counting_succession", new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                    (config, value) -> config.setMaxCountingSuccession(Integer.parseInt(value))).valueFromConfig(
                    GuildConfig::getMaxCountingSuccession).build());

    public static final GuildConfigOption PATRON_ROLE = GUILD_CONFIG_OPTIONS.register("patron_role",
            new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                                                    (config, value) -> config.setPatronRole(Long.parseLong(value)))
                                            .valueFromConfig(GuildConfig::getPatronRole)
                                            .validator(Validators.ROLE_VALIDATOR).build());
}
