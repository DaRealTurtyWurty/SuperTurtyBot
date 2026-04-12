package dev.darealturtywurty.superturtybot.dashboard.service.nsfw;

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

public final class NsfwSettingsService {
    private final JDA jda;

    public NsfwSettingsService(JDA jda) {
        this.jda = jda;
    }

    public NsfwSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public NsfwSettingsResponse updateSettings(long guildId, NsfwSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        if (request == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_nsfw_settings",
                    "The NSFW settings payload was missing.");
        }

        List<String> nsfwChannelIds = normalizeSnowflakes(request.getNsfwChannelIds());
        validateRequest(guild, nsfwChannelIds);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setNsfwChannels(String.join(" ", nsfwChannelIds));
        guildData.setArtistNsfwFilterEnabled(request.isArtistNsfwFilterEnabled());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static NsfwSettingsResponse toResponse(GuildData guildData) {
        return new NsfwSettingsResponse(
                GuildData.getLongs(guildData.getNsfwChannels()).stream().map(String::valueOf).toList(),
                guildData.isArtistNsfwFilterEnabled()
        );
    }

    private static void validateRequest(Guild guild, List<String> nsfwChannelIds) {
        for (String channelId : nsfwChannelIds) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_nsfw_channel",
                        "One or more NSFW channels were not valid text channels in this guild.");
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
