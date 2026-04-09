package dev.darealturtywurty.superturtybot.commands.util.remindme;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RemindMeCommand extends CoreCommand {
    private static final List<String> DURATION_SUGGESTIONS = List.of("10m", "30m", "1h", "2h30m", "1d", "1w");

    public RemindMeCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new RemindMeCreateSubcommand(),
                new RemindMeListSubcommand(),
                new RemindMeDeleteSubcommand(),
                new RemindMeClearSubcommand()
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
    public boolean isServerOnly() {
        return true;
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
                    .map(reminder -> new Command.Choice(
                            RemindMeSubcommand.formatReminderChoice(reminder.getId(), reminder.getReminder()),
                            reminder.getId()))
                    .toList();
            event.replyChoices(choices).queue();
            return;
        }

        event.replyChoices().queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, "❌ You must provide a valid subcommand!", false, true);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
