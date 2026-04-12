package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarnManager;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;

public final class WarningsSettingsService {
    private final JDA jda;

    public WarningsSettingsService(JDA jda) {
        this.jda = jda;
    }

    public DashboardWarningsResponse getSettings(long guildId) {
        Guild guild = requireGuild(guildId);
        return new DashboardWarningsResponse(toSettings(GuildData.getOrCreateGuildData(guildId)), listWarnings(guild));
    }

    public DashboardWarningsResponse updateSettings(long guildId, WarningsSettingsRequest request) {
        Guild guild = requireGuild(guildId);
        validateRequest(request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setWarningsModeratorOnly(request.warningsModeratorOnly());
        guildData.setWarningXpPercentage(request.warningXpPercentage());
        guildData.setWarningEconomyPercentage(request.warningEconomyPercentage());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return new DashboardWarningsResponse(toSettings(guildData), listWarnings(guild));
    }

    public DashboardWarningDetailResponse getWarningDetail(long guildId, String warningUuid) {
        Guild guild = requireGuild(guildId);
        if (warningUuid == null || warningUuid.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_uuid",
                    "The warning UUID was missing.");
        }

        Warning warning = Database.getDatabase().warnings.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("uuid", warningUuid)
        )).first();
        if (warning == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "warning_not_found",
                    "That warning could not be found.");
        }

        return new DashboardWarningDetailResponse(
                toRecord(guild, warning),
                toUserSummary(guild, warning.getUser()),
                listWarningsForUser(guild, warning.getUser(), warningUuid)
        );
    }

    public DashboardWarningHistoryResponse getUserWarnings(long guildId, long userId) {
        Guild guild = requireGuild(guildId);
        return new DashboardWarningHistoryResponse(
                toUserSummary(guild, userId),
                listWarningsForUser(guild, userId, null)
        );
    }

    public DashboardWarningsResponse deleteWarning(long guildId, String warningUuid) {
        Guild guild = requireGuild(guildId);
        if (warningUuid == null || warningUuid.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_uuid",
                    "The warning UUID was missing.");
        }

        Warning warning = Database.getDatabase().warnings.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("uuid", warningUuid)
        )).first();
        if (warning == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "warning_not_found",
                    "That warning could not be found.");
        }

        var warnedUser = this.jda.getUserById(warning.getUser());
        if (warnedUser == null) {
            try {
                warnedUser = this.jda.retrieveUserById(warning.getUser()).complete();
            } catch (Exception ignored) {
            }
        }

        if (warnedUser != null) {
            WarnManager.removeWarn(warnedUser, guild, warningUuid, this.jda.getSelfUser());
        } else {
            Database.getDatabase().warnings.findOneAndDelete(Filters.and(
                    Filters.eq("guild", guildId),
                    Filters.eq("uuid", warningUuid)
            ));
        }

        return new DashboardWarningsResponse(toSettings(GuildData.getOrCreateGuildData(guildId)), listWarnings(guild));
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private static WarningsSettingsResponse toSettings(GuildData guildData) {
        return new WarningsSettingsResponse(
                guildData.isWarningsModeratorOnly(),
                guildData.getWarningXpPercentage(),
                guildData.getWarningEconomyPercentage()
        );
    }

    private List<DashboardWarningRecord> listWarnings(Guild guild) {
        return Database.getDatabase().warnings.find(Filters.eq("guild", guild.getIdLong()))
                .sort(Sorts.descending("warnedAt"))
                .limit(100)
                .into(new ArrayList<>())
                .stream()
                .map(warning -> toRecord(guild, warning))
                .toList();
    }

    private List<DashboardWarningRecord> listWarningsForUser(Guild guild, long userId, String excludeUuid) {
        var warnings = Database.getDatabase().warnings.find(Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", userId)))
                .sort(Sorts.descending("warnedAt"))
                .limit(100)
                .into(new ArrayList<>())
                .stream()
                .filter(warning -> excludeUuid == null || !excludeUuid.equals(warning.getUuid()))
                .map(warning -> toRecord(guild, warning))
                .toList();

        return warnings;
    }

    private DashboardWarningRecord toRecord(Guild guild, Warning warning) {
        ResolvedUser warnedUser = resolveUser(guild, warning.getUser());
        ResolvedUser warnerUser = resolveUser(guild, warning.getWarner());

        return new DashboardWarningRecord(
                warning.getUuid(),
                Long.toString(warning.getUser()),
                warnedUser.displayName(),
                warnedUser.avatarUrl(),
                Long.toString(warning.getWarner()),
                warnerUser.displayName(),
                warnerUser.avatarUrl(),
                warning.getReason(),
                warning.getWarnedAt()
        );
    }

    private DashboardWarningUserSummary toUserSummary(Guild guild, long userId) {
        ResolvedUser user = resolveUser(guild, userId);
        return new DashboardWarningUserSummary(Long.toString(userId), user.displayName(), user.avatarUrl());
    }

    private ResolvedUser resolveUser(Guild guild, long userId) {
        Member member = guild.getMemberById(userId);
        if (member != null) {
            return new ResolvedUser(member.getEffectiveName(), member.getEffectiveAvatarUrl());
        }

        var user = this.jda.getUserById(userId);
        if (user != null) {
            return new ResolvedUser(user.getName(), user.getEffectiveAvatarUrl());
        }

        return new ResolvedUser("Unknown User", null);
    }

    private static void validateRequest(WarningsSettingsRequest request) {
        if (request == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warnings_settings",
                    "The warnings settings payload was missing.");
        }

        if (!Float.isFinite(request.warningXpPercentage()) || request.warningXpPercentage() < 0F || request.warningXpPercentage() >= 100F) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_xp_percentage",
                    "The warning XP percentage must be between 0 and 100.");
        }

        if (!Float.isFinite(request.warningEconomyPercentage()) || request.warningEconomyPercentage() < 0F || request.warningEconomyPercentage() >= 100F) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_economy_percentage",
                    "The warning economy percentage must be between 0 and 100.");
        }
    }

    private record ResolvedUser(String displayName, String avatarUrl) {
    }
}
