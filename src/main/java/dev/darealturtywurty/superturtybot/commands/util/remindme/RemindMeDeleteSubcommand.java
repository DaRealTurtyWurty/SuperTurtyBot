package dev.darealturtywurty.superturtybot.commands.util.remindme;

import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Locale;

public class RemindMeDeleteSubcommand extends RemindMeSubcommand {
    public RemindMeDeleteSubcommand() {
        super("delete", "Deletes one of your reminders");
        addOption(new OptionData(OptionType.STRING, "id", "The reminder ID to delete", true).setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        String id = event.getOption("id", "", OptionMapping::getAsString);
        if (id == null || id.isBlank()) {
            reply(event, "❌ You must provide a reminder ID!", false, true);
            return;
        }

        if (!ReminderManager.deleteReminder(event.getGuild().getIdLong(), event.getUser().getIdLong(), id)) {
            reply(event, "❌ You do not have a reminder with that ID!", false, true);
            return;
        }

        reply(event, "✅ Reminder `%s` deleted.".formatted(id.toUpperCase(Locale.ROOT)), false, true);
    }
}
