package dev.darealturtywurty.superturtybot.commands.util;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue") // False positive
public class ReminderCommand extends CoreCommand {
    public ReminderCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("create", "Creates a reminder").addOptions(
                        new OptionData(OptionType.STRING, "reminder", "The reminder to set", true),
                        new OptionData(OptionType.STRING, "date", "The date to set the reminder for", true),
                        new OptionData(OptionType.STRING, "time", "The time to set the reminder for", true),
                        new OptionData(OptionType.CHANNEL, "channel", "The channel to send the reminder in", true),
                        new OptionData(OptionType.STRING, "message", "The message to send when the reminder is sent", false)
                ),
                new SubcommandData("delete", "Deletes a reminder").addOptions(
                        new OptionData(OptionType.STRING, "reminder", "The reminder to delete", true)
                ),
                new SubcommandData("list", "Lists all reminders"),
                new SubcommandData("clear", "Clears all reminders")
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Creates, deletes, lists, and clears reminders.";
    }

    @Override
    public String getName() {
        return "reminder";
    }

    @Override
    public String getRichName() {
        return "Reminder";
    }

    @Override
    public String getHowToUse() {
        return """
                /reminder create <reminder> <date> <time> <channel> [message]
                /reminder delete <reminder>
                /reminder list
                /reminder clear""";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if(!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in servers!", false, true);
            return;
        }

        String subcommand = event.getSubcommandName();
        if(subcommand == null || subcommand.isBlank()) {
            reply(event, "❌ You must provide a subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "create" -> runCreateReminder(event);
            case "delete" -> runDeleteReminder(event);
            case "list" -> runListReminders(event);
            case "clear" -> runClearReminders(event);
            default -> reply(event, "❌ You must provide a valid subcommand!", false, true);
        }
    }

    private void runCreateReminder(SlashCommandInteractionEvent event) {
        String reminder = event.getOption("reminder", "Unspecified", OptionMapping::getAsString);
        String date = event.getOption("date", null, OptionMapping::getAsString);
        String time = event.getOption("time", null, OptionMapping::getAsString);
        GuildChannelUnion channel = event.getOption("channel", null, OptionMapping::getAsChannel);
        String message = event.getOption("message", "{@user} your reminder for {reminder} has been reached!", OptionMapping::getAsString);

        if(date == null || date.isBlank()) {
            reply(event, "❌ You must provide a date!", false, true);
            return;
        }

        if(time == null || time.isBlank()) {
            reply(event, "❌ You must provide a time!", false, true);
            return;
        }

        if(channel == null || !channel.getType().isMessage()) {
            reply(event, "❌ You must provide a text channel!", false, true);
            return;
        }

        GuildMessageChannel textChannel = channel.asGuildMessageChannel();
        if(!textChannel.canTalk(event.getMember())) {
            reply(event, "❌ You cannot send messages in that channel!", false, true);
            return;
        }

        if(!textChannel.canTalk(event.getGuild().getSelfMember())) {
            reply(event, "❌ I cannot send messages in that channel!", false, true);
            return;
        }

        if(message == null || message.isBlank()) {
            reply(event, "❌ You must provide a message!", false, true);
            return;
        }

        long dateTime;
        try {
            dateTime = parseDateTime(date, time);
        } catch (IllegalArgumentException exception) {
            reply(event, "❌ " + exception.getMessage(), false, true);
            return;
        }

        Constants.LOGGER.info("Reminder date time: {}", dateTime);
        if(dateTime < System.currentTimeMillis()) {
            reply(event, "❌ You cannot set a reminder in the past!", false, true);
            return;
        }

        if(dateTime == Long.MAX_VALUE) {
            reply(event, "❌ You cannot set a reminder that far in the future!", false, true);
            return;
        }

        if (dateTime == System.currentTimeMillis()) {
            reply(event, "❌ You cannot set a reminder for right now (how on earth did you even manage this)!", false, true);
            return;
        }

        if(reminder.length() > 100) {
            reply(event, "❌ The reminder cannot be longer than 100 characters!", false, true);
            return;
        }

        if(message.length() > 2000) {
            reply(event, "❌ The message cannot be longer than 2000 characters!", false, true);
            return;
        }

        var reminderObject = new Reminder(event.getGuild().getIdLong(), event.getUser().getIdLong(), reminder, message, channel.getIdLong(), dateTime);
        Database.getDatabase().reminders.insertOne(reminderObject);
        reminderObject.schedule(event.getJDA());
        reply(event, "✅ Reminder set!", false, true);
    }

    private void runDeleteReminder(SlashCommandInteractionEvent event) {
        String reminder = event.getOption("reminder", "Unspecified", OptionMapping::getAsString);
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        Reminder reminderObj = Database.getDatabase().reminders.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("user", userId), Filters.eq("reminder", reminder))).first();
        if(reminderObj == null) {
            reply(event, "❌ You do not have a reminder with that name!", false, true);
            return;
        }

        if(reminderObj.cancel()) {
            reply(event, "✅ Reminder deleted!", false, true);
        } else {
            reply(event, "❌ Failed to delete reminder!", false, true);
        }
    }

    private void runListReminders(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        long guildId = event.getGuild().getIdLong();

        List<Reminder> reminders = Database.getDatabase().reminders.find(Filters.and(Filters.eq("guild", guildId), Filters.eq("user", userId))).into(new ArrayList<>());
        if(reminders.isEmpty()) {
            reply(event, "❌ You do not have any reminders!", false, true);
            return;
        }

        event.deferReply().queue();

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Reminder reminder : reminders) {
            String after = "\nChannel: <#" + reminder.getChannel() + ">\nTime: " + TimeFormat.RELATIVE.format(reminder.getTime());

            int lengthLeft = 2000 - after.length() - "Message: ".length();
            contents.field(reminder.getReminder(), "Message: " + reminder.getMessage().substring(0, lengthLeft - 3) + "..." + after, false);
        }

        var embed = new PaginatedEmbed.Builder(10, contents)
                .timestamp(Instant.now())
                .title(event.getUser().getName() + "'s Reminders")
                .description("Here are your reminders!")
                .color(Color.ORANGE)
                .build(event.getJDA());

        embed.send(event.getHook(), () -> reply(event, "❌ Failed to send reminders!", false, true));
    }

    private void runClearReminders(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        long guildId = event.getGuild().getIdLong();

        DeleteResult result = Database.getDatabase().reminders.deleteMany(Filters.and(Filters.eq("guild", guildId), Filters.eq("user", userId)));
        if(result.getDeletedCount() == 0) {
            reply(event, "❌ You do not have any reminders!", false, true);
            return;
        }

        reply(event, "✅ Deleted " + result.getDeletedCount() + " reminders!");
    }

    @SuppressWarnings("MagicConstant")
    public static long parseDateTime(String dateString, String timeString) {
        String[] dateParts = dateString.split("/");
        String[] timeParts = timeString.split(":");

        if(dateParts.length != 3) {
            throw new IllegalArgumentException("You must provide a valid date in the format `dd/mm/yyyy`!");
        }

        if(timeParts.length != 2) {
            throw new IllegalArgumentException("You must provide a valid time in the format `hh:mm`!");
        }

        int day, month, year, hour, minute;

        try {
            day = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]);
            year = Integer.parseInt(dateParts[2]);
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("You must provide a valid date in the format `dd/mm/yyyy` and a valid time in the format `hh:mm`!");
        }

        if(day < 1 || day > 31) {
            throw new IllegalArgumentException("You must provide a valid day!");
        }

        if(month < 1 || month > 12) {
            throw new IllegalArgumentException("You must provide a valid month!");
        }

        if(year < 2020) {
            throw new IllegalArgumentException("You must provide a year that is 2020 or later!");
        }

        if(hour < 0 || hour > 23) {
            throw new IllegalArgumentException("You must provide a valid hour!");
        }

        if(minute < 0 || minute > 59) {
            throw new IllegalArgumentException("You must provide a valid minute!");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long time = calendar.getTimeInMillis();
        if(time == Long.MAX_VALUE) {
            throw new IllegalArgumentException("You cannot set a reminder that far in the future!");
        }

        return time;
    }
}
