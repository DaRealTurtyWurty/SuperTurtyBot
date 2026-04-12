package dev.darealturtywurty.superturtybot.dashboard.service.ai;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class AiSettingsService {
    private final JDA jda;

    public AiSettingsService(JDA jda) {
        this.jda = jda;
    }

    public AiSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public AiSettingsResponse updateSettings(long guildId, AiSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        List<String> channelWhitelist = normalizeSnowflakes(request.getAiChannelWhitelist());
        List<String> userBlacklist = normalizeSnowflakes(request.getAiUserBlacklist());
        validateRequest(guild, channelWhitelist, userBlacklist);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setAiEnabled(request.isAiEnabled());
        guildData.setAiChannelWhitelist(String.join(" ", channelWhitelist));
        guildData.setAiUserBlacklist(String.join(" ", userBlacklist));

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static AiSettingsResponse toResponse(GuildData guildData) {
        return new AiSettingsResponse(
                guildData.isAiEnabled(),
                GuildData.getLongs(guildData.getAiChannelWhitelist()).stream().map(String::valueOf).toList(),
                GuildData.getLongs(guildData.getAiUserBlacklist()).stream().map(String::valueOf).toList()
        );
    }

    private static void validateRequest(Guild guild, List<String> channelWhitelist, List<String> userBlacklist) {
        for (String channelId : channelWhitelist) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_ai_channel",
                        "One or more AI whitelist channels were not valid text channels in this guild.");
            }
        }

        for (String userId : userBlacklist) {
            Member member = guild.getMemberById(userId);
            if (member == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_ai_user",
                        "One or more AI blacklisted users were not members of this guild.");
            }
        }
    }

    private static List<String> normalizeSnowflakes(List<String> values) {
        if (values == null || values.isEmpty())
            return List.of();

        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.isEmpty())
                continue;

            try {
                long parsed = Long.parseLong(trimmed);
                if (parsed <= 0L)
                    throw new NumberFormatException("Snowflake ID must be positive.");

                normalized.add(Long.toString(parsed));
            } catch (NumberFormatException exception) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_snowflake_id",
                        "One of the supplied IDs was not a valid Discord snowflake.");
            }
        }

        return new ArrayList<>(new LinkedHashSet<>(normalized));
    }
}
