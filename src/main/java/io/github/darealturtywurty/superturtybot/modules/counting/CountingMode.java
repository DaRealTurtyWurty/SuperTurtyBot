package io.github.darealturtywurty.superturtybot.modules.counting;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;

public enum CountingMode {
    NORMAL, REVERSE, DECIMAL, MATHS(true);

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
        String value;

        switch (mode) {
            case DECIMAL -> {
                final int decimalPlaces = (int) Math.floor(result / 9);
                final int leftover = (int) result % 9;
                final var builder = new StringBuilder("0.");
                for (var i = 0; i < decimalPlaces; i++) {
                    builder.append("0");
                }

                builder.append(leftover);
                value = builder.toString();
            }
            default -> value = String.valueOf((int) result);
        }

        return value;
    }

    public static float parse(CountingMode mode, String str) {
        float value;
        
        switch (mode) {
            case DECIMAL -> {
                if (!str.startsWith("0.")) {
                    value = Float.NaN;
                    break;
                }
                
                final String decimalOnly = str.replace("0.", "");
                final int decimalPlaces = StringUtils.countMatches(decimalOnly, '0');
                final int lastNumb = Integer.parseInt(decimalOnly.substring(decimalOnly.length() - 1));
                value = lastNumb + decimalPlaces * 9;
            }
            
            default -> {
                try {
                    value = Float.parseFloat(str.split("/s*")[0]);
                } catch (final NumberFormatException exception) {
                    value = Float.NaN;
                }
            }
        }
        
        return value;
    }
}
