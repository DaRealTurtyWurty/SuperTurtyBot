package dev.darealturtywurty.superturtybot.commands.core.privacy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyPreferences;
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
        String status = PrivacyPreferences.isOptedOut(event.getUser().getIdLong()) ? "Opted out" : "Enabled";
        reply(event, "**Your privacy with TurtyBot**\n" + policy + "\nOptional data processing: **" + status + "**.\n\n"
            + "The bot stores Discord identifiers, settings, messages needed for enabled features, moderation and "
            + "modmail records, and game/economy progress. Optional AI replies send conversation content to OpenAI.\n\n"
            + "**Export:** `/privacy export` privately downloads your structured database records. Other people's "
            + "identities, staff-only moderation text and messages are excluded. Email us for a wider access review.\n\n"
            + "**Deletion:** `/privacy delete` asks for confirmation, removes supported database records and clears "
            + "relevant caches. A request is saved for operator review of external copies, active restrictions and "
            + "other remaining records. Deletion also opts you out to prevent profile recreation.\n\n"
            + "**Opt-out:** `/privacy opt-out` disables most commands and background features across all servers. "
            + "We keep only your Discord user ID in the opt-out list. Existing records remain until deleted. "
            + "Server safety moderation and privacy handling still apply. Use `/privacy opt-in` to enable optional "
            + "features again. Privacy commands always remain available.\n\n"
            + "Daily cleanup removes optional profiles after 12 months of inactivity, closed modmail and "
            + "expired moderation records after 90 days, and resolved privacy requests after 30 days. "
            + "Active restrictions and unresolved requests remain. nginx logs are kept for 90 days. "
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
