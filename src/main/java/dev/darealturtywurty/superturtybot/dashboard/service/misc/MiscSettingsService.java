package dev.darealturtywurty.superturtybot.dashboard.service.misc;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public final class MiscSettingsService {
    private final JDA jda;

    public MiscSettingsService(JDA jda) {
        this.jda = jda;
    }

    public MiscSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public MiscSettingsResponse updateSettings(long guildId, MiscSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        if (request == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_misc_settings",
                    "The misc settings payload was missing.");
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setShouldCreateGists(request.isShouldCreateGists());
        guildData.setShouldSendStartupMessage(request.isShouldSendStartupMessage());
        guildData.setShouldSendChangelog(request.isShouldSendChangelog());
        guildData.setStickyRolesEnabled(request.isStickyRolesEnabled());
        guildData.setPatronRole(parseRoleId(guild, request.getPatronRoleId()));

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static MiscSettingsResponse toResponse(GuildData guildData) {
        return new MiscSettingsResponse(
                guildData.isShouldCreateGists(),
                guildData.isShouldSendStartupMessage(),
                guildData.isShouldSendChangelog(),
                guildData.isStickyRolesEnabled(),
                guildData.getPatronRole() == 0L ? null : Long.toString(guildData.getPatronRole())
        );
    }

    private static long parseRoleId(Guild guild, String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return 0L;
        }

        try {
            long parsed = Long.parseLong(roleId.trim());
            if (guild.getRoleById(parsed) == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_patron_role",
                        "The supplied patron role was not a valid role in this guild.");
            }

            return Math.max(parsed, 0L);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_patron_role",
                    "The supplied patron role ID was not a valid Discord snowflake.");
        }
    }
}
