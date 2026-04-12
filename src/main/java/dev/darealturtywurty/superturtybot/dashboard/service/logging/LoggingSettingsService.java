package dev.darealturtywurty.superturtybot.dashboard.service.logging;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public final class LoggingSettingsService {
    private final JDA jda;

    public LoggingSettingsService(JDA jda) {
        this.jda = jda;
    }

    public LoggingSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public LoggingSettingsResponse updateSettings(long guildId, LoggingSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        long loggingChannelId = parseChannelId(request.getLoggingChannelId());
        if (loggingChannelId != 0L && guild.getTextChannelById(loggingChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_logging_channel",
                    "The supplied logging channel was not a text channel in this guild.");
        }

        long modLoggingChannelId = parseChannelId(request.getModLoggingChannelId());
        if (modLoggingChannelId != 0L && guild.getTextChannelById(modLoggingChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_mod_logging_channel",
                    "The supplied moderation logging channel was not a text channel in this guild.");
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setLoggingChannel(loggingChannelId);
        guildData.setModLogging(modLoggingChannelId);
        guildData.setLogChannelCreate(request.isLogChannelCreate());
        guildData.setLogChannelDelete(request.isLogChannelDelete());
        guildData.setLogChannelUpdate(request.isLogChannelUpdate());
        guildData.setLogEmojiAdded(request.isLogEmojiAdded());
        guildData.setLogEmojiRemoved(request.isLogEmojiRemoved());
        guildData.setLogEmojiUpdate(request.isLogEmojiUpdate());
        guildData.setLogForumTagUpdate(request.isLogForumTagUpdate());
        guildData.setLogStickerUpdate(request.isLogStickerUpdate());
        guildData.setLogGuildUpdate(request.isLogGuildUpdate());
        guildData.setLogRoleUpdate(request.isLogRoleUpdate());
        guildData.setLogBan(request.isLogBan());
        guildData.setLogUnban(request.isLogUnban());
        guildData.setLogInviteCreate(request.isLogInviteCreate());
        guildData.setLogInviteDelete(request.isLogInviteDelete());
        guildData.setLogMemberJoin(request.isLogMemberJoin());
        guildData.setLogMemberRemove(request.isLogMemberRemove());
        guildData.setLogStickerAdded(request.isLogStickerAdded());
        guildData.setLogStickerRemove(request.isLogStickerRemove());
        guildData.setLogTimeout(request.isLogTimeout());
        guildData.setLogMessageBulkDelete(request.isLogMessageBulkDelete());
        guildData.setLogMessageDelete(request.isLogMessageDelete());
        guildData.setLogMessageUpdate(request.isLogMessageUpdate());
        guildData.setLogRoleCreate(request.isLogRoleCreate());
        guildData.setLogRoleDelete(request.isLogRoleDelete());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static LoggingSettingsResponse toResponse(GuildData guildData) {
        return new LoggingSettingsResponse(
                guildData.getLoggingChannel() == 0L ? null : Long.toString(guildData.getLoggingChannel()),
                guildData.getModLogging() == 0L ? null : Long.toString(guildData.getModLogging()),
                guildData.isLogChannelCreate(),
                guildData.isLogChannelDelete(),
                guildData.isLogChannelUpdate(),
                guildData.isLogEmojiAdded(),
                guildData.isLogEmojiRemoved(),
                guildData.isLogEmojiUpdate(),
                guildData.isLogForumTagUpdate(),
                guildData.isLogStickerUpdate(),
                guildData.isLogGuildUpdate(),
                guildData.isLogRoleUpdate(),
                guildData.isLogBan(),
                guildData.isLogUnban(),
                guildData.isLogInviteCreate(),
                guildData.isLogInviteDelete(),
                guildData.isLogMemberJoin(),
                guildData.isLogMemberRemove(),
                guildData.isLogStickerAdded(),
                guildData.isLogStickerRemove(),
                guildData.isLogTimeout(),
                guildData.isLogMessageBulkDelete(),
                guildData.isLogMessageDelete(),
                guildData.isLogMessageUpdate(),
                guildData.isLogRoleCreate(),
                guildData.isLogRoleDelete()
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
