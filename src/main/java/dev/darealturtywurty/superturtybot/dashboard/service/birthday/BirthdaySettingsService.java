package dev.darealturtywurty.superturtybot.dashboard.service.birthday;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public final class BirthdaySettingsService {
    private final JDA jda;

    public BirthdaySettingsService(JDA jda) {
        this.jda = jda;
    }

    public BirthdaySettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public BirthdaySettingsResponse updateSettings(long guildId, BirthdaySettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        long birthdayChannelId = parseChannelId(request.getBirthdayChannelId());
        if (birthdayChannelId != 0L && guild.getTextChannelById(birthdayChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_birthday_channel",
                    "The supplied birthday channel was not a text channel in this guild.");
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setBirthdayChannel(birthdayChannelId);
        guildData.setAnnounceBirthdays(request.isAnnounceBirthdays());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static BirthdaySettingsResponse toResponse(GuildData guildData) {
        return new BirthdaySettingsResponse(
                guildData.getBirthdayChannel() == 0L ? null : Long.toString(guildData.getBirthdayChannel()),
                guildData.isAnnounceBirthdays()
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
