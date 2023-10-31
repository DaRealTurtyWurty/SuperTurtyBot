package dev.darealturtywurty.superturtybot.core.util;

import java.util.List;
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
        throw new IllegalAccessError("Cannot access private constructor!");
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

    /**
     * Recursive implementation, invokes itself for each factor of a thousand, increasing the class on each invocation.
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

    public static String convertExplicitContentLevel(Guild.ExplicitContentLevel level) {
        return switch (level) {
            case OFF -> "None";
            case NO_ROLE -> "Un-roled Members";
            case ALL -> "All Messages";
            case UNKNOWN -> "Unknown";
            default -> "Undefined";
        };
    }

    public static String convertNotificationLevel(Guild.NotificationLevel level) {
        return switch (level) {
            case ALL_MESSAGES -> "All Messages";
            case MENTIONS_ONLY -> "Mentions Only";
            case UNKNOWN -> "Unknown";
            default -> "Undefined";
        };
    }

    public static String convertNSFWLevel(Guild.NSFWLevel level) {
        return switch (level) {
            case SAFE -> "Safe";
            case AGE_RESTRICTED -> "Age Restricted";
            case EXPLICIT -> "Explicit";
            case DEFAULT -> "Default";
            case UNKNOWN -> "Unknown";
            default -> "Undefined";
        };
    }

    public static String convertVerificationLevel(Guild.VerificationLevel level) {
        return switch (level) {
            case NONE -> "None";
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
            case VERY_HIGH -> "Very High";
            case UNKNOWN -> "Unknown";
            default -> "Undefined";
        };
    }
}
