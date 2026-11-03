package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.dashboard.service.audit.ServerAuditDashboardService;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ServerAuditCommand extends CoreCommand {
    public ServerAuditCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public String getName() {
        return "serveraudit";
    }

    @Override
    public String getRichName() {
        return "Server Audit";
    }

    @Override
    public String getDescription() {
        return "Checks bot permissions, settings, stale notifiers and dashboard/API health";
    }

    @Override
    public String getAccess() {
        return "Manage Server";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            reply(event, "This command can only be used in a server.", false, true);
            return;
        }

        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "You need Manage Server permission to run a server audit.", false, true);
            return;
        }

        event.deferReply(true)
            .queue(hook -> Thread.ofVirtual().name("server-audit").start(() -> {
                try {
                    ServerAuditDashboardService.Snapshot report = new ServerAuditDashboardService()
                        .run(event.getGuild());
                    String url = ServerAuditDashboardService.dashboardUrl(event.getGuild().getIdLong());
                    int count = report.findings().size();
                    var embed = new EmbedBuilder()
                        .setTitle("Server audit", url)
                        .setColor(count == 0 ? 0x38BDF8 : 0xFBBF24)
                        .setDescription(count == 0
                            ? "No issues found in the completed checks. Review check coverage in the dashboard."
                            : "Found **" + count
                                + " issue(s)** to review. Open the dashboard for details and repair guidance.")
                        .setFooter(event.getGuild().getName(), event.getGuild().getIconUrl())
                        .setTimestamp(Instant.ofEpochMilli(report.generatedAtMs()));
                    if (url == null) {
                        embed.addField("Dashboard link unavailable",
                            "Your report is saved. Ask the bot operator to configure the dashboard site URL.", false);
                    }
                    var response = hook.editOriginalEmbeds(embed.build()).setAllowedMentions(List.of());
                    if (url != null) {
                        response.setComponents(ActionRow.of(Button.link(url, "View server audit")));
                    }
                    response.queue();
                } catch (DashboardApiException exception) {
                    hook.editOriginal(exception.getMessage()).queue();
                } catch (Exception exception) {
                    Constants.LOGGER.error("Server audit failed for guild {}", event.getGuild().getIdLong(), exception);
                    hook.editOriginal("The audit could not complete. Ask the bot operator to check the logs.").queue();
                }
            }));
    }
}
