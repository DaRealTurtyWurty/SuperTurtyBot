package dev.darealturtywurty.superturtybot.commands.util.remindme;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RemindMeSubcommand extends SubcommandCommand {
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?i)(\\d+)\\s*(w(?:eeks?)?|d(?:ays?)?|h(?:ours?)?|m(?:in(?:ute)?s?|ins?)?|s(?:ec(?:ond)?s?|ecs?)?)");
    private static final long MIN_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final long MAX_DURATION_MILLIS = TimeUnit.DAYS.toMillis(365);

    protected RemindMeSubcommand(String name, String description) {
        super(name, description);
    }

    public static String summarizeReminder(String text) {
        if (text == null || text.isBlank())
            return "(empty reminder)";

        String normalized = text.replace('\n', ' ').trim();
        if (normalized.length() <= 80)
            return normalized;

        return normalized.substring(0, 77) + "...";
    }

    public static String formatReminderChoice(String id, String reminder) {
        String label = "[%s] %s".formatted(id == null ? "UNKNOWN" : id, summarizeReminder(reminder));
        if (label.length() <= 100)
            return label;

        return label.substring(0, 97) + "...";
    }

    public static long parseDurationMillis(String input) {
        if (input == null || input.isBlank())
            throw new IllegalArgumentException("You must provide a duration like `10m`, `2h30m`, or `3d`!");

        Matcher matcher = DURATION_PATTERN.matcher(input);
        long totalMillis = 0L;
        int lastEnd = 0;
        boolean matched = false;

        while (matcher.find()) {
            String between = input.substring(lastEnd, matcher.start());
            if (!between.isBlank() && !between.replace(",", "").isBlank())
                throw new IllegalArgumentException("Invalid duration! Use values like `10m`, `2h30m`, or `3d`.");

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
        if (!matched || (!trailing.isBlank() && !trailing.replace(",", "").isBlank()))
            throw new IllegalArgumentException("Invalid duration! Use values like `10m`, `2h30m`, or `3d`.");

        if (totalMillis < MIN_DURATION_MILLIS)
            throw new IllegalArgumentException("Reminders must be at least 5 seconds in the future!");

        if (totalMillis > MAX_DURATION_MILLIS)
            throw new IllegalArgumentException("Reminders cannot be more than 365 days away!");

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
