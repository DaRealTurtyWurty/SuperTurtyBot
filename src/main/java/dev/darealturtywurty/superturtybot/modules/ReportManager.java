package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Report;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.List;

public class ReportManager {
    private static final String REPORT_MESSAGE =
            "%s has been reported for `%s` by %s. Please review the report and " + "take appropriate action.";

    public static Report reportUser(Guild guild, User reported, User reporter, String reason) {
        if (guild == null) {
            throw new IllegalArgumentException("No guild context available.");
        }

        if (reported == null) {
            throw new IllegalArgumentException("Reported user was missing.");
        }

        if (reporter == null) {
            throw new IllegalArgumentException("Reporter user was missing.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Report reason was empty.");
        }

        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config != null && config.getModLogging() != 0L) {
            TextChannel modLogging = guild.getTextChannelById(config.getModLogging());
            if (modLogging != null) {
                modLogging.sendMessage(
                                REPORT_MESSAGE.formatted(reported.getAsMention(), truncate(reason, 1720),
                                        reporter.getAsMention()))
                        .queue();
            }
        }

        Report report = new Report(guild.getIdLong(), reported.getIdLong(), reporter.getIdLong(), reason);
        Database.getDatabase().reports.insertOne(report);
        return report;
    }

    public static String truncate(String str, int maxLength) {
        if (str == null || str.isBlank()) return str;
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    public static List<Report> getReports(Guild guild, User user) {
        if (guild == null || user == null)
            return List.of();

        return Database.getDatabase().reports.find(
                               Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("reported",
                                       user.getIdLong())))
                                             .into(new ArrayList<>());
    }
}
