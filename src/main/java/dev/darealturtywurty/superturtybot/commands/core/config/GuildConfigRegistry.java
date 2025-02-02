package dev.darealturtywurty.superturtybot.commands.core.config;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigOption.DataType;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.ArrayList;
import java.util.List;

public class GuildConfigRegistry {
    public static final Registry<GuildConfigOption> GUILD_CONFIG_OPTIONS = new Registry<>();

    private static final GuildConfigOption STARBOARD = GUILD_CONFIG_OPTIONS.register("starboard",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setStarboard(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getStarboard)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption IS_STARBOARD_ENABLED = GUILD_CONFIG_OPTIONS.register("starboard_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setStarboardEnabled(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isStarboardEnabled).build());

    private static final GuildConfigOption MINIMUM_STARS = GUILD_CONFIG_OPTIONS.register("minimum_stars",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                            (config, value) -> config.setMinimumStars(Integer.parseInt(value)))
                    .valueFromConfig(GuildData::getMinimumStars).validator((event, str) -> {
                        if (event.getGuild() == null)
                            return false;

                        final int input = Integer.parseInt(str);
                        return input >= 1 && input <= event.getGuild().getMemberCount();
                    }).build());

    private static final GuildConfigOption DO_BOT_STARS_COUNT = GUILD_CONFIG_OPTIONS.register("bot_stars_count",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setBotStarsCount(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isBotStarsCount).build());

    private static final GuildConfigOption MOD_LOGGING = GUILD_CONFIG_OPTIONS.register("mod_logging",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setModLogging(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getModLogging)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption LEVEL_ROLES = GUILD_CONFIG_OPTIONS.register("level_roles",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setLevelRoles)
                    .valueFromConfig(GuildData::getLevelRoles).validator((event, value) -> {
                        if (event.getGuild() == null)
                            return false;

                        final String[] split = value.split("[ ;]");
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

    private static final GuildConfigOption LEVELLING_COOLDOWN = GUILD_CONFIG_OPTIONS.register("level_cooldown",
            new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                            (config, value) -> config.setLevelCooldown(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getLevelCooldown)
                    .validator((event, value) -> Long.parseLong(value) > 0).build());

    private static final GuildConfigOption MINIMUM_XP = GUILD_CONFIG_OPTIONS.register("min_xp",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                    .serializer((config, value) -> config.setMinXP(Integer.parseInt(value)))
                    .valueFromConfig(GuildData::getMinXP).validator(
                            (event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());

    private static final GuildConfigOption MAXIMUM_XP = GUILD_CONFIG_OPTIONS.register("max_xp",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                    .serializer((config, value) -> config.setMaxXP(Integer.parseInt(value)))
                    .valueFromConfig(GuildData::getMaxXP).validator(
                            (event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());

    private static final GuildConfigOption LEVELLING_ITEM_CHANCE = GUILD_CONFIG_OPTIONS.register(
            "levelling_item_chance", new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                    (config, value) -> config.setLevellingItemChance(Integer.parseInt(value))).valueFromConfig(
                    GuildData::getLevellingItemChance).validator(
                    (event, value) -> Integer.parseInt(value) >= 0 && Integer.parseInt(value) <= 100).build());

    private static final GuildConfigOption LEVELLING_ENABLED = GUILD_CONFIG_OPTIONS.register("levelling_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setLevellingEnabled(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isLevellingEnabled).build());

    private static final GuildConfigOption DISABLED_LEVELLING_CHANNELS = GUILD_CONFIG_OPTIONS.register(
            "disabled_levelling_channels", new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(
                    GuildData::setDisabledLevellingChannels).valueFromConfig(
                    GuildData::getDisabledLevellingChannels).validator((event, value) -> {
                final String[] channels = value.split("[ ;]");
                for (final String channelStr : channels) {
                    if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr)) return false;
                }

                return true;
            }).build());

    private static final GuildConfigOption SHOWCASE_CHANNELS = GUILD_CONFIG_OPTIONS.register("showcase_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setShowcaseChannels)
                    .valueFromConfig(GuildData::getShowcaseChannels)
                    .validator((event, value) -> {
                        final String[] channels = value.split("[ ;]");
                        for (final String channelStr : channels) {
                            if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                return false;
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption IS_STARBOARD_MEDIA = GUILD_CONFIG_OPTIONS.register(
            "is_starboard_media_only", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setStarboardMediaOnly(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildData::isStarboardMediaOnly).build());

    @SuppressWarnings("ConstantValue")
    private static final GuildConfigOption STAR_EMOJI = GUILD_CONFIG_OPTIONS.register("star_emoji",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setStarEmoji)
                    .valueFromConfig(GuildData::getStarEmoji).validator(
                            (event, value) -> (MentionType.EMOJI.getPattern().matcher(value).matches() || Emoji.fromUnicode(
                                    value) != null || Emoji.fromFormatted(value) != null)).build());

    private static final GuildConfigOption SUGGESTIONS = GUILD_CONFIG_OPTIONS.register("suggestions",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setSuggestions(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getSuggestions)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption DISABLE_LEVEL_UP_MESSAGES = GUILD_CONFIG_OPTIONS.register(
            "disable_level_up_messages", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setDisableLevelUpMessages(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildData::isDisableLevelUpMessages).build());

    private static final GuildConfigOption HAS_LEVEL_UP_CHANNEL = GUILD_CONFIG_OPTIONS.register("has_level_up_channel",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setHasLevelUpChannel(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isHasLevelUpChannel).build());

    private static final GuildConfigOption LEVEL_UP_MESSAGE_CHANNEL = GUILD_CONFIG_OPTIONS.register(
            "level_up_message_channel", new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                    (config, value) -> config.setLevelUpMessageChannel(Long.parseLong(value))).valueFromConfig(
                    GuildData::getLevelUpMessageChannel).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption SHOULD_EMBED_LEVEL_UP_MESSAGES = GUILD_CONFIG_OPTIONS.register(
            "should_embed_level_up_message", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setShouldEmbedLevelUpMessage(Boolean.parseBoolean(value)))
                    .valueFromConfig(
                            GuildData::isShouldEmbedLevelUpMessage)
                    .build());

    private static final GuildConfigOption SHOULD_MODERATORS_JOIN_THREADS = GUILD_CONFIG_OPTIONS.register(
            "should_moderators_join_threads", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setShouldModeratorsJoinThreads(Boolean.parseBoolean(value)))
                    .valueFromConfig(
                            GuildData::isShouldModeratorsJoinThreads)
                    .build());

    private static final GuildConfigOption AUTO_THREAD_CHANNELS = GUILD_CONFIG_OPTIONS.register("auto_thread_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setAutoThreadChannels)
                    .valueFromConfig(GuildData::getAutoThreadChannels)
                    .validator((event, value) -> {
                        final String[] channels = value.split("[ ;]");
                        for (final String channelStr : channels) {
                            if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                return false;
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption SHOULD_CREATE_GISTS = GUILD_CONFIG_OPTIONS.register("should_create_gists",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setShouldCreateGists(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isShouldCreateGists).build());

    private static final GuildConfigOption LOGGING_CHANNEL = GUILD_CONFIG_OPTIONS.register("logging_channel",
            new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                            (config, value) -> config.setLoggingChannel(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getLoggingChannel)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption OPT_IN_CHANNELS = GUILD_CONFIG_OPTIONS.register("opt_in_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setOptInChannels)
                    .valueFromConfig(GuildData::getOptInChannels)
                    .validator((event, value) -> {
                        final String[] channels = value.split("[ ;]");
                        for (final String channelStr : channels) {
                            if (!Validators.GUILD_CHANNEL_VALIDATOR.test(event, channelStr))
                                return false;
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption NSFW_CHANNELS = GUILD_CONFIG_OPTIONS.register("nsfw_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setNsfwChannels)
                    .valueFromConfig(GuildData::getNsfwChannels).validator((event, value) -> {
                        final String[] channels = value.split("[ ;]");
                        for (final String channelStr : channels) {
                            if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr)) return false;
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption ARE_WARNINGS_MODERATOR_ONLY = GUILD_CONFIG_OPTIONS.register(
            "warnings_moderator_only", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setWarningsModeratorOnly(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildData::isWarningsModeratorOnly).build());

    private static final GuildConfigOption MAX_COUNTING_SUCCESSION = GUILD_CONFIG_OPTIONS.register(
            "max_counting_succession", new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                    (config, value) -> config.setMaxCountingSuccession(Integer.parseInt(value))).valueFromConfig(
                    GuildData::getMaxCountingSuccession).build());

    private static final GuildConfigOption PATRON_ROLE = GUILD_CONFIG_OPTIONS.register("patron_role",
            new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                            (config, value) -> config.setPatronRole(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getPatronRole)
                    .validator(Validators.ROLE_VALIDATOR).build());

    private static final GuildConfigOption SHOULD_SEND_STARTUP_MESSAGE = GUILD_CONFIG_OPTIONS.register(
            "should_send_startup_message", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                    (config, value) -> config.setShouldSendStartupMessage(Boolean.parseBoolean(value))).valueFromConfig(
                    GuildData::isShouldSendStartupMessage).build());

    private static final GuildConfigOption ECONOMY_CURRENCY = GUILD_CONFIG_OPTIONS.register("economy_currency",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setEconomyCurrency)
                    .valueFromConfig(GuildData::getEconomyCurrency).build());

    private static final GuildConfigOption ECONOMY_ENABLED = GUILD_CONFIG_OPTIONS.register("economy_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setEconomyEnabled(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isEconomyEnabled).build());

    private static final GuildConfigOption CAN_ADD_PLAYLISTS = GUILD_CONFIG_OPTIONS.register("can_add_playlists",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                            (config, value) -> config.setCanAddPlaylists(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isCanAddPlaylists).build());

    private static final GuildConfigOption MAX_SONGS_PER_USER = GUILD_CONFIG_OPTIONS.register("max_songs_per_user",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                            (config, value) -> config.setMaxSongsPerUser(Integer.parseInt(value)))
                    .valueFromConfig(GuildData::getMaxSongsPerUser).build());

    private static final GuildConfigOption MUSIC_PERMISSIONS = GUILD_CONFIG_OPTIONS.register("music_permissions",
            new GuildConfigOption.Builder().dataType(DataType.STRING).serializer((guildConfig, s) -> {
                        // format:
                        // command:(role:permission);(role:permission);(role:permission)-command:(role:permission);(role:permission);(role:permission)

                        final String[] commands = s.split("-");
                        List<CommandPermission> permissions = new ArrayList<>();
                        for (final String command : commands) {
                            final String[] commandAndPermissions = command.split(":");
                            final String[] permissionsStr = commandAndPermissions[1].split(";");
                            List<CommandPermission.Permission> perms = new ArrayList<>();
                            for (final String permission : permissionsStr) {
                                final String[] roleAndPermission = permission.split(";");
                                long roleId = 0L;
                                try {
                                    roleId = Long.parseLong(roleAndPermission[0]);
                                } catch (final NumberFormatException ignored) {
                                }

                                perms.add(new CommandPermission.Permission(roleId, roleAndPermission[1]));
                            }

                            var cmdPermission = new CommandPermission(commandAndPermissions[0]);
                            cmdPermission.setPermissions(perms);
                            permissions.add(cmdPermission);
                        }

                        guildConfig.setMusicPermissions(permissions);
                    })
                    .valueFromConfig(GuildData::getMusicPermissions).validator((event, value) -> {
                        if (event.getGuild() == null)
                            return false;

                        final String[] commands = value.split("-");
                        for (final String command : commands) {
                            final String[] commandAndPermissions = command.split(":");
                            final String[] permissionsStr = commandAndPermissions[1].split(";");
                            for (final String permission : permissionsStr) {
                                final String[] roleAndPermission = permission.split(";");
                                if (roleAndPermission.length != 2) return false;
                                long roleId = 0L;
                                try {
                                    roleId = Long.parseLong(roleAndPermission[0]);
                                } catch (final NumberFormatException ignored) {
                                }

                                if (roleId != 0L) {
                                    final Role role = event.getGuild().getRoleById(roleId);
                                    if (role == null) return false;
                                }
                            }
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption CHAT_REVIVAL_ENABLED = GUILD_CONFIG_OPTIONS.register("chat_revival_enabled",
            new GuildConfigOption.Builder()
                    .dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setChatRevivalEnabled(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isChatRevivalEnabled).build());

    private static final GuildConfigOption CHAT_REVIVAL_CHANNEL = GUILD_CONFIG_OPTIONS.register("chat_revival_channel",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setChatRevivalChannel(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getChatRevivalChannel)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption CHAT_REVIVAL_TIME = GUILD_CONFIG_OPTIONS.register("chat_revival_time",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                    .serializer((config, value) -> config.setChatRevivalTime(Integer.parseInt(value)))
                    .valueFromConfig(GuildData::getChatRevivalTime)
                    .validator((event, value) -> Integer.parseInt(value) > 0).build());

    private static final GuildConfigOption WARNING_XP_PERCENTAGE = GUILD_CONFIG_OPTIONS.register("warning_xp_percentage",
            new GuildConfigOption.Builder().dataType(DataType.FLOAT)
                    .serializer((config, value) -> config.setWarningXpPercentage(Float.parseFloat(value)))
                    .valueFromConfig(GuildData::getWarningXpPercentage)
                    .validator((event, value) -> Float.parseFloat(value) > 0 && Float.parseFloat(value) < 100)
                    .build());

    private static final GuildConfigOption WARNING_ECONOMY_PERCENTAGE = GUILD_CONFIG_OPTIONS.register("warning_economy_percentage",
            new GuildConfigOption.Builder().dataType(DataType.FLOAT)
                    .serializer((config, value) -> config.setWarningEconomyPercentage(Float.parseFloat(value)))
                    .valueFromConfig(GuildData::getWarningEconomyPercentage)
                    .validator((event, value) -> Float.parseFloat(value) > 0 && Float.parseFloat(value) < 100)
                    .build());

    private static final GuildConfigOption DEFAULT_ECONOMY_BALANCE = GUILD_CONFIG_OPTIONS.register("default_economy_balance",
            new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                    .serializer((config, value) -> config.setDefaultEconomyBalance(Integer.parseInt(value)))
                    .valueFromConfig(GuildData::getDefaultEconomyBalance)
                    .validator((event, value) -> Integer.parseInt(value) > 0).build());

    private static final GuildConfigOption ANNOUNCE_BIRTHDAYS = GUILD_CONFIG_OPTIONS.register("announce_birthdays",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setAnnounceBirthdays(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isAnnounceBirthdays).build());

    private static final GuildConfigOption BIRTHDAY_CHANNEL = GUILD_CONFIG_OPTIONS.register("birthday_channel",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setBirthdayChannel(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getBirthdayChannel)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption WELCOME_CHANNEL = GUILD_CONFIG_OPTIONS.register("welcome_channel",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setWelcomeChannel(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getWelcomeChannel)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    private static final GuildConfigOption SHOULD_SEND_CHANGELOG = GUILD_CONFIG_OPTIONS.register("should_send_changelog",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setShouldSendChangelog(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isShouldSendChangelog).build());

    private static final GuildConfigOption AI_CHANNEL_WHITELIST = GUILD_CONFIG_OPTIONS.register("ai_channel_whitelist",
            new GuildConfigOption.Builder().dataType(DataType.STRING)
                    .serializer(GuildData::setAiChannelWhitelist)
                    .valueFromConfig(GuildData::getAiChannelWhitelist)
                    .validator((event, value) -> {
                        final String[] channels = value.split("[ ;]");
                        for (final String channelStr : channels) {
                            if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                return false;
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption AI_USER_BLACKLIST = GUILD_CONFIG_OPTIONS.register("ai_user_blacklist",
            new GuildConfigOption.Builder().dataType(DataType.STRING)
                    .serializer(GuildData::setAiUserBlacklist)
                    .valueFromConfig(GuildData::getAiUserBlacklist)
                    .validator((event, value) -> {
                        final String[] users = value.split("[ ;]");
                        for (final String userStr : users) {
                            if (!Validators.USER_VALIDATOR.test(event, userStr))
                                return false;
                        }

                        return true;
                    }).build());

    private static final GuildConfigOption AI_ENABLED = GUILD_CONFIG_OPTIONS.register("ai_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setAiEnabled(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isAiEnabled).build());

    private static final GuildConfigOption COLLECTOR_CHANNEL = GUILD_CONFIG_OPTIONS.register("collector_channel",
            new GuildConfigOption.Builder().dataType(DataType.LONG)
                    .serializer((config, value) -> config.setCollectorChannel(Long.parseLong(value)))
                    .valueFromConfig(GuildData::getCollectorChannel)
                    .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

    public static final GuildConfigOption COLLECTING_ENABLED = GUILD_CONFIG_OPTIONS.register("collecting_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setCollectingEnabled(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isCollectingEnabled).build());

    public static final GuildConfigOption DISCORD_INVITE_WHITELIST_CHANNELS = GUILD_CONFIG_OPTIONS.register("discord_invite_whitelist_channels",
            new GuildConfigOption.Builder().dataType(DataType.STRING)
                    .serializer(GuildData::setDiscordInviteWhitelistChannels)
                    .valueFromConfig(GuildData::getDiscordInviteWhitelistChannels)
                    .validator((event, value) -> {
                        final String[] channels = value.split("[ ;]");
                        for (final String channelStr : channels) {
                            if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                return false;
                        }

                        return true;
                    }).build());

    public static final GuildConfigOption LEVEL_DEPLETION_ENABLED = GUILD_CONFIG_OPTIONS.register("level_depletion_enabled",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setShouldDepleteLevels(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isShouldDepleteLevels).build());

    public static final GuildConfigOption SHOULD_ANNOUNCE_JOINS = GUILD_CONFIG_OPTIONS.register("should_announce_joins",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setShouldAnnounceJoins(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isShouldAnnounceJoins).build());

    public static final GuildConfigOption SHOULD_ANNOUNCE_LEAVES = GUILD_CONFIG_OPTIONS.register("should_announce_leaves",
            new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                    .serializer((config, value) -> config.setShouldAnnounceLeaves(Boolean.parseBoolean(value)))
                    .valueFromConfig(GuildData::isShouldAnnounceLeaves).build());
}
