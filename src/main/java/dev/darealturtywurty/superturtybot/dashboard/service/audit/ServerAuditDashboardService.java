package dev.darealturtywurty.superturtybot.dashboard.service.audit;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.modules.audit.ServerAuditService;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.bson.Document;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Stores the latest completed report for each server so Discord and the dashboard share the same audit. */
public final class ServerAuditDashboardService {
    private static final Set<Long> RUNNING = ConcurrentHashMap.newKeySet();

    public record Snapshot(
        String guildId,
        long generatedAtMs,
        List<ServerAuditService.Finding> findings,
        List<String> notes,
        List<ServerAuditService.HealthCheck> healthChecks
    ) {
    }

    public Snapshot latest(long guildId) {
        Document document = Database.getDatabase().mongoDatabase.getCollection("serverAudits")
            .find(Filters.eq("_id", Long.toString(guildId))).first();
        return document == null ? null : Constants.GSON.fromJson(document.getString("report"), Snapshot.class);
    }

    public Snapshot run(Guild guild) {
        long guildId = guild.getIdLong();
        if (!RUNNING.add(guildId))
            throw new DashboardApiException(HttpStatus.CONFLICT, "audit_running",
                "An audit is already running for this server. Try again shortly.");
        try {
            Snapshot previous = latest(guildId);
            if (previous != null && System.currentTimeMillis() - previous.generatedAtMs() < 30_000)
                throw new DashboardApiException(HttpStatus.TOO_MANY_REQUESTS, "audit_rate_limit",
                    "Wait 30 seconds between server audits.");
            ServerAuditService.Report report = new ServerAuditService().audit(guild);
            var snapshot = new Snapshot(guild.getId(), System.currentTimeMillis(), report.findings(), report.notes(),
                report.healthChecks());
            var document = new Document("_id", guild.getId()).append("report", Constants.GSON.toJson(snapshot));
            Database.getDatabase().mongoDatabase.getCollection("serverAudits")
                .replaceOne(Filters.eq("_id", guild.getId()), document, new ReplaceOptions().upsert(true));
            return snapshot;
        } finally {
            RUNNING.remove(guildId);
        }
    }

    public Guild requireManager(JDA jda, long guildId, long userId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "audit_guild_missing",
                "The bot is not connected to this server.");
        Member member;
        try {
            member = guild.retrieveMemberById(userId).useCache(false).timeout(5, TimeUnit.SECONDS).complete();
        } catch (RuntimeException exception) {
            throw new DashboardApiException(HttpStatus.FORBIDDEN, "audit_access_denied",
                "Server membership could not be verified. Try signing in again.");
        }
        if (!member.hasPermission(Permission.MANAGE_SERVER))
            throw new DashboardApiException(HttpStatus.FORBIDDEN, "audit_access_denied",
                "Manage Server permission is required for this dashboard action.");
        return guild;
    }

    public static String dashboardUrl(long guildId) {
        String configured = Environment.INSTANCE.dashboardSiteUrl().map(String::trim).orElse("");
        try {
            URI uri = URI.create(configured);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null)
                return null;
            return configured.replaceAll("/+$", "") + "/dashboard/" + guildId + "/audit";
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
