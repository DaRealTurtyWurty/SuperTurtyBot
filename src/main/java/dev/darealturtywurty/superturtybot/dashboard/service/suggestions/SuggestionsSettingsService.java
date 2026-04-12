package dev.darealturtywurty.superturtybot.dashboard.service.suggestions;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public final class SuggestionsSettingsService {
    private final JDA jda;

    public SuggestionsSettingsService(JDA jda) {
        this.jda = jda;
    }

    public SuggestionsSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public SuggestionsSettingsResponse updateSettings(long guildId, SuggestionsSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        long suggestionsChannelId = parseChannelId(request.getSuggestionsChannelId());
        if (suggestionsChannelId != 0L && guild.getTextChannelById(suggestionsChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_suggestions_channel",
                    "The supplied suggestions channel was not a text channel in this guild.");
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setSuggestions(suggestionsChannelId);

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static SuggestionsSettingsResponse toResponse(GuildData guildData) {
        return new SuggestionsSettingsResponse(
                guildData.getSuggestions() == 0L ? null : Long.toString(guildData.getSuggestions())
        );
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return 0L;
        }

        try {
            long parsed = Long.parseLong(channelId.trim());
            return Math.max(parsed, 0L);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_channel_id",
                    "One of the supplied channel IDs was not a valid Discord snowflake.");
        }
    }
}
