package dev.darealturtywurty.superturtybot.dashboard.service.threads;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ThreadSettingsService {
    private final JDA jda;

    public ThreadSettingsService(JDA jda) {
        this.jda = jda;
    }

    public ThreadSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public ThreadSettingsResponse updateSettings(long guildId, ThreadSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        if (request == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_thread_settings",
                    "The thread settings payload was missing.");
        }

        List<String> autoThreadChannelIds = normalizeSnowflakes(request.getAutoThreadChannelIds());
        validateRequest(guild, autoThreadChannelIds);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setShouldModeratorsJoinThreads(request.isShouldModeratorsJoinThreads());
        guildData.setAutoThreadChannels(String.join(" ", autoThreadChannelIds));

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static ThreadSettingsResponse toResponse(GuildData guildData) {
        return new ThreadSettingsResponse(
                guildData.isShouldModeratorsJoinThreads(),
                GuildData.getLongs(guildData.getAutoThreadChannels()).stream().map(String::valueOf).toList()
        );
    }

    private static void validateRequest(Guild guild, List<String> autoThreadChannelIds) {
        for (String channelId : autoThreadChannelIds) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_auto_thread_channel",
                        "One or more auto-thread channels were not valid text channels in this guild.");
            }
        }
    }

    private static List<String> normalizeSnowflakes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                long parsed = Long.parseLong(trimmed);
                if (parsed <= 0L) {
                    throw new NumberFormatException("Snowflake ID must be positive.");
                }

                normalized.add(Long.toString(parsed));
            } catch (NumberFormatException exception) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_snowflake_id",
                        "One of the supplied IDs was not a valid Discord snowflake.");
            }
        }

        return new ArrayList<>(new LinkedHashSet<>(normalized));
    }
}
