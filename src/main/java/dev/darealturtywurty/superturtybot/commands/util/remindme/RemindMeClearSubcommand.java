package dev.darealturtywurty.superturtybot.commands.util.remindme;

import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RemindMeClearSubcommand extends RemindMeSubcommand {
    public RemindMeClearSubcommand() {
        super("clear", "Deletes all of your reminders");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long deleted = ReminderManager.clearReminders(event.getUser().getIdLong());
        if (deleted == 0) {
            reply(event, "❌ You do not have any active reminders!", false, true);
            return;
        }

        reply(event, "✅ Deleted %d reminder%s.".formatted(deleted, deleted == 1 ? "" : "s"), false, true);
    }
}
