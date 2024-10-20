package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Report;
import dev.darealturtywurty.superturtybot.modules.ReportManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ReportCommand extends CoreCommand {
    public ReportCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to report", true),
                new OptionData(OptionType.STRING, "reason", "The reason for the report", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Report a user for breaking the rules";
    }

    @Override
    public String getName() {
        return "report";
    }

    @Override
    public String getRichName() {
        return "Report User";
    }

    @Override
    public String getHowToUse() {
        return "/report [user] [reason]";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You can only use this command in a server!", false, true);
            return;
        }

        User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        User reporter = event.getUser();

        if (user.isBot()) {
            reply(event, "❌ You cannot report a bot!", false, true);
            return;
        }

        if(user.isSystem()) {
            reply(event, "❌ You cannot report a system user!", false, true);
            return;
        }

        if (user.getIdLong() == reporter.getIdLong()) {
            reply(event, "❌ You cannot report yourself!", false, true);
            return;
        }

        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        Optional<Report> report = ReportManager.reportUser(event.getGuild(), user, reporter, reason);
        report.ifPresentOrElse(
                ignored -> reply(event,
                        "✅ Successfully reported " + user.getEffectiveName() + " for `" + ReportManager.truncate(reason, 1720) + "`"),
                () -> reply(event,
                        "❌ Failed to report " + user.getEffectiveName() + " for `" + ReportManager.truncate(reason, 1720) + "1",
                        false,
                        true)
        );
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
