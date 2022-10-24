package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Report;
import dev.darealturtywurty.superturtybot.modules.ReportManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class ReportsCommand extends CoreCommand {
    public ReportsCommand() {
        super(new Types(true, false, false, false));
    }


    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to get reports for", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Get a list of reports for a user";
    }

    @Override
    public String getAccess() {
        return "Moderators (Ban Permission)";
    }

    @Override
    public String getName() {
        return "reports";
    }

    @Override
    public String getRichName() {
        return "User Reports";
    }

    @Override
    public String getHowToUse() {
        return "/reports [user]";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "This command can only be used in a server!", false, true);
            return;
        }

        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            reply(event, "You do not have permission to use this command!", false, true);
            return;
        }

        if (!event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            reply(event, "I do not have permission to use this command!", false, true);
            return;
        }

        User user = event.getOption("user").getAsUser();

        List<Report> reports = ReportManager.getReports(event.getGuild(), user);
        if (reports.isEmpty()) {
            reply(event, "There are no reports for this user!");
            return;
        }

        var builder = new StringBuilder();
        for (int index = 0; index < reports.size(); index++) {
            Report report = reports.get(index);
            User reporter = event.getJDA().getUserById(report.getReporter());
            builder.append(String.format("**%d.** %s - `%s`%n", index + 1, reporter.getAsMention(),
                    ReportManager.truncate(report.getReason(), 256)));
        }

        reply(event, String.format("Reports for %s:%n%s", user.getAsMention(), builder));
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
