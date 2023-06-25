package dev.darealturtywurty.superturtybot.modules.counting;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;

import dev.darealturtywurty.superturtybot.modules.counting.maths.MathHandler;

public enum CountingMode {
    NORMAL, REVERSE, DECIMAL, MATHS(true), BINARY, TERNARY, QUATERNARY, QUINARY, SENARY, SEPTENARY, OCTAL, NONARY,
    UNDECIMAL, DUODECIMAL, TRIDECIMAL, TETRADECIMAL, PENTADECIMAL, HEXADECIMAL, BASE36, SQUARES,
    TRIANGULAR, PENTAGONAL, HEXAGONAL, CUBES, PRIMES, ABUNDENT, COMPOSITE, ODD, EVEN, FIBONACCI, LUCAS, GOLOMB, HAPPY,
    LUCKY;
    
    private final boolean notify;
    
    CountingMode() {
        this(false);
    }
    
    CountingMode(boolean notify) {
        this.notify = notify;
    }
    
    public boolean shouldNotify() {
        return this.notify;
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
                final var builder = new StringBuilder("0.");
                for (var i = 0; i < decimalPlaces; i++) {
                    builder.append("0");
                }
                
                builder.append(leftover);
                yield builder.toString();
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
