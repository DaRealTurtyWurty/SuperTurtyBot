package dev.darealturtywurty.superturtybot.commands.core.config;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigOption.DataType;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GuildConfigRegistry {
    public static final Registry<GuildConfigOption> GUILD_CONFIG_OPTIONS = new Registry<>();

    static {
        GUILD_CONFIG_OPTIONS.register("starboard",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setStarboard(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getStarboard)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("starboard_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setStarboardEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isStarboardEnabled).build());

        GUILD_CONFIG_OPTIONS.register("minimum_stars",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                                (config, value) -> config.setMinimumStars(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getMinimumStars).validator((event, str) -> {
                            if (event.getGuild() == null)
                                return false;

                            final int input = Integer.parseInt(str);
                            return input >= 1 && input <= event.getGuild().getMemberCount();
                        }).build());

        GUILD_CONFIG_OPTIONS.register("bot_stars_count",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setBotStarsCount(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isBotStarsCount).build());

        GUILD_CONFIG_OPTIONS.register("mod_logging",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setModLogging(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getModLogging)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("level_roles",
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

        GUILD_CONFIG_OPTIONS.register("level_cooldown",
                new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                                (config, value) -> config.setLevelCooldown(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getLevelCooldown)
                        .validator((event, value) -> Long.parseLong(value) > 0).build());

        GUILD_CONFIG_OPTIONS.register("min_xp",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setMinXP(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getMinXP).validator(
                                (event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());

        GUILD_CONFIG_OPTIONS.register("max_xp",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setMaxXP(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getMaxXP).validator(
                                (event, value) -> Integer.parseInt(value) > 0 && Integer.parseInt(value) < 100).build());

        GUILD_CONFIG_OPTIONS.register(
                "levelling_item_chance", new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                        (config, value) -> config.setLevellingItemChance(Integer.parseInt(value))).valueFromConfig(
                        GuildData::getLevellingItemChance).validator(
                        (event, value) -> Integer.parseInt(value) >= 0 && Integer.parseInt(value) <= 100).build());

        GUILD_CONFIG_OPTIONS.register("levelling_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setLevellingEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isLevellingEnabled).build());

        GUILD_CONFIG_OPTIONS.register(
                "disabled_levelling_channels", new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(
                        GuildData::setDisabledLevellingChannels).valueFromConfig(
                        GuildData::getDisabledLevellingChannels).validator((event, value) -> {
                    final String[] channels = value.split("[ ;]");
                    for (final String channelStr : channels) {
                        if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr)) return false;
                    }

                    return true;
                }).build());

        GUILD_CONFIG_OPTIONS.register("showcase_channels",
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

        GUILD_CONFIG_OPTIONS.register(
                "is_starboard_media_only", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                        (config, value) -> config.setStarboardMediaOnly(Boolean.parseBoolean(value))).valueFromConfig(
                        GuildData::isStarboardMediaOnly).build());

        GUILD_CONFIG_OPTIONS.register("star_emoji",
                new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setStarEmoji)
                        .valueFromConfig(GuildData::getStarEmoji).validator(
                                (event, value) -> (MentionType.EMOJI.getPattern().matcher(value).matches() || Emoji.fromUnicode(
                                        value) != null || Emoji.fromFormatted(value) != null)).build());

        GUILD_CONFIG_OPTIONS.register("suggestions",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setSuggestions(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getSuggestions)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register(
                "disable_level_up_messages", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                        (config, value) -> config.setDisableLevelUpMessages(Boolean.parseBoolean(value))).valueFromConfig(
                        GuildData::isDisableLevelUpMessages).build());

        GUILD_CONFIG_OPTIONS.register("has_level_up_channel",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setHasLevelUpChannel(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isHasLevelUpChannel).build());

        GUILD_CONFIG_OPTIONS.register(
                "level_up_message_channel", new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                        (config, value) -> config.setLevelUpMessageChannel(Long.parseLong(value))).valueFromConfig(
                        GuildData::getLevelUpMessageChannel).validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register(
                "should_embed_level_up_message", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setShouldEmbedLevelUpMessage(Boolean.parseBoolean(value)))
                        .valueFromConfig(
                                GuildData::isShouldEmbedLevelUpMessage)
                        .build());

        GUILD_CONFIG_OPTIONS.register(
                "should_moderators_join_threads", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setShouldModeratorsJoinThreads(Boolean.parseBoolean(value)))
                        .valueFromConfig(
                                GuildData::isShouldModeratorsJoinThreads)
                        .build());

        GUILD_CONFIG_OPTIONS.register("auto_thread_channels",
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

        GUILD_CONFIG_OPTIONS.register("should_create_gists",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setShouldCreateGists(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isShouldCreateGists).build());

        GUILD_CONFIG_OPTIONS.register("logging_channel",
                new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                                (config, value) -> config.setLoggingChannel(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getLoggingChannel)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("opt_in_channels",
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

        GUILD_CONFIG_OPTIONS.register("nsfw_channels",
                new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setNsfwChannels)
                        .valueFromConfig(GuildData::getNsfwChannels).validator((event, value) -> {
                            final String[] channels = value.split("[ ;]");
                            for (final String channelStr : channels) {
                                if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr)) return false;
                            }

                            return true;
                        }).build());

        GUILD_CONFIG_OPTIONS.register(
                "warnings_moderator_only", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                        (config, value) -> config.setWarningsModeratorOnly(Boolean.parseBoolean(value))).valueFromConfig(
                        GuildData::isWarningsModeratorOnly).build());

        GUILD_CONFIG_OPTIONS.register(
                "max_counting_succession", new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                        (config, value) -> config.setMaxCountingSuccession(Integer.parseInt(value))).valueFromConfig(
                        GuildData::getMaxCountingSuccession).build());

        GUILD_CONFIG_OPTIONS.register("patron_role",
                new GuildConfigOption.Builder().dataType(DataType.LONG).serializer(
                                (config, value) -> config.setPatronRole(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getPatronRole)
                        .validator(Validators.ROLE_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register(
                "should_send_startup_message", new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                        (config, value) -> config.setShouldSendStartupMessage(Boolean.parseBoolean(value))).valueFromConfig(
                        GuildData::isShouldSendStartupMessage).build());

        GUILD_CONFIG_OPTIONS.register("economy_currency",
                new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setEconomyCurrency)
                        .valueFromConfig(GuildData::getEconomyCurrency).build());

        GUILD_CONFIG_OPTIONS.register("economy_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setEconomyEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isEconomyEnabled).build());

        GUILD_CONFIG_OPTIONS.register("can_add_playlists",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN).serializer(
                                (config, value) -> config.setCanAddPlaylists(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isCanAddPlaylists).build());

        GUILD_CONFIG_OPTIONS.register("max_songs_per_user",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER).serializer(
                                (config, value) -> config.setMaxSongsPerUser(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getMaxSongsPerUser).build());

        GUILD_CONFIG_OPTIONS.register("music_permissions",
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

        GUILD_CONFIG_OPTIONS.register("chat_revival_enabled",
                new GuildConfigOption.Builder()
                        .dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setChatRevivalEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isChatRevivalEnabled).build());

        GUILD_CONFIG_OPTIONS.register("chat_revival_channel",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setChatRevivalChannel(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getChatRevivalChannel)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("chat_revival_time",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setChatRevivalTime(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getChatRevivalTime)
                        .validator((event, value) -> Integer.parseInt(value) > 0).build());

        GUILD_CONFIG_OPTIONS.register("warning_xp_percentage",
                new GuildConfigOption.Builder().dataType(DataType.FLOAT)
                        .serializer((config, value) -> config.setWarningXpPercentage(Float.parseFloat(value)))
                        .valueFromConfig(GuildData::getWarningXpPercentage)
                        .validator((event, value) -> Float.parseFloat(value) > 0 && Float.parseFloat(value) < 100)
                        .build());

        GUILD_CONFIG_OPTIONS.register("warning_economy_percentage",
                new GuildConfigOption.Builder().dataType(DataType.FLOAT)
                        .serializer((config, value) -> config.setWarningEconomyPercentage(Float.parseFloat(value)))
                        .valueFromConfig(GuildData::getWarningEconomyPercentage)
                        .validator((event, value) -> Float.parseFloat(value) > 0 && Float.parseFloat(value) < 100)
                        .build());

        GUILD_CONFIG_OPTIONS.register("default_economy_balance",
                new GuildConfigOption.Builder().dataType(DataType.STRING)
                        .serializer((config, value) -> config.setDefaultEconomyBalance(new BigInteger(value)))
                        .valueFromConfig(GuildData::getDefaultEconomyBalance)
                        .validator((event, value) -> {
                            try {
                                return new BigInteger(value).signum() > 0;
                            } catch (NumberFormatException ignored) {
                                return false;
                            }
                        }).build());

        GUILD_CONFIG_OPTIONS.register("announce_birthdays",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setAnnounceBirthdays(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isAnnounceBirthdays).build());

        GUILD_CONFIG_OPTIONS.register("birthday_channel",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setBirthdayChannel(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getBirthdayChannel)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("welcome_channel",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setWelcomeChannel(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getWelcomeChannel)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("should_send_changelog",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setShouldSendChangelog(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isShouldSendChangelog).build());

        GUILD_CONFIG_OPTIONS.register("ai_channel_whitelist",
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

        GUILD_CONFIG_OPTIONS.register("ai_user_blacklist",
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

        GUILD_CONFIG_OPTIONS.register("ai_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setAiEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isAiEnabled).build());

        GUILD_CONFIG_OPTIONS.register("collector_channel",
                new GuildConfigOption.Builder().dataType(DataType.LONG)
                        .serializer((config, value) -> config.setCollectorChannel(Long.parseLong(value)))
                        .valueFromConfig(GuildData::getCollectorChannel)
                        .validator(Validators.TEXT_CHANNEL_VALIDATOR).build());

        GUILD_CONFIG_OPTIONS.register("collecting_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setCollectingEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isCollectingEnabled).build());

        GUILD_CONFIG_OPTIONS.register("discord_invite_whitelist_channels",
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

        GUILD_CONFIG_OPTIONS.register("level_depletion_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setShouldDepleteLevels(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isShouldDepleteLevels).build());

        GUILD_CONFIG_OPTIONS.register("should_announce_joins",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setShouldAnnounceJoins(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isShouldAnnounceJoins).build());

        GUILD_CONFIG_OPTIONS.register("should_announce_leaves",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setShouldAnnounceLeaves(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isShouldAnnounceLeaves).build());

        GUILD_CONFIG_OPTIONS.register("xp_boosted_channels",
                new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setXpBoostedChannels)
                        .valueFromConfig(GuildData::getXpBoostedChannels)
                        .validator((event, value) -> {
                            final String[] channels = value.split("[ ;]");
                            for (final String channelStr : channels) {
                                if (!Validators.TEXT_CHANNEL_VALIDATOR.test(event, channelStr))
                                    return false;
                            }

                            return true;
                        }).build());

        GUILD_CONFIG_OPTIONS.register("xp_boosted_roles",
                new GuildConfigOption.Builder().dataType(DataType.STRING).serializer(GuildData::setXpBoostedRoles)
                        .valueFromConfig(GuildData::getXpBoostedRoles)
                        .validator((event, value) -> {
                            final String[] roles = value.split("[ ;]");
                            for (final String roleStr : roles) {
                                if (!Validators.ROLE_VALIDATOR.test(event, roleStr))
                                    return false;
                            }

                            return true;
                        }).build());

        GUILD_CONFIG_OPTIONS.register("xp_boost_percentage",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setXpBoostPercentage(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getXpBoostPercentage)
                        .validator((event, value) -> Integer.parseInt(value) >= 0 && Integer.parseInt(value) <= 100)
                        .build());

        GUILD_CONFIG_OPTIONS.register("do_server_boosts_affect_xp",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setDoServerBoostsAffectXP(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isDoServerBoostsAffectXP).build());

        GUILD_CONFIG_OPTIONS.register("image_spam_autoban_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setImageSpamAutoBanEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isImageSpamAutoBanEnabled).build());

        GUILD_CONFIG_OPTIONS.register("image_spam_window_seconds",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setImageSpamWindowSeconds(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getImageSpamWindowSeconds)
                        .validator((event, value) -> Integer.parseInt(value) >= 1).build());

        GUILD_CONFIG_OPTIONS.register("image_spam_min_images",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setImageSpamMinImages(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getImageSpamMinImages)
                        .validator((event, value) -> Integer.parseInt(value) >= 1).build());

        GUILD_CONFIG_OPTIONS.register("image_spam_new_member_threshold_hours",
                new GuildConfigOption.Builder().dataType(DataType.INTEGER)
                        .serializer((config, value) -> config.setImageSpamNewMemberThresholdHours(Integer.parseInt(value)))
                        .valueFromConfig(GuildData::getImageSpamNewMemberThresholdHours)
                        .validator((event, value) -> Integer.parseInt(value) >= 1).build());

        GUILD_CONFIG_OPTIONS.register("discord_invite_guard_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setDiscordInviteGuardEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isDiscordInviteGuardEnabled).build());

        GUILD_CONFIG_OPTIONS.register("scam_detection_enabled",
                new GuildConfigOption.Builder().dataType(DataType.BOOLEAN)
                        .serializer((config, value) -> config.setScamDetectionEnabled(Boolean.parseBoolean(value)))
                        .valueFromConfig(GuildData::isScamDetectionEnabled).build());
    }
}
