package dev.darealturtywurty.superturtybot.dashboard.service.levelling;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

public final class LevellingSettingsService {
    private final JDA jda;

    public LevellingSettingsService(JDA jda) {
        this.jda = jda;
    }

    public LevellingSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public LevellingSettingsResponse updateSettings(long guildId, LevellingSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        validateRequest(guild, request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setLevellingEnabled(request.isLevellingEnabled());
        guildData.setLevelCooldown(request.getLevelCooldown());
        guildData.setMinXP(request.getMinXp());
        guildData.setMaxXP(request.getMaxXp());
        guildData.setLevellingItemChance(request.getLevellingItemChance());
        guildData.setDisabledLevellingChannels(String.join(" ", normalizeChannelIds(request.getDisabledLevellingChannelIds())));
        guildData.setDisableLevelUpMessages(request.isDisableLevelUpMessages());
        guildData.setHasLevelUpChannel(request.isHasLevelUpChannel());
        guildData.setLevelUpMessageChannel(parseChannelId(request.getLevelUpMessageChannelId()));
        guildData.setShouldEmbedLevelUpMessage(request.isShouldEmbedLevelUpMessage());
        guildData.setShouldDepleteLevels(request.isLevelDepletionEnabled());
        guildData.setLevelRoles(String.join(" ", normalizeLevelRoleMappings(request.getLevelRoleMappings())));
        guildData.setXpBoostedChannels(String.join(" ", normalizeChannelIds(request.getXpBoostedChannelIds())));
        guildData.setXpBoostedRoles(String.join(" ", normalizeRoleIds(request.getXpBoostedRoleIds())));
        guildData.setXpBoostPercentage(request.getXpBoostPercentage());
        guildData.setDoServerBoostsAffectXP(request.isDoServerBoostsAffectXP());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static LevellingSettingsResponse toResponse(GuildData guildData) {
        return new LevellingSettingsResponse(
                guildData.isLevellingEnabled(),
                guildData.getLevelCooldown(),
                guildData.getMinXP(),
                guildData.getMaxXP(),
                guildData.getLevellingItemChance(),
                GuildData.getLongs(guildData.getDisabledLevellingChannels()).stream().map(String::valueOf).toList(),
                guildData.isDisableLevelUpMessages(),
                guildData.isHasLevelUpChannel(),
                guildData.getLevelUpMessageChannel() == 0L ? null : Long.toString(guildData.getLevelUpMessageChannel()),
                guildData.isShouldEmbedLevelUpMessage(),
                guildData.isShouldDepleteLevels(),
                parseStoredLevelRoleMappings(guildData.getLevelRoles()),
                GuildData.getLongs(guildData.getXpBoostedChannels()).stream().map(String::valueOf).toList(),
                GuildData.getLongs(guildData.getXpBoostedRoles()).stream().map(String::valueOf).toList(),
                guildData.getXpBoostPercentage(),
                guildData.isDoServerBoostsAffectXP()
        );
    }

    private static void validateRequest(Guild guild, LevellingSettingsRequest request) {
        if (request.getLevelCooldown() <= 0L) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_level_cooldown",
                    "Levelling cooldown must be greater than zero milliseconds.");
        }

        if (request.getMinXp() <= 0 || request.getMinXp() >= 100) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_min_xp",
                    "Minimum XP must be between 1 and 99.");
        }

        if (request.getMaxXp() <= 0 || request.getMaxXp() >= 100) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_max_xp",
                    "Maximum XP must be between 1 and 99.");
        }

        if (request.getMinXp() > request.getMaxXp()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_xp_range",
                    "Minimum XP cannot be greater than maximum XP.");
        }

        if (request.getLevellingItemChance() < 0 || request.getLevellingItemChance() > 100) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_levelling_item_chance",
                    "Levelling item chance must be between 0 and 100.");
        }

        if (request.getXpBoostPercentage() < 0 || request.getXpBoostPercentage() > 1000) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_xp_boost_percentage",
                    "XP boost percentage must be between 0 and 1000.");
        }

        for (String channelId : normalizeChannelIds(request.getDisabledLevellingChannelIds())) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_disabled_levelling_channel",
                        "One or more disabled levelling channels were not valid text channels in this guild.");
            }
        }

        for (String channelId : normalizeChannelIds(request.getXpBoostedChannelIds())) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_xp_boosted_channel",
                        "One or more XP boosted channels were not valid text channels in this guild.");
            }
        }

        for (String roleId : normalizeRoleIds(request.getXpBoostedRoleIds())) {
            Role role = guild.getRoleById(roleId);
            if (role == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_xp_boosted_role",
                        "One or more XP boosted roles were not valid roles in this guild.");
            }
        }

        for (String mapping : normalizeLevelRoleMappings(request.getLevelRoleMappings())) {
            LevelRoleMapping parsed = parseLevelRoleMapping(mapping);
            Role role = guild.getRoleById(parsed.roleId());
            if (role == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_level_role",
                        "One or more level role mappings referenced a role that does not exist in this guild.");
            }
        }

        long levelUpMessageChannelId = parseChannelId(request.getLevelUpMessageChannelId());
        if (request.isHasLevelUpChannel() && levelUpMessageChannelId == 0L) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_level_up_message_channel",
                    "A level-up message channel is required while the dedicated level-up channel option is enabled.");
        }

        if (levelUpMessageChannelId != 0L && guild.getTextChannelById(levelUpMessageChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_level_up_message_channel",
                    "The supplied level-up message channel was not a text channel in this guild.");
        }
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank())
            return 0L;

        try {
            long parsed = Long.parseLong(channelId.trim());
            return Math.max(parsed, 0L);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_channel_id",
                    "One of the supplied channel IDs was not a valid Discord snowflake.");
        }
    }

    private static List<String> normalizeChannelIds(List<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty())
            return List.of();

        return new ArrayList<>(new LinkedHashSet<>(channelIds.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }

    private static List<String> normalizeRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty())
            return List.of();

        return new ArrayList<>(new LinkedHashSet<>(roleIds.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }

    private static List<String> normalizeLevelRoleMappings(List<String> mappings) {
        if (mappings == null || mappings.isEmpty())
            return List.of();

        return new ArrayList<>(new LinkedHashSet<>(mappings.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }

    private static List<String> parseStoredLevelRoleMappings(String value) {
        if (value == null || value.isBlank())
            return List.of();

        return Stream.of(value.split("[ ;]"))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }

    private static LevelRoleMapping parseLevelRoleMapping(String mapping) {
        String[] parts = mapping.split("->");
        if (parts.length != 2) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_level_role_format",
                    "Level role mappings must use the format `level->roleId`.");
        }

        try {
            int level = Integer.parseInt(parts[0].trim());
            long roleId = Long.parseLong(parts[1].trim());
            if (level <= 0 || roleId <= 0L)
                throw new NumberFormatException("Level and role ID must be positive.");

            return new LevelRoleMapping(level, roleId);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_level_role_format",
                    "Level role mappings must use the format `level->roleId` with positive numbers.");
        }
    }

    private record LevelRoleMapping(int level, long roleId) {
    }
}
