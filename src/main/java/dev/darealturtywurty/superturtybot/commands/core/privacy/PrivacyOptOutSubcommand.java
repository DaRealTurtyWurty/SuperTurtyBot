package dev.darealturtywurty.superturtybot.commands.core.privacy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyPreferences;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyRuntimeCleanup;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PrivacyOptOutSubcommand extends SubcommandCommand {
    private final PrivacyOperations operations;

    public PrivacyOptOutSubcommand(PrivacyOperations operations) {
        super("opt-out", "Disable optional commands and data collection across all servers");
        this.operations = operations;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        if (!operations.tryBegin(userId)) {
            reply(event, "A privacy operation is already running. Please wait and try again.", false, true);
            return;
        }
        event.deferReply(true).queue(hook -> Thread.ofVirtual().name("privacy-opt-out").start(() -> {
            boolean saved = false;
            try {
                PrivacyPreferences.setOptedOut(Database.getDatabase().mongoDatabase, userId, true);
                saved = true;
                PrivacyRuntimeCleanup.pauseUser(userId);
                hook.editOriginal("**You're opted out of optional data processing across all servers.**\n\n"
                    + "Most commands, games, levelling, AI replies and other optional features are disabled for you. "
                    + "Scheduled reminders are paused and dashboard sessions are signed out. Server safety moderation "
                    + "and handling privacy requests can still involve your data.\n\n"
                    + "We keep only your Discord user ID in the opt-out list to honour this choice. Existing feature "
                    + "records are not erased by opting out; use `/privacy delete` for deletion.\n\n"
                    + "Your privacy commands remain available. Use `/privacy opt-in` to enable optional features again.")
                    .queue();
            } catch (RuntimeException exception) {
                hook.editOriginal(saved
                    ? "Your opt-out was saved, but some cache or reminder cleanup could not finish. "
                        + "Optional features remain disabled. Email turtybot@gmail.com if you need help."
                    : "Your preference could not be saved. Please try `/privacy opt-out` again or email turtybot@gmail.com.")
                    .queue();
            } finally {
                operations.finish(userId);
            }
        }), error -> operations.finish(userId));
    }
}
