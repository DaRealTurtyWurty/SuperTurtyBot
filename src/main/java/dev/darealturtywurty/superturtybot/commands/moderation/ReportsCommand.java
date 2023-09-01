package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Report;
import dev.darealturtywurty.superturtybot.modules.ReportManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
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

        event.deferReply().queue();

        var contents = new PaginatedEmbed.ContentsBuilder();

        CompletableFuture<?> future = new CompletableFuture<>();
        for (int index = 0; index < reports.size(); index++) {
            Report report = reports.get(index);

            int finalIndex = index;
            event.getJDA().retrieveUserById(report.getReporter()).queue(reporter -> {
                contents.field(String.format("Reported by %s", reporter.getName()),
                        ReportManager.truncate(report.getReason(), 256));

                if (finalIndex == reports.size() - 1) {
                    future.complete(null);
                }
            });
        }

        future.thenRun(() -> {
            PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                    .title(String.format("Reports for %s", user.getName()))
                    .description(user.getName() + " has " + reports.size() + " reports")
                    .color(Color.RED)
                    .timestamp(Instant.now())
                    .footer("Requested by " + event.getUser().getName(), event.getMember().getEffectiveAvatarUrl())
                    .authorOnly(event.getUser().getIdLong())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ User has no reports!").queue());
        });
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
