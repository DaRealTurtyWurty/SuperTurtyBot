package dev.darealturtywurty.superturtybot.commands.core.privacy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import dev.darealturtywurty.superturtybot.Environment;
import java.net.URI;

public final class PrivacyInfoSubcommand extends SubcommandCommand {
    public PrivacyInfoSubcommand() {
        super("info", "Read how your data is used and how to request access or deletion");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String url = policyUrl(Environment.INSTANCE.dashboardSiteUrl().orElse(""));
        String policy = url == null
            ? "Contact **turtybot@gmail.com** for the full policy."
            : "Read the [privacy policy](" + url + ").";
        reply(event, "**Your privacy with TurtyBot**\n" + policy + "\n\n"
            + "The bot stores Discord identifiers, settings, messages needed for enabled features, moderation and "
            + "modmail records, and game/economy progress. Optional AI replies send conversation content to OpenAI.\n\n"
            + "**Export:** `/privacy export` privately downloads your structured database records. Other people's "
            + "identities, staff-only moderation text and messages are excluded. Email us for a wider access review.\n\n"
            + "**Deletion:** `/privacy delete` asks for confirmation, removes supported database records and clears "
            + "relevant caches. A request is saved for operator review of external copies, active restrictions and "
            + "other remaining records. It does not unban you. Future activity may create new records; this is not "
            + "an opt-out from future collection.\n\n"
            + "Most stored feature records currently have no automatic expiry. nginx logs are kept for 90 days. "
            + "Dashboard sessions expire after seven days; local AI context expires after 30 minutes without access.\n\n"
            + "Questions, correction requests, or help: **turtybot@gmail.com**. "
            + "You do not need a server administrator's permission to use these commands.", false, true);
    }

    public static String policyUrl(String configured) {
        try {
            URI uri = URI.create(configured.trim());
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                return null;
            return configured.trim().replaceAll("/+$", "") + "/privacy";
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
