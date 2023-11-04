package dev.darealturtywurty.superturtybot.core.util;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;

import java.time.OffsetDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static int getDaysForMonth(int month) {
        return getDaysForMonth(month, Calendar.getInstance().get(Calendar.YEAR));
    }

    public static byte getDaysForMonth(int month, int year) {
        // if leap year
        if(Year.isLeap(year) && month == 2)
            return 29;
        else if (month < 1 || month > 12)
            return -1;

        return switch (month) {
            case 2 -> 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }

    public static int calculateMinBirthYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) - 150;
    }

    public static int calculateMaxBirthYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) - 12;
    }

    public static String mapMonth(int month) {
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

    public static String mapDay(int day) {
        return switch (day) {
            case 1, 21, 31 -> day + "st";
            case 2, 22 -> day + "nd";
            case 3, 23 -> day + "rd";
            default -> day + "th";
        };
    }

    // TODO: Ensure it works for leap years
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

    public static String formatTime(OffsetDateTime time) {
        return time.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static String millisecondsFormatted(final long millis) {
        final long hours = TimeUnit.MILLISECONDS.toHours(millis)
            - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        final String ret = String.format("%s%s%s", hours > 0 ? String.format("%02d", hours) + ":" : "",
            minutes > 0 ? String.format("%02d", minutes) + ":" : "00:",
            seconds > 0 ? String.format("%02d", seconds) : "00").trim();
        return ret.endsWith(":") ? ret.substring(0, ret.length() - 1) : ret;
    }

    public static String parseDate(final String[] parts) {
        final var deadlineStrBuilder = new StringBuilder();
        deadlineStrBuilder.append(parts[0]).append("-");
        deadlineStrBuilder.append(parts[1]).append("-");
        deadlineStrBuilder.append(parts[2]).append("T");
        if (parts.length == 3) {
            deadlineStrBuilder.append("00:00:00.000Z");
            return deadlineStrBuilder.toString();
        }

        if (parts.length >= 4) {
            deadlineStrBuilder.append(parts[3]).append(":");
            if (parts.length == 4) {
                deadlineStrBuilder.append("00:00.000Z");
                return deadlineStrBuilder.toString();
            }
        }

        if (parts.length >= 5) {
            deadlineStrBuilder.append(parts[4]).append(":");
            if (parts.length == 5) {
                deadlineStrBuilder.append("00.000Z");
                return deadlineStrBuilder.toString();
            }
        }

        if (parts.length >= 6) {
            deadlineStrBuilder.append(parts[5]).append(".");
            if (parts.length == 5) {
                deadlineStrBuilder.append("000Z");
                return deadlineStrBuilder.toString();
            }
        }

        if (parts.length >= 7) {
            deadlineStrBuilder.append(parts[6]).append("Z");
        }

        return deadlineStrBuilder.toString();
    }

    public static short getDayOfYear(int day, int month, int year) {
        short dayOfYear = 0;
        for (short i = 1; i < month; i++) {
            dayOfYear += getDaysForMonth(i, year);
        }

        dayOfYear += day;
        return dayOfYear;
    }
}
