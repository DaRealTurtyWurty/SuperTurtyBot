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
import java.util.Optional;

public class ReportManager {
    private static final String REPORT_MESSAGE =
            "%s has been reported for `%s` by %s. Please review the report and " + "take appropriate action.";

    public static Optional<Report> reportUser(Guild guild, User reported, User reporter, String reason) {
        if (guild == null || reported == null || reporter == null || reason == null || reason.isBlank())
            return Optional.empty();

        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) return Optional.empty();

        TextChannel modLogging = guild.getTextChannelById(config.getModLogging());
        if (modLogging == null) return Optional.empty();

        modLogging.sendMessage(
                          REPORT_MESSAGE.formatted(reported.getAsMention(), truncate(reason, 1720),
                                  reporter.getAsMention()))
                  .queue();

        var report = new Report(guild.getIdLong(), reported.getIdLong(), reporter.getIdLong(), reason);
        Database.getDatabase().reports.insertOne(report);
        return Optional.of(report);
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
