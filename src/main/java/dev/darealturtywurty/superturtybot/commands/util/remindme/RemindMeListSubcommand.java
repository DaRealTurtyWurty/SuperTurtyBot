package dev.darealturtywurty.superturtybot.commands.util.remindme;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.List;

public class RemindMeListSubcommand extends RemindMeSubcommand {
    public RemindMeListSubcommand() {
        super("list", "Lists your active reminders");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        List<Reminder> reminders = ReminderManager.getRemindersForUser(event.getUser().getIdLong());
        if (reminders.isEmpty()) {
            reply(event, "❌ You do not have any active reminders!", false, true);
            return;
        }

        StringBuilder builder = new StringBuilder("Here are your active reminders:\n");
        int shown = 0;
        for (Reminder reminder : reminders) {
            String line = "`%s` %s | %s | %s\n".formatted(
                    reminder.getId(),
                    summarizeReminder(reminder.getReminder()),
                    TimeFormat.RELATIVE.format(reminder.getTime()),
                    reminder.getChannel() == 0 ? "DMs" : "<#" + reminder.getChannel() + ">");
            if (builder.length() + line.length() > 1900)
                break;

            builder.append(line);
            shown++;
        }

        if (shown < reminders.size()) {
            builder.append("...and ").append(reminders.size() - shown).append(" more.");
        }

        reply(event, builder.toString(), false, true);
    }
}
