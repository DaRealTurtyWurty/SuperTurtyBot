package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class UptimeCommand extends CoreCommand {
    public UptimeCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Shows the bot's uptime";
    }

    @Override
    public String getName() {
        return "uptime";
    }

    @Override
    public String getRichName() {
        return "Uptime";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        long uptime = System.currentTimeMillis() - TurtyBot.START_TIME;
        String formatted = millisecondsFormatted(uptime);

        reply(event, "The bot has been running for `" + formatted + "`!");
    }

    public static String millisecondsFormatted(long millis) {
        // get years, months, weeks, days, hours, minutes, seconds and milliseconds
        long years = millis / 31536000000L;
        if (years > 0) millis -= years * 31536000000L;

        long months = millis / 2628000000L;
        if (months > 0) millis -= months * 2628000000L;

        long weeks = millis / 604800000L;
        if (weeks > 0) millis -= weeks * 604800000L;

        long days = millis / 86400000L;
        if (days > 0) millis -= days * 86400000L;

        long hours = millis / 3600000L;
        if (hours > 0) millis -= hours * 3600000L;

        long minutes = millis / 60000L;
        if (minutes > 0) millis -= minutes * 60000L;

        long seconds = millis / 1000L;
        if (seconds > 0) millis -= seconds * 1000L;

        // build the string
        var sb = new StringBuilder();
        if (years > 0) sb.append(years).append(" year").append(years == 1 ? "" : "s").append(", ");
        if (months > 0) sb.append(months).append(" month").append(months == 1 ? "" : "s").append(", ");
        if (weeks > 0) sb.append(weeks).append(" week").append(weeks == 1 ? "" : "s").append(", ");
        if (days > 0) sb.append(days).append(" day").append(days == 1 ? "" : "s").append(", ");
        if (hours > 0) sb.append(hours).append(" hour").append(hours == 1 ? "" : "s").append(", ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes == 1 ? "" : "s").append(", ");
        if (seconds > 0) sb.append(seconds).append(" second").append(seconds == 1 ? "" : "s").append(", ");
        if (millis > 0) sb.append(millis).append(" millisecond").append(millis == 1 ? "" : "s");

        String asStr = sb.toString();
        if (asStr.endsWith(", ")) asStr = asStr.substring(0, asStr.length() - 2);

        return asStr;
    }
}
