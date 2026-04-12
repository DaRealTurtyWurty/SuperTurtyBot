package dev.darealturtywurty.superturtybot.dashboard.service.welcome;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public final class WelcomeSettingsService {
    private final JDA jda;

    public WelcomeSettingsService(JDA jda) {
        this.jda = jda;
    }

    public WelcomeSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public WelcomeSettingsResponse updateSettings(long guildId, WelcomeSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        long welcomeChannelId = parseChannelId(request.getWelcomeChannelId());
        if (welcomeChannelId != 0L && guild.getTextChannelById(welcomeChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_welcome_channel",
                    "The supplied welcome channel was not a text channel in this guild.");
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setWelcomeChannel(welcomeChannelId);
        guildData.setShouldAnnounceJoins(request.isShouldAnnounceJoins());
        guildData.setShouldAnnounceLeaves(request.isShouldAnnounceLeaves());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static WelcomeSettingsResponse toResponse(GuildData guildData) {
        return new WelcomeSettingsResponse(
                guildData.getWelcomeChannel() == 0L ? null : Long.toString(guildData.getWelcomeChannel()),
                guildData.isShouldAnnounceJoins(),
                guildData.isShouldAnnounceLeaves()
        );
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
}
