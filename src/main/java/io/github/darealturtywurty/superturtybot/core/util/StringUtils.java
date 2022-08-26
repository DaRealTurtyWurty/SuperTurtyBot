package io.github.darealturtywurty.superturtybot.core.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import com.vdurmont.emoji.EmojiParser;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public final class StringUtils {
    private static final char[] CHARS = { 'k', 'm', 'b', 't' };
    
    private StringUtils() {
        throw new IllegalAccessError("This is illegal, expect police at your door in 2-5 minutes!");
    }

    public static String convertOnlineStatus(OnlineStatus status) {
        return switch (status) {
            case DO_NOT_DISTURB -> "Do Not Disturb";
            case IDLE -> "Idle";
            case INVISIBLE -> "Invisible";
            case OFFLINE -> "Offline";
            case ONLINE -> "Online";
            case UNKNOWN -> "Unknown";
            default -> "Undefined";
        };
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
    
    /**
     * Recursive implementation, invokes itself for each factor of a thousand, increasing the class on each invokation.
     *
     * @param  n         the number to format
     * @param  iteration in fact this is the class from the array c
     * @return           a String representing the number n formatted in a cool looking way.
     */
    public static String numberFormat(final double n, final int iteration) {
        if (n < 1000)
            return String.valueOf(n);
        final double d = (long) n / 100 / 10D;
        final boolean isRound = d * 10 % 10 == 0;// true if the decimal part is equal to 0 (then it's trimmed
                                                 // anyway)
        return d < 1000 ? // this determines the class, i.e. 'k', 'm' etc
            (d > 99.9D || isRound && d > 9.99D ? // this decides whether to trim the decimals
                (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
            ) + "" + CHARS[iteration] : numberFormat(d, iteration + 1);
    }

    public static String parseDate(final String[] parts) {
        final var deadlineStrBuilder = new StringBuilder();
        deadlineStrBuilder.append(parts[0] + "-");
        deadlineStrBuilder.append(parts[1] + "-");
        deadlineStrBuilder.append(parts[2] + "T");
        if (parts.length == 3) {
            deadlineStrBuilder.append("00:00:00.000Z");
            return deadlineStrBuilder.toString();
        }

        if (parts.length >= 4) {
            deadlineStrBuilder.append(parts[3] + ":");
            if (parts.length == 4) {
                deadlineStrBuilder.append("00:00.000Z");
                return deadlineStrBuilder.toString();
            }
        }

        if (parts.length >= 5) {
            deadlineStrBuilder.append(parts[4] + ":");
            if (parts.length == 5) {
                deadlineStrBuilder.append("00.000Z");
                return deadlineStrBuilder.toString();
            }
        }

        if (parts.length >= 6) {
            deadlineStrBuilder.append(parts[5] + ".");
            if (parts.length == 5) {
                deadlineStrBuilder.append("000Z");
                return deadlineStrBuilder.toString();
            }
        }

        if (parts.length >= 7) {
            deadlineStrBuilder.append(parts[6] + "Z");
        }

        return deadlineStrBuilder.toString();
    }

    public static boolean readBoolean(String str) {
        final String mod = str.trim().toLowerCase();
        return switch (mod) {
            case "y", "ye", "yes", "yea", "yeah", "true", "1" -> true;
            case "n", "nope", "no", "nah", "ne", "false", "0" -> false;
            default -> false;
        };
    }
    
    public static @NotNull Function<OptionMapping, String> readEmoji(JDA jda, Guild guild) {
        return option -> {
            final String string = option.getAsString();
            
            final List<String> emojis = EmojiParser.extractEmojis(string);
            final List<Long> emotes = MentionType.EMOJI.getPattern().matcher(string).results()
                .map(result -> string.substring(result.start(), result.end())).map(str -> {
                    final String[] parts = str.split(":");
                    if (parts.length < 1)
                        return 0L;
                    
                    final String id = parts[parts.length - 1].replace(">", "").replace("<", "");
                    try {
                        return Long.parseLong(id);
                    } catch (final NumberFormatException exception) {
                        return 0L;
                    }
                }).toList();
            if (emojis.isEmpty()) {
                if (emotes.isEmpty())
                    return null;
                
                final RichCustomEmoji emote = jda.getEmojiById(emotes.get(0));
                if (emote == null || emote.getGuild().getMemberById(guild.getSelfMember().getIdLong()) == null
                    || !emote.getGuild().getMemberById(guild.getSelfMember().getIdLong()).canInteract(emote))
                    return null;
                
                return emote.getAsMention();
            }
            
            return Emoji.fromFormatted(emojis.get(0)).getName();
        };
    }
    
    public static @NotNull Function<OptionMapping, Role> readRole() {
        return option -> {
            try {
                return option.getAsRole();
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        };
    }

    public static String replaceHTMLCodes(String str) {
        return str.replace("&amp;", "&").replace("&copy;", "©").replace("&trade;", "™").replace("&thinsp;", "\u2009")
            .replace("&ensp;", "\u2002").replace("&emsp;", "\u2003").replace("&hearts;", "♥").replace("&star;", "☆")
            .replace("&starf;", "★").replace("&bigstar;", "★").replace("&euro;", "€").replace("&mdash;", "—")
            .replace("&ndash;", "–");
    }
    
    public static String stripEmote(String emote) {
        return emote.replace("<:", "").replace("<a:", "").replace(">", "");
    }
    
    public static String trueFalseToYesNo(final boolean value) {
        return value ? "Yes" : "No";
    }
    
    public static String truncateString(String str, int length) {
        if (str.length() > length)
            return str.substring(0, length - 3) + "...";
        return str;
    }
    
    public static String upperSnakeToSpacedPascal(String str) {
        return WordUtils.capitalize(str.toLowerCase().replace("_", " "));
    }
}
