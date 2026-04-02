package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemindMeCommand extends CoreCommand {
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?i)(\\d+)\\s*(w(?:eeks?)?|d(?:ays?)?|h(?:ours?)?|m(?:in(?:ute)?s?|ins?)?|s(?:ec(?:ond)?s?|ecs?)?)");
    private static final long MIN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final long MAX_DURATION_MILLIS = TimeUnit.DAYS.toMillis(365);
    private static final List<String> DURATION_SUGGESTIONS = List.of("10m", "30m", "1h", "2h30m", "1d", "1w");

    public RemindMeCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("create", "Creates a reminder").addOptions(
                        new OptionData(OptionType.STRING, "duration", "When to remind you, like 10m, 2h30m, or 3d", true)
                                .setAutoComplete(true),
                        new OptionData(OptionType.STRING, "reminder", "What to remind you about", true)
                ),
                new SubcommandData("list", "Lists your active reminders"),
                new SubcommandData("delete", "Deletes one of your reminders").addOptions(
                        new OptionData(OptionType.STRING, "id", "The reminder ID to delete", true).setAutoComplete(true)
                ),
                new SubcommandData("clear", "Deletes all of your reminders")
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Creates and manages reminders with simple relative durations.";
    }

    @Override
    public String getHowToUse() {
        return """
                /remindme create <duration> <reminder>
                /remindme list
                /remindme delete <id>
                /remindme clear""";
    }

    @Override
    public String getName() {
        return "remindme";
    }

    @Override
    public String getRichName() {
        return "Remind Me";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        super.onCommandAutoCompleteInteraction(event);
        if (!event.getName().equalsIgnoreCase(getName()))
            return;

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.replyChoices().queue();
            return;
        }

        String focusedName = event.getFocusedOption().getName();
        String focusedValue = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);

        if (subcommand.equalsIgnoreCase("create") && focusedName.equals("duration")) {
            List<String> suggestions = DURATION_SUGGESTIONS.stream()
                    .filter(choice -> focusedValue.isBlank() || choice.toLowerCase(Locale.ROOT).contains(focusedValue))
                    .limit(25)
                    .toList();
            event.replyChoiceStrings(suggestions).queue();
            return;
        }

        if (subcommand.equalsIgnoreCase("delete") && focusedName.equals("id")) {
            List<Command.Choice> choices = ReminderManager.getRemindersForUser(event.getUser().getIdLong()).stream()
                    .filter(reminder -> focusedValue.isBlank()
                            || safeLower(reminder.getId()).contains(focusedValue)
                            || safeLower(reminder.getReminder()).contains(focusedValue))
                    .limit(25)
                    .map(reminder -> new Command.Choice(formatReminderChoice(reminder), reminder.getId()))
                    .toList();
            event.replyChoices(choices).queue();
            return;
        }

        event.replyChoices().queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null || subcommand.isBlank()) {
            reply(event, "❌ You must provide a valid subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "create" -> runCreate(event);
            case "list" -> runList(event);
            case "delete" -> runDelete(event);
            case "clear" -> runClear(event);
            default -> reply(event, "❌ You must provide a valid subcommand!", false, true);
        }
    }

    private void runCreate(SlashCommandInteractionEvent event) {
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

    private void runList(SlashCommandInteractionEvent event) {
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

    private void runDelete(SlashCommandInteractionEvent event) {
        String id = event.getOption("id", "", OptionMapping::getAsString);
        if (id == null || id.isBlank()) {
            reply(event, "❌ You must provide a reminder ID!", false, true);
            return;
        }

        if (!ReminderManager.deleteReminder(event.getUser().getIdLong(), id)) {
            reply(event, "❌ You do not have a reminder with that ID!", false, true);
            return;
        }

        reply(event, "✅ Reminder `%s` deleted.".formatted(id.toUpperCase(Locale.ROOT)), false, true);
    }

    private void runClear(SlashCommandInteractionEvent event) {
        long deleted = ReminderManager.clearReminders(event.getUser().getIdLong());
        if (deleted == 0) {
            reply(event, "❌ You do not have any active reminders!", false, true);
            return;
        }

        reply(event, "✅ Deleted %d reminder%s.".formatted(deleted, deleted == 1 ? "" : "s"), false, true);
    }

    private static String formatReminderChoice(Reminder reminder) {
        String label = "[%s] %s".formatted(
                reminder.getId() == null ? "UNKNOWN" : reminder.getId(),
                summarizeReminder(reminder.getReminder()));
        if (label.length() <= 100)
            return label;

        return label.substring(0, 97) + "...";
    }

    private static String summarizeReminder(String text) {
        if (text == null || text.isBlank())
            return "(empty reminder)";

        String normalized = text.replace('\n', ' ').trim();
        if (normalized.length() <= 80)
            return normalized;

        return normalized.substring(0, 77) + "...";
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("You must provide a duration like `10m`, `2h30m`, or `3d`!");
        }

        Matcher matcher = DURATION_PATTERN.matcher(input);
        long totalMillis = 0L;
        int lastEnd = 0;
        boolean matched = false;

        while (matcher.find()) {
            String between = input.substring(lastEnd, matcher.start());
            if (!between.isBlank() && !between.replace(",", "").isBlank()) {
                throw new IllegalArgumentException("Invalid duration! Use values like `10m`, `2h30m`, or `3d`.");
            }

            long amount = Long.parseLong(matcher.group(1));
            try {
                totalMillis = Math.addExact(totalMillis, unitToMillis(amount, matcher.group(2)));
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("That duration is too large to store safely!");
            }
            lastEnd = matcher.end();
            matched = true;
        }

        String trailing = input.substring(lastEnd);
        if (!matched || (!trailing.isBlank() && !trailing.replace(",", "").isBlank())) {
            throw new IllegalArgumentException("Invalid duration! Use values like `10m`, `2h30m`, or `3d`.");
        }

        if (totalMillis < MIN_DURATION_MILLIS) {
            throw new IllegalArgumentException("Reminders must be at least 5 seconds in the future!");
        }

        if (totalMillis > MAX_DURATION_MILLIS) {
            throw new IllegalArgumentException("Reminders cannot be more than 365 days away!");
        }

        return totalMillis;
    }

    private static long unitToMillis(long amount, String unit) {
        String normalized = unit.toLowerCase(Locale.ROOT);
        return switch (normalized.charAt(0)) {
            case 'w' -> Math.multiplyExact(amount, TimeUnit.DAYS.toMillis(7));
            case 'd' -> Math.multiplyExact(amount, TimeUnit.DAYS.toMillis(1));
            case 'h' -> Math.multiplyExact(amount, TimeUnit.HOURS.toMillis(1));
            case 'm' -> Math.multiplyExact(amount, TimeUnit.MINUTES.toMillis(1));
            case 's' -> Math.multiplyExact(amount, TimeUnit.SECONDS.toMillis(1));
            default -> throw new IllegalArgumentException("Unsupported duration unit: " + unit);
        };
    }
}
