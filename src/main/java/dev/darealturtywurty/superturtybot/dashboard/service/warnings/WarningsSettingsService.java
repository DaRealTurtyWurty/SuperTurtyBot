package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarnManager;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import dev.darealturtywurty.superturtybot.database.pojos.warnings.WarningSanctionAction;
import dev.darealturtywurty.superturtybot.database.pojos.warnings.WarningSanctionConfig;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        guildData.setWarningExpiryDays(request.warningExpiryDays());
        guildData.setWarningXpPercentage(request.warningXpPercentage());
        guildData.setWarningEconomyPercentage(request.warningEconomyPercentage());
        guildData.setWarningSanctions(toSanctions(request.sanctions()));

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return new DashboardWarningsResponse(toSettings(guildData), listWarnings(guild));
    }

    public DashboardWarningDetailResponse getWarningDetail(long guildId, String warningUuid) {
        Guild guild = requireGuild(guildId);
        if (warningUuid == null || warningUuid.isBlank())
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_uuid",
                    "The warning UUID was missing.");

        Warning warning = Database.getDatabase().warnings.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("uuid", warningUuid)
        )).first();
        if (warning == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "warning_not_found",
                    "That warning could not be found.");

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
        if (warningUuid == null || warningUuid.isBlank())
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_uuid",
                    "The warning UUID was missing.");

        Warning warning = Database.getDatabase().warnings.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("uuid", warningUuid)
        )).first();
        if (warning == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "warning_not_found",
                    "That warning could not be found.");

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
        if (guild == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");

        return guild;
    }

    private static WarningsSettingsResponse toSettings(GuildData guildData) {
        return new WarningsSettingsResponse(
                guildData.isWarningsModeratorOnly(),
                Math.max(0, guildData.getWarningExpiryDays()),
                guildData.getWarningXpPercentage(),
                guildData.getWarningEconomyPercentage(),
                guildData.getEffectiveWarningSanctions().stream()
                        .map(WarningsSettingsService::toPayload)
                        .toList()
        );
    }

    private static WarningSanctionPayload toPayload(WarningSanctionConfig sanction) {
        return new WarningSanctionPayload(
                sanction.getId(),
                sanction.getType(),
                sanction.getWarningCount(),
                sanction.getDurationMinutes(),
                sanction.getDeleteMessageDays()
        );
    }

    private static List<WarningSanctionConfig> toSanctions(List<WarningSanctionPayload> sanctions) {
        if (sanctions == null || sanctions.isEmpty())
            return GuildData.createDefaultWarningSanctions();

        return sanctions.stream()
                .map(payload -> new WarningSanctionConfig(
                        payload.id() == null || payload.id().isBlank() ? UUID.randomUUID().toString() : payload.id(),
                        payload.type(),
                        payload.warningCount(),
                        payload.durationMinutes(),
                        payload.deleteMessageDays()
                ))
                .toList();
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
        GuildData config = GuildData.getOrCreateGuildData(guild);
        long expiresAt = WarnManager.getWarningExpiresAt(config, warning);
        boolean active = WarnManager.isWarningActive(config, warning, System.currentTimeMillis());

        return new DashboardWarningRecord(
                warning.getUuid(),
                Long.toString(warning.getUser()),
                warnedUser.displayName(),
                warnedUser.avatarUrl(),
                Long.toString(warning.getWarner()),
                warnerUser.displayName(),
                warnerUser.avatarUrl(),
                warning.getReason(),
                warning.getWarnedAt(),
                expiresAt,
                active
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
        if (request == null)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warnings_settings",
                    "The warnings settings payload was missing.");

        if (request.warningExpiryDays() < 0 || request.warningExpiryDays() > 3650)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_expiry_days",
                    "Warning expiry must be between 0 and 3650 days.");

        if (!Float.isFinite(request.warningXpPercentage()) || request.warningXpPercentage() < 0F || request.warningXpPercentage() >= 100F)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_xp_percentage",
                    "The warning XP percentage must be between 0 and 100.");

        if (!Float.isFinite(request.warningEconomyPercentage()) || request.warningEconomyPercentage() < 0F || request.warningEconomyPercentage() >= 100F)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_economy_percentage",
                    "The warning economy percentage must be between 0 and 100.");

        if (request.sanctions() == null || request.sanctions().isEmpty())
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanctions",
                    "At least one warning sanction must be configured.");

        if (request.sanctions().size() > 25)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "too_many_warning_sanctions",
                    "No more than 25 warning sanctions can be configured.");

        for (WarningSanctionPayload sanction : request.sanctions()) {
            WarningSanctionAction action = WarningSanctionAction.fromKey(sanction.type());
            if (action == null)
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanction_type",
                        "One of the warning sanctions had an invalid action type.");

            if (sanction.warningCount() <= 0 || sanction.warningCount() > 100)
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanction_threshold",
                        "Warning sanction thresholds must be between 1 and 100.");

            if ((action == WarningSanctionAction.TIMEOUT || action == WarningSanctionAction.TEMPBAN)
                    && (sanction.durationMinutes() <= 0 || sanction.durationMinutes() > 40320L)) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanction_duration",
                        "Timeout and temporary ban sanctions must be between 1 minute and 28 days.");
            }

            if (action != WarningSanctionAction.TIMEOUT && action != WarningSanctionAction.TEMPBAN && sanction.durationMinutes() != 0L) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanction_duration",
                        "Only timeout and temporary ban sanctions can define a duration.");
            }

            if ((action == WarningSanctionAction.BAN || action == WarningSanctionAction.TEMPBAN)
                    && (sanction.deleteMessageDays() < 0 || sanction.deleteMessageDays() > 7)) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanction_delete_days",
                        "Ban and temporary ban sanctions can only delete between 0 and 7 days of messages.");
            }

            if (action != WarningSanctionAction.BAN && action != WarningSanctionAction.TEMPBAN && sanction.deleteMessageDays() != 0) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_warning_sanction_delete_days",
                        "Only ban and temporary ban sanctions can define deleted message days.");
            }
        }
    }

    private record ResolvedUser(String displayName, String avatarUrl) {
    }
}
