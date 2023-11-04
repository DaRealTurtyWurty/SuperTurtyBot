package dev.darealturtywurty.superturtybot.commands.util;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;
import dev.darealturtywurty.superturtybot.modules.BirthdayManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BirthdayCommand extends CoreCommand {
    public BirthdayCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("set", "Sets your birthday").addOptions(
                        new OptionData(OptionType.INTEGER, "day", "The day of your birthday", true, true).setRequiredRange(1, 31),
                        new OptionData(OptionType.INTEGER, "month", "The month of your birthday", true).setRequiredRange(1, 12),
                        new OptionData(OptionType.INTEGER, "year", "The year of your birthday", true).setRequiredRange(TimeUtils.calculateMinBirthYear(), TimeUtils.calculateMaxBirthYear())
                ),
                new SubcommandData("view", "Views a user's birthday").addOptions(
                        new OptionData(OptionType.USER, "user", "The user to view the birthday of", false)
                )
        );
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if(!event.getName().equals(getName()))
            return;

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.replyChoices().queue();
        } else if (subcommand.equalsIgnoreCase("set")) {
            AutoCompleteQuery selected = event.getFocusedOption();
            if(!selected.getName().equals("day"))
                return;

            int month = event.getOption("month", 1, OptionMapping::getAsInt);
            String day = selected.getValue();

            List<Integer> validForMonth = new ArrayList<>();
            for (int index = 0; index < TimeUtils.getDaysForMonth(month); index++) {
                validForMonth.add(index + 1);
            }

            validForMonth.removeIf(s -> !Integer.toString(s).startsWith(day));
            List<Command.Choice> choices = validForMonth.stream().limit(25).map(integer -> new Command.Choice(Integer.toString(integer), integer)).toList();
            event.replyChoices(choices).queue();
        }
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Set your birthday or view someone else's birthday!";
    }

    @Override
    public String getName() {
        return "birthday";
    }

    @Override
    public String getRichName() {
        return "Birthday";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must provide a subcommand!", false, true);
            return;
        }

        if (subcommand.equalsIgnoreCase("set")) {
            Birthday birthday = Database.getDatabase().birthdays.find(Filters.eq("user", event.getUser().getIdLong())).first();
            if (birthday != null && event.getUser().getIdLong() != Environment.INSTANCE.ownerId().orElse(0L)) {
                reply(event, "❌ You have already set your birthday to " + birthday.getDay() + " of " + birthday.getMonth() + "!", false, true);
                return;
            }

            int day = event.getOption("day", 1, OptionMapping::getAsInt);
            int month = event.getOption("month", 1, OptionMapping::getAsInt);
            int year = event.getOption("year", Calendar.getInstance().get(Calendar.YEAR), OptionMapping::getAsInt);
            if (day < 1 || day > TimeUtils.getDaysForMonth(month, year)) {
                reply(event, "❌ You must provide a valid day for the month of " + TimeUtils.mapMonth(month) + "!", false, true);
                return;
            }

            if (year < TimeUtils.calculateMinBirthYear() || year > TimeUtils.calculateMaxBirthYear()) {
                reply(event, "❌ You must provide a valid year between " + TimeUtils.calculateMinBirthYear() + " and " + TimeUtils.calculateMaxBirthYear() + "!", false, true);
                return;
            }

            birthday = new Birthday(event.getUser().getIdLong(), day, month, year);
            Database.getDatabase().birthdays.insertOne(birthday);

            short dayOfYear = TimeUtils.getDayOfYear(day, month, year);
            BirthdayManager.addBirthday(birthday.getUser(), dayOfYear);

            reply(event, "✅ Your birthday has been set to the " +
                    TimeUtils.mapDay(day) + " of " + TimeUtils.mapMonth(month) + " " + year +
                    "! (" + TimeFormat.RELATIVE.format(TimeUtils.calculateTimeOfNextBirthday(birthday)) + ")");
        } else if (subcommand.equalsIgnoreCase("view")) {
            User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
            if(user == null) {
                reply(event, "❌ You must provide a user!", false, true);
                return;
            }

            Birthday birthday = Database.getDatabase().birthdays.find(Filters.eq("user", user.getIdLong())).first();
            if (birthday == null) {
                reply(event, "❌ That user has not set their birthday yet!", false, true);
                return;
            }

            reply(event,
                    "🎂 " + user.getAsMention() + "'s birthday is on the " +
                            TimeUtils.mapDay(birthday.getDay()) + " of " + TimeUtils.mapMonth(birthday.getMonth()) +
                            "! (" + TimeFormat.RELATIVE.format(TimeUtils.calculateTimeOfNextBirthday(birthday)) + ") This year they will be " +
                            (Calendar.getInstance().get(Calendar.YEAR) - birthday.getYear()) + " years old!");
        }
    }

}
