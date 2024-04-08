package dev.darealturtywurty.superturtybot.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MathUtils {
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static byte clamp(byte value, byte min, byte max) {
        return (byte) Math.max(min, Math.min(max, value));
    }

    public static short clamp(short value, short min, short max) {
        return (short) Math.max(min, Math.min(max, value));
    }

    public static BigInteger clamp(BigInteger value, BigInteger min, BigInteger max) {
        return value.max(min).min(max);
    }

    public static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.max(min).min(max);
    }

    public static int clampPositive(int value) {
        return Math.max(0, value);
    }

    public static long clampPositive(long value) {
        return Math.max(0, value);
    }

    public static float clampPositive(float value) {
        return Math.max(0, value);
    }

    public static double clampPositive(double value) {
        return Math.max(0, value);
    }

    public static byte clampPositive(byte value) {
        return (byte) Math.max(0, value);
    }

    public static short clampPositive(short value) {
        return (short) Math.max(0, value);
    }

    public static BigInteger clampPositive(BigInteger value) {
        return value.max(BigInteger.ZERO);
    }

    public static BigDecimal clampPositive(BigDecimal value) {
        return value.max(BigDecimal.ZERO);
    }

    public static int clampNegative(int value) {
        return Math.min(0, value);
    }

    public static long clampNegative(long value) {
        return Math.min(0, value);
    }

    public static float clampNegative(float value) {
        return Math.min(0, value);
    }

    public static double clampNegative(double value) {
        return Math.min(0, value);
    }

    public static byte clampNegative(byte value) {
        return (byte) Math.min(0, value);
    }

    public static short clampNegative(short value) {
        return (short) Math.min(0, value);
    }

    public static BigInteger clampNegative(BigInteger value) {
        return value.min(BigInteger.ZERO);
    }

    public static BigDecimal clampNegative(BigDecimal value) {
        return value.min(BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Number> T genericClamp(T value, T min, T max) {
        if (value instanceof Integer intV)
            return (T) Integer.valueOf(clamp(intV, min.intValue(), max.intValue()));
        if (value instanceof Long longV)
            return (T) Long.valueOf(clamp(longV, min.longValue(), max.longValue()));
        if (value instanceof Float floatV)
            return (T) Float.valueOf(clamp(floatV, min.floatValue(), max.floatValue()));
        if (value instanceof Double doubleV)
            return (T) Double.valueOf(clamp(doubleV, min.doubleValue(), max.doubleValue()));
        if (value instanceof Byte byteV)
            return (T) Byte.valueOf(clamp(byteV, min.byteValue(), max.byteValue()));
        if (value instanceof Short shortV)
            return (T) Short.valueOf(clamp(shortV, min.shortValue(), max.shortValue()));
        if (value instanceof BigInteger bigIntegerV)
            return (T) clamp(bigIntegerV, (BigInteger) min, (BigInteger) max);
        if (value instanceof BigDecimal bigDecimalV)
            return (T) clamp(bigDecimalV, (BigDecimal) min, (BigDecimal) max);

        throw new IllegalArgumentException("Cannot clamp " + value.getClass().getSimpleName());
    }

    public static int map(int stage, int min1, int max1, int min2, int max2) {
        return (stage - min1) * (max2 - min2) / (max1 - min1) + min2;
    }

    /**
     * Generates a random integer between the min and max bounds with a weighted distribution.
     *
     * @param min    The minimum bound
     * @param max    The maximum bound
     * @param weight The weight of the distribution
     *               (0.0 is uniform distribution,
     *               -100.0 is a ceiling distribution,
     *               100.0 is a floor distribution)
     * @return       A random integer between the min and max bounds with a weighted distribution
     */
    public static int weightedRandomInt(int min, int max, double weight) {
        if (min == max) {
            return min;
        }

        int upperBound = Math.max(max, min);
        int lowerBound = min + max - upperBound;

        int scale = 0x10000000;
        double random = Math.random() * scale;

        // If it's uniformly distributed, just return a random number between the bounds
        if (weight == 0.0) {
            return lowerBound + (int) Math.floor(random % (upperBound - lowerBound + 1));
        }

        double normalizedWeightedResult = getNormalizedWeightedResult(weight, random, scale);
        double scaledNormalizedWeightedResult = normalizedWeightedResult * (upperBound - lowerBound); // Scale the normalized result to the bounds
        double scaledResult = scaledNormalizedWeightedResult + lowerBound; // Shift the scaled result to the lower bound

        if ((weight < 0) ^ (min > max)) { // If the weight is negative and the bounds are flipped, return the ceiling
            return (int) Math.ceil(upperBound - scaledResult); // Return the ceiling
        }

        return (int) Math.floor(lowerBound + scaledResult); // Return the floor
    }

    private static double getNormalizedWeightedResult(double weight, double random, int scale) {
        double distributionBase = 2;
        double middleBias = 0.5;

        double absoluteWeight = Math.abs(weight); // Get the absolute weight
        double middleBiasFactor = Math.pow(middleBias, -absoluteWeight); // Calculate the middle bias
        double distributionBaseFactor = Math.pow(distributionBase, -absoluteWeight); // Calculate the base of the distribution

        double biasRange = middleBiasFactor - distributionBaseFactor; // Calculate the range of the bias
        double scaledRandomValue = random * biasRange / scale; // Scale to bias range
        double shiftedScaledRandomValue = scaledRandomValue + distributionBaseFactor; // Shift to distribution base

        double weightedResult = Math.pow(shiftedScaledRandomValue, -1 / absoluteWeight); // Invert the weight so that the distribution is correct

        double normalizedWeightedResult = (weightedResult - middleBias) / (distributionBase - middleBias); // Normalize the result to the bounds
        return normalizedWeightedResult;
    }
}
