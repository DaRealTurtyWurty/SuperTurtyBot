package dev.darealturtywurty.superturtybot.commands.core.privacy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyRuntimeCleanup;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyPreferences;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PrivacyOptInSubcommand extends SubcommandCommand {
    private final PrivacyOperations operations;

    public PrivacyOptInSubcommand(PrivacyOperations operations) {
        super("opt-in", "Enable optional features and remove your identifier from the opt-out list");
        this.operations = operations;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        if (!operations.tryBegin(userId)) {
            reply(event, "A privacy operation is already running. Please wait and try again.", false, true);
            return;
        }
        event.deferReply(true).queue(hook -> Thread.ofVirtual().name("privacy-opt-in").start(() -> {
            boolean saved = false;
            try {
                PrivacyPreferences.setOptedOut(Database.getDatabase().mongoDatabase, userId, false);
                saved = true;
                PrivacyRuntimeCleanup.resumeUser(userId);
                hook.editOriginal("**Optional features are enabled again.** Your identifier has been removed from "
                    + "the opt-out list. Using enabled features can create or update your records. "
                    + "Future reminders have been resumed; overdue reminders will not be replayed. "
                    + "Previously deleted records and progress are not restored.").queue();
            } catch (RuntimeException exception) {
                hook.editOriginal(saved
                    ? "Your opt-in was saved, but reminder scheduling could not finish. Optional features are enabled. "
                        + "Review your reminders with `/remindme list` or email turtybot@gmail.com."
                    : "Your preference could not be saved. Please try again or email turtybot@gmail.com.").queue();
            } finally {
                operations.finish(userId);
            }
        }), error -> operations.finish(userId));
    }
}
