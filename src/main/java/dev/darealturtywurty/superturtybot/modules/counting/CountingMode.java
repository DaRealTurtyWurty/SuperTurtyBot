package dev.darealturtywurty.superturtybot.modules.counting;

import dev.darealturtywurty.superturtybot.modules.counting.maths.MathHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

public enum CountingMode {
    NORMAL("Normal", "Simple ascending count: 1, 2, 3, 4..."),
    REVERSE("Reverse", "Start at 10 and count downward."),
    DECIMAL("Decimal", "Use the 0.x pattern this mode expects."),
    MATHS(true, "Maths", "Solve the generated expression before posting."),
    BINARY("Binary", "Count using base 2 digits."),
    TERNARY("Ternary", "Count using base 3 digits."),
    QUATERNARY("Quaternary", "Count using base 4 digits."),
    QUINARY("Quinary", "Count using base 5 digits."),
    SENARY("Senary", "Count using base 6 digits."),
    SEPTENARY("Septenary", "Count using base 7 digits."),
    OCTAL("Octal", "Count using base 8 digits."),
    NONARY("Nonary", "Count using base 9 digits."),
    UNDECIMAL("Undecimal", "Count using base 11 digits."),
    DUODECIMAL("Duodecimal", "Count using base 12 digits."),
    TRIDECIMAL("Tridecimal", "Count using base 13 digits."),
    TETRADECIMAL("Tetradecimal", "Count using base 14 digits."),
    PENTADECIMAL("Pentadecimal", "Count using base 15 digits."),
    HEXADECIMAL("Hexadecimal", "Count using base 16 digits."),
    BASE36("Base 36", "Use numbers and letters up to Z."),
    SQUARES("Squares", "Count through perfect square numbers."),
    TRIANGULAR("Triangular", "Count through triangle number jumps."),
    PENTAGONAL("Pentagonal", "Count through pentagonal numbers."),
    HEXAGONAL("Hexagonal", "Count through hexagonal numbers."),
    CUBES("Cubes", "Count through cube numbers."),
    PRIMES("Primes", "Count through prime numbers only."),
    ABUNDENT("Abundant", "Count through abundant numbers."),
    COMPOSITE("Composite", "Count through composite numbers."),
    ODD("Odd", "Only odd numbers are valid."),
    EVEN("Even", "Only even numbers are valid."),
    FIBONACCI("Fibonacci", "Count through the Fibonacci sequence."),
    LUCAS("Lucas", "Count through the Lucas sequence."),
    GOLOMB("Golomb", "Count through the Golomb sequence."),
    HAPPY("Happy", "Count through happy numbers."),
    LUCKY("Lucky", "Count through lucky numbers.");

    private final boolean notify;
    private final String displayName;
    private final String description;

    CountingMode() {
        this(false, "Normal", "Count up by one.");
    }

    CountingMode(boolean notify) {
        this(notify, "Maths", "Count with generated arithmetic expressions.");
    }

    CountingMode(String displayName, String description) {
        this(false, displayName, description);
    }

    CountingMode(boolean notify, String displayName, String description) {
        this.notify = notify;
        this.displayName = displayName;
        this.description = description;
    }

    public boolean shouldNotify() {
        return this.notify;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public static float getNextNumber(CountingMode mode, float current) {
        return switch (mode) {
            case REVERSE -> current - 1;
            case SQUARES -> MathHandler.nextPerfectSquare(current);
            default -> current + 1;
        };
    }

    public static float getStartingNumber(CountingMode mode) {
        return switch (mode) {
            case REVERSE -> 10;
            case DECIMAL -> 1;
            case MATHS -> ThreadLocalRandom.current().nextInt(1, 100);
            default -> 0;
        };
    }

    public static String parse(CountingMode mode, float current, float result) {
        return switch (mode) {
            case DECIMAL -> {
                final int decimalPlaces = (int) Math.floor(result / 9);
                final int leftover = (int) result % 9;

                yield "0." + "0".repeat(Math.max(0, decimalPlaces)) +
                        leftover;
            }
            case BINARY -> Integer.toBinaryString((int) result);
            case TERNARY -> Integer.toString((int) result, 3);
            case QUATERNARY -> Integer.toString((int) result, 4);
            case QUINARY -> Integer.toString((int) result, 5);
            case SENARY -> Integer.toString((int) result, 6);
            case SEPTENARY -> Integer.toString((int) result, 7);
            case OCTAL -> Integer.toOctalString((int) result);
            case NONARY -> Integer.toString((int) result, 9);
            case UNDECIMAL -> Integer.toString((int) result, 11);
            case DUODECIMAL -> Integer.toString((int) result, 12);
            case TRIDECIMAL -> Integer.toString((int) result, 13);
            case TETRADECIMAL -> Integer.toString((int) result, 14);
            case PENTADECIMAL -> Integer.toString((int) result, 15);
            case HEXADECIMAL -> Integer.toHexString((int) result);
            case BASE36 -> Integer.toString((int) result, 36);
            default -> String.valueOf((int) result);
        };
    }

    public static float parse(CountingMode mode, String str) {
        return switch (mode) {
            case DECIMAL -> {
                if (!str.startsWith("0.")) {
                    yield Float.NaN;
                }

                final String decimalOnly = str.replace("0.", "");
                final int decimalPlaces = StringUtils.countMatches(decimalOnly, '0');
                final int lastNumb = Integer.parseInt(decimalOnly.substring(decimalOnly.length() - 1));
                yield lastNumb + decimalPlaces * 9;
            }
            case BINARY -> tryParseInt(str, 2);
            case TERNARY -> tryParseInt(str, 3);
            case QUATERNARY -> tryParseInt(str, 4);
            case QUINARY -> tryParseInt(str, 5);
            case SENARY -> tryParseInt(str, 6);
            case SEPTENARY -> tryParseInt(str, 7);
            case OCTAL -> tryParseInt(str, 8);
            case NONARY -> tryParseInt(str, 9);
            case UNDECIMAL -> tryParseInt(str, 11);
            case DUODECIMAL -> tryParseInt(str, 12);
            case TRIDECIMAL -> tryParseInt(str, 13);
            case TETRADECIMAL -> tryParseInt(str, 14);
            case PENTADECIMAL -> tryParseInt(str, 15);
            case HEXADECIMAL -> tryParseInt(str, 16);
            case BASE36 -> tryParseInt(str, 36);

            default -> tryParseFloat(str);
        };
    }

    private static float tryParseFloat(String str) {
        try {
            return Float.parseFloat(str.trim());
        } catch (final NumberFormatException exception) {
            return Float.NaN;
        }
    }

    private static float tryParseInt(String str, int radix) {
        try {
            return Integer.parseInt(str, radix);
        } catch (final NumberFormatException exception) {
            return Float.NaN;
        }
    }
}
