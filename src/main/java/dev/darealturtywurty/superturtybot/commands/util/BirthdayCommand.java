package dev.darealturtywurty.superturtybot.commands.util;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;

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
                        new OptionData(OptionType.INTEGER, "year", "The year of your birthday", true).setRequiredRange(calculateMinBirthYear(), calculateMaxBirthYear())
                ),
                new SubcommandData("view", "Views a user's birthday").addOptions(
                        new OptionData(OptionType.USER, "user", "The user to view the birthday of", false)
                )
        );
    }

    private static int calculateMinBirthYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) - 150;
    }

    private static int calculateMaxBirthYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) - 12;
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
            day = switch (month) {
                case 2 -> Math.min(day, 28);
                case 4, 6, 9, 11 -> Math.min(day, 30);
                default -> Math.min(day, 31);
            };

            if (year < calculateMinBirthYear() || year > calculateMaxBirthYear()) {
                reply(event, "❌ You must provide a valid year between " + calculateMinBirthYear() + " and " + calculateMaxBirthYear() + "!", false, true);
                return;
            }

            birthday = new Birthday(event.getUser().getIdLong(), day, month, year);
            Database.getDatabase().birthdays.insertOne(birthday);

            reply(event, "✅ Your birthday has been set to the " +
                    mapDay(day) + " of " + getMonth(month) + " " + year +
                    "! (" + TimeFormat.RELATIVE.format(calculateTimeOfNextBirthday(birthday)) + ")");
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
                            mapDay(birthday.getDay()) + " of " + getMonth(birthday.getMonth()) +
                            "! (" + TimeFormat.RELATIVE.format(calculateTimeOfNextBirthday(birthday)) + ") This year they will be " +
                            (Calendar.getInstance().get(Calendar.YEAR) - birthday.getYear()) + " years old!");
        }
    }

    private static String getMonth(int month) {
        return switch (month) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Unknown";
        };
    }

    private static String mapDay(int day) {
        return switch (day) {
            case 1, 21, 31 -> day + "st";
            case 2, 22 -> day + "nd";
            case 3, 23 -> day + "rd";
            default -> day + "th";
        };
    }

    public static long calculateTimeOfNextBirthday(Birthday birthday) {
        Calendar calendar = Calendar.getInstance();
        int day = birthday.getDay();
        int month = birthday.getMonth();
        int year = calendar.get(Calendar.YEAR);
        if (month < calendar.get(Calendar.MONTH) + 1 || (month == calendar.get(Calendar.MONTH) + 1 && day < calendar.get(Calendar.DAY_OF_MONTH))) {
            year++;
        }

        calendar.set(year, month - 1, day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }
}
