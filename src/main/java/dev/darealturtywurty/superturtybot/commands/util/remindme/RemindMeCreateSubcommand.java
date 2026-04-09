package dev.darealturtywurty.superturtybot.commands.util.remindme;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;

public class RemindMeCreateSubcommand extends RemindMeSubcommand {
    public RemindMeCreateSubcommand() {
        super("create", "Creates a reminder");
        addOptions(
                new OptionData(OptionType.STRING, "duration", "When to remind you, like 10m, 2h30m, or 3d", true)
                        .setAutoComplete(true),
                new OptionData(OptionType.STRING, "reminder", "What to remind you about", true)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String durationInput = event.getOption("duration", "", OptionMapping::getAsString);
        String reminderText = event.getOption("reminder", "", OptionMapping::getAsString);
        if (reminderText == null || reminderText.isBlank()) {
            reply(event, "❌ You must provide something to be reminded about!", false, true);
            return;
        }

        if (reminderText.length() > 500) {
            reply(event, "❌ Reminder text cannot be longer than 500 characters!", false, true);
            return;
        }

        long durationMillis;
        try {
            durationMillis = parseDurationMillis(durationInput);
        } catch (IllegalArgumentException exception) {
            reply(event, "❌ " + exception.getMessage(), false, true);
            return;
        }

        long guildId = event.getGuild() == null ? 0L : event.getGuild().getIdLong();
        long channelId = event.getGuild() == null ? 0L : event.getChannel().getIdLong();
        Reminder reminder = ReminderManager.createReminder(
                guildId,
                event.getUser().getIdLong(),
                channelId,
                reminderText,
                System.currentTimeMillis() + durationMillis);

        String destination = event.getGuild() == null ? "your DMs" : event.getChannel().getAsMention();
        reply(event,
                "✅ Reminder `%s` set for %s. I'll remind you in %s.".formatted(
                        reminder.getId(),
                        TimeFormat.RELATIVE.format(reminder.getTime()),
                        destination),
                false,
                true);
    }
}
