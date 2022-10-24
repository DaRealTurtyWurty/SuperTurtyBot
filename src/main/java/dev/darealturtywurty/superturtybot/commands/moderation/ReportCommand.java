package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Report;
import dev.darealturtywurty.superturtybot.modules.ReportManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Optional;

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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "You can only use this command in a server!", false, true);
            return;
        }

        User user = event.getOption("user").getAsUser();
        User reporter = event.getUser();
        String reason = event.getOption("reason").getAsString();
        Optional<Report> report = ReportManager.reportUser(event.getGuild(), user, reporter, reason);
        report.ifPresentOrElse(ignored -> {
            reply(event,
                    "Successfully reported " + user.getAsTag() + " for `" + ReportManager.truncate(reason, 1720) + "`");
        }, () -> {
            reply(event, "Failed to report " + user.getAsTag() + " for `" + ReportManager.truncate(reason, 1720) + "1",
                    false, true);
        });
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
