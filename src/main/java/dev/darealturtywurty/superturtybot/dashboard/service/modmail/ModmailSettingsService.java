package dev.darealturtywurty.superturtybot.dashboard.service.modmail;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ModmailSettingsService {
    private final JDA jda;

    public ModmailSettingsService(JDA jda) {
        this.jda = jda;
    }

    public ModmailSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public ModmailSettingsResponse updateSettings(long guildId, ModmailSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        validateRequest(guild, request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setModmailModeratorRoles(String.join(" ", normalizeRoleIds(request.getModeratorRoleIds())));
        guildData.setModmailTicketCreatedMessage(normalizeMessage(request.getTicketCreatedMessage()));

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static ModmailSettingsResponse toResponse(GuildData guildData) {
        return new ModmailSettingsResponse(
                GuildData.getLongs(guildData.getModmailModeratorRoles()).stream().map(String::valueOf).toList(),
                guildData.getModmailTicketCreatedMessage()
        );
    }

    private static void validateRequest(Guild guild, ModmailSettingsRequest request) {
        List<String> roleIds = normalizeRoleIds(request.getModeratorRoleIds());
        if (roleIds.isEmpty()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_modmail_roles",
                    "Modmail requires at least one moderator role.");
        }

        for (String roleId : roleIds) {
            Role role = guild.getRoleById(roleId);
            if (role == null || role.isManaged() || role.isPublicRole()) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_modmail_role",
                        "One or more modmail roles were not valid roles in this guild.");
            }
        }

        String message = normalizeMessage(request.getTicketCreatedMessage());
        if (message.length() > 2000) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_modmail_message",
                    "The ticket created message must be 2000 characters or fewer.");
        }

        if (message.contains("@everyone") || message.contains("@here")) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_modmail_message",
                    "The ticket created message cannot mention everyone or here.");
        }
    }

    private static String normalizeMessage(String message) {
        return message == null ? "" : message.trim();
    }

    private static List<String> normalizeRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(new LinkedHashSet<>(roleIds.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }
}
