package dev.darealturtywurty.superturtybot.core.util;

import com.vdurmont.emoji.EmojiParser;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@SuppressWarnings("UnnecessaryDefault")
public final class StringUtils {
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

    private static String getSuffixForIteration(int iteration) {
        if (iteration > 1000) {
            return "e" + iteration * 3;
        }
        // suffixes taken from: https://button-simulatored.fandom.com/wiki/Suffix_List
        String[] suffixForOnesDigit = {"", "U", "D", "T", "Qd", "Qn", "Sx", "Sp", "Oc", "No"};
        String[] suffixForTensDigit = {"", "De", "Vt", "Tg", "qg", "Qg", "sg", "Sg", "Og", "Ng"};
        String[] suffixForHundredsDigit = {"", "Ce", "Du", "Tr", "Qa", "Qi", "Se", "Si", "Ot", "Ni"};
        switch (iteration) {
            case 0 -> {
                return "";
            }
            case 1 -> {
                return "k";
            }
            case 2 -> {
                return "m";
            }
            case 3 -> {
                return "b";
            }
        }
        int numberYouSayItAs = iteration - 1;
        int ones = numberYouSayItAs % 10;
        int tens = numberYouSayItAs / 10 % 10;
        int hundreds = numberYouSayItAs / 100 % 10;

        return suffixForOnesDigit[ones] + suffixForTensDigit[tens] + suffixForHundredsDigit[hundreds];
    }

    /**
     * Formats a number to a readable format.
     *
     * @param n the number to format
     * @return a formatted string
     */
    public static String numberFormat(final BigInteger n) {
        return numberFormat(n, "");
    }

    /**
     * Formats a number to a readable format with the currency symbol from the guild data.
     *
     * @param n the number to format
     * @param configWithCurrencySymbol a config with a currency symbol
     * @return a formatted string
     */
    public static String numberFormat(final BigInteger n, GuildData configWithCurrencySymbol) {
        return numberFormat(n, configWithCurrencySymbol.getEconomyCurrency());
    }

    /**
     * Formats a number to a readable format with a string between the sign and the number.
     *
     * @param n the number to format
     * @param betweenSignAndNumber a string between the sign and the number
     * @return a formatted string
     */
    public static String numberFormat(BigInteger n, String betweenSignAndNumber) {
        boolean isNegative = n.signum() < 0;
        n = n.abs();
        int iteration = 0;
        BigInteger oneHundred = BigInteger.valueOf(100);
        n = n.multiply(oneHundred);
        while (n.compareTo(BigInteger.valueOf(100_000)) >= 0) {
            n = n.divide(BigInteger.valueOf(1000));
            iteration++;
        }

        BigInteger[] divMod = n.divideAndRemainder(oneHundred);
        String divStr = divMod[0].toString();
        String numberSuffix = getSuffixForIteration(iteration);
        String formattedNumber;
        if (divMod[1].signum() == 0) {
            formattedNumber = divStr + numberSuffix;
        } else {
            String modStr = divMod[1].toString();
            formattedNumber = divStr + "." + (modStr.length() < 2 ? "0" : "") + modStr + numberSuffix;
        }

        return (isNegative ? "-" : "") + betweenSignAndNumber + formattedNumber;
    }

    /**
     * Returns a function that parses a string OptionMapping to a BigInteger.
     * If the string is not a valid number, the event will be replied to with an error message, and the BigInteger will be null.
     * @param event the event to reply to if the number is not valid
     * @param wasDeferred whether the event was already deferred
     * @return a function that parses a string OptionMapping to a BigInteger
     * @see #getAsBigInteger(SlashCommandInteractionEvent)
     */
    public static Function<OptionMapping, @Nullable BigInteger> getAsBigInteger(SlashCommandInteractionEvent event, boolean wasDeferred) {
        return option -> {
            final String string = option.getAsString();
            try {
                return new BigInteger(string);
            } catch (final NumberFormatException exception) {
                String message = "❌ That is not a valid number!";
                if (wasDeferred) {
                    event.getHook().editOriginal(message).mentionRepliedUser(false).queue();
                } else {
                    CoreCommand.reply(event, message, false, true);
                }
                return null;
            }
        };
    }

    /**
     * Returns a function that parses a string OptionMapping to a BigInteger.</br>
     * Identical to calling {@link #getAsBigInteger(SlashCommandInteractionEvent, boolean) StringUtils.getAsBigInteger(event, true)}.
     * @param event the event to reply to if the number is not valid
     * @return a function that parses a string OptionMapping to a BigInteger
     * @see #getAsBigInteger(SlashCommandInteractionEvent, boolean)
     */
    public static Function<OptionMapping, @Nullable BigInteger> getAsBigInteger(SlashCommandInteractionEvent event) {
        return getAsBigInteger(event, true);
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
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

                final RichCustomEmoji emote = jda.getEmojiById(emotes.getFirst());
                Member selfMember = emote == null ?
                        null :
                        emote.getGuild().getMemberById(guild.getSelfMember().getIdLong());
                if (emote == null ||
                        selfMember == null ||
                        emote.getGuild().getMemberById(guild.getSelfMember().getIdLong()) == null ||
                        !selfMember.canInteract(emote))
                    return null;

                return emote.getAsMention();
            }

            return Emoji.fromFormatted(emojis.getFirst()).getName();
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

    public static String makeProgressBar(long total, long current, int size, String line, String slider) {
        final var result = new String[size];
        if (current >= total) {
            Arrays.fill(result, line);
            result[size - 1] = slider;
            return String.join("", result);
        }

        final double percentage = (float) current / total;
        final int progress = (int) Math.max(0, Math.min(Math.round(size * percentage), size - 1));
        for (int index = 0; index < progress; index++) {
            result[index] = line;
        }

        result[progress] = slider;
        for (int index = progress + 1; index < size; index++) {
            result[index] = line;
        }

        return String.join("", result);
    }

    public static String makeProgressBar(double total, double current, int size, String line, String slider) {
        final var result = new String[size];
        if (current >= total) {
            Arrays.fill(result, line);
            result[size - 1] = slider;
            return String.join("", result);
        }

        final double percentage = current / total;
        final int progress = (int) Math.max(0, Math.min(Math.round(size * percentage), size - 1));
        for (int index = 0; index < progress; index++) {
            result[index] = line;
        }

        result[progress] = slider;
        for (int index = progress + 1; index < size; index++) {
            result[index] = line;
        }

        return String.join("", result);
    }

    public static String getOrdinalSuffix(int age) {
        if (age >= 10 && age <= 20)
            return "th";

        return switch (age % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public static boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (final NumberFormatException exception) {
            return false;
        }
    }

    public static String formatCurrency(String currencySymbol, long amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        String formattedAmount = currencyFormatter.format(amount);

        // Remove the default currency symbol and replace it with the custom symbol
        formattedAmount = formattedAmount.replace(NumberFormat.getCurrencyInstance(Locale.US).getCurrency().getSymbol(), currencySymbol);

        // Remove the decimal point and the cents
        formattedAmount = formattedAmount.replace(".00", "");

        return formattedAmount;
    }
}
