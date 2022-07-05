package io.github.darealturtywurty.superturtybot.modules.counting.maths;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.primes.Primes;

import com.google.common.math.BigIntegerMath;

public final class MathHandler {
    private static final Random RANDOM = new Random();

    private MathHandler() {
    }
    
    public static MathOperation chooseOperation(float number) {
        final List<MathOperation> floats = MathOperation.getFloats();
        if ((int) number != number)
            return floats.get(RANDOM.nextInt(floats.size() - 1));
        
        if (number <= 0)
            return MathOperation.ADD;
        if (number > 50_000)
            return MathOperation.MODULO;

        final double root = Math.sqrt(number);
        if (root == (int) root && root != Double.NaN && RANDOM.nextInt(3) == 0 && number != 1)
            return MathOperation.SQRT;

        final List<MathOperation> ints = new ArrayList<>(MathOperation.getInts());
        ints.remove(MathOperation.SQRT);
        
        if (number < 10 && number > 2 && RANDOM.nextInt(ints.size()) == 1) {
            ints.remove(MathOperation.FACTORIAL);
            return MathOperation.FACTORIAL;
        }

        if (number < 2) {
            ints.remove(MathOperation.FACTORIAL);
        }

        if (Primes.isPrime((int) number) || number == 1) {
            ints.remove(MathOperation.DIVIDE);
            ints.remove(MathOperation.SQRT);
        }
        
        if (number < 3) {
            ints.remove(MathOperation.MODULO);
        }

        if (number > 20) {
            ints.remove(MathOperation.SQUARE);
        }
        
        return ints.get(RANDOM.nextInt(ints.size() - 1));
    }

    public static List<Integer> getDivisors(int number) {
        final List<Integer> divisors = new ArrayList<>();
        for (int i = 2; i < number; i++) {
            if (number % i == 0) {
                divisors.add(i);
            }
        }
        
        return divisors;
    }
    
    public static Pair<MathOperation, Float> getNextNumber(float current) {
        final MathOperation operation = chooseOperation(current);
        
        final float value = switch (operation) {
            case MODULO -> {
                final List<Integer> nonDivisors = getNonDivisors((int) current);
                yield current % getRandom(nonDivisors);
            }
            case ADD -> current + RANDOM.nextInt(2, 100);
            case CEIL -> (float) Math.ceil(current);
            case DIVIDE -> {
                final List<Integer> divisors = getDivisors((int) current);
                yield current / getRandom(divisors);
            }
            case FACTORIAL -> BigIntegerMath.factorial((int) current).floatValue();
            case FLOOR -> (float) Math.floor(current);
            case MULTIPLY -> current * RANDOM.nextInt(2, 25);
            case ROUND -> Math.round(current);
            case SQRT -> (float) Math.sqrt(current);
            case SQUARE -> current * current;
            case SUBTRACT -> current - RANDOM.nextInt(2, 100);
        };

        return Pair.of(operation, value);
    }
    
    public static List<Integer> getNonDivisors(int number) {
        final List<Integer> nonDivisors = new ArrayList<>();
        for (int i = 2; i < number; i++) {
            if (number % i > 0) {
                nonDivisors.add(i);
            }
        }

        return nonDivisors;
    }
    
    public static float nextPerfectSquare(float numb) {
        final float nextN = (float) Math.floor(Math.sqrt(numb)) + 1;
        return nextN * nextN;
    }

    public static String parse(MathOperation operation, float current, float result) {
        final String format = operation.getFormat();
        return switch (operation) {
            case ADD -> String.format(format, (int) current, (int) (result - current));
            case CEIL -> String.format(format, current);
            case DIVIDE -> String.format(format, (int) current, (int) (current / result));
            case FLOOR -> String.format(format, current);
            case MODULO -> String.format(format, (int) current, (int) (current - result));
            case MULTIPLY -> String.format(format, (int) current, (int) (result / current));
            case ROUND -> String.format(format, current);
            case SUBTRACT -> String.format(format, (int) current, (int) (current - result));
            default -> String.format(format, (int) current);
        };
    }

    private static int getRandom(List<Integer> list) {
        System.out.println(list.size());
        return list.get(RANDOM.nextInt(list.size()));
    }
}
