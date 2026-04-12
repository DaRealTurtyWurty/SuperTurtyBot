package dev.darealturtywurty.superturtybot.dashboard.service.reports;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Report;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.List;

public final class ReportsService {
    private final JDA jda;

    public ReportsService(JDA jda) {
        this.jda = jda;
    }

    public DashboardReportHistoryResponse getUserReports(long guildId, long userId) {
        Guild guild = requireGuild(guildId);
        return new DashboardReportHistoryResponse(
                toUserSummary(guild, userId),
                listReports(guildId, guild, userId)
        );
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private List<DashboardReportRecord> listReports(long guildId, Guild guild, long userId) {
        return Database.getDatabase().reports.find(Filters.and(
                        Filters.eq("guild", guildId),
                        Filters.eq("reported", userId)))
                .sort(Sorts.descending("reportedAt"))
                .into(new ArrayList<>())
                .stream()
                .map(report -> toRecord(guild, report))
                .toList();
    }

    private DashboardReportRecord toRecord(Guild guild, Report report) {
        ResolvedUser reporter = resolveUser(guild, report.getReporter());
        return new DashboardReportRecord(
                Long.toString(report.getReporter()),
                reporter.displayName(),
                reporter.avatarUrl(),
                report.getReason(),
                report.getReportedAt()
        );
    }

    private DashboardReportUserSummary toUserSummary(Guild guild, long userId) {
        ResolvedUser user = resolveUser(guild, userId);
        return new DashboardReportUserSummary(Long.toString(userId), user.displayName(), user.avatarUrl());
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

    private record ResolvedUser(String displayName, String avatarUrl) {
    }
}
