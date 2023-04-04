package dev.darealturtywurty.superturtybot.modules.counting.maths;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.primes.Primes;

public final class MathHandler {
    private static final Random RANDOM = new Random();

    private MathHandler() {
    }

    public static MathOperation chooseOperation(float number) {
        final List<MathOperation> floats = MathOperation.getFloats();
        if ((int) number != number)
            return floats.get(RANDOM.nextInt(floats.size() - 1));

        if (number < -1000) {
            int random = RANDOM.nextInt(4);
            return switch (random) {
                case 0 -> Math.sqrt(number) == (int) Math.sqrt(number) ? MathOperation.SQRT : MathOperation.ADD;
                case 1 -> !Primes.isPrime((int) number) ? MathOperation.DIVIDE : MathOperation.ADD;
                case 2 -> MathOperation.MODULO;
                default -> MathOperation.ADD;
            };
        }

        if (number > 1000) {
            int random = RANDOM.nextInt(4);
            return switch (random) {
                case 0 -> Math.sqrt(number) == (int) Math.sqrt(number) ? MathOperation.SQRT : MathOperation.SUBTRACT;
                case 1 -> !Primes.isPrime((int) number) ? MathOperation.DIVIDE : MathOperation.SUBTRACT;
                case 2 -> MathOperation.MODULO;
                default -> MathOperation.SUBTRACT;
            };
        }

        final double root = Math.sqrt(number);
        if (root == (int) root && RANDOM.nextInt(3) == 0 && number != 1)
            return MathOperation.SQRT;

        final List<MathOperation> ints = new ArrayList<>(MathOperation.getInts());
        ints.remove(MathOperation.SQRT);

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

    public static List<Float> getDivisors(int number) {
        final List<Float> divisors = new ArrayList<>();
        for (float i = 2; i < number; i++) {
            if (number % i == 0) {
                divisors.add(i);
            }
        }

        if(divisors.isEmpty())
            divisors.add(2.0F);

        return divisors;
    }

    public static Pair<MathOperation, Float> getNextNumber(float current) {
        final MathOperation operation = chooseOperation(current);

        final float value = switch (operation) {
            case MODULO -> {
                final List<Float> nonDivisors = getNonDivisors((int) current);
                yield current % getRandom(nonDivisors);
            }
            case ADD -> current + RANDOM.nextInt(2, 100);
            case CEIL -> (float) Math.ceil(current);
            case DIVIDE -> {
                final List<Float> divisors = getDivisors((int) current);
                yield current / getRandom(divisors);
            }
            case FLOOR -> (float) Math.floor(current);
            case MULTIPLY -> current * RANDOM.nextInt(2, current > 100 || current < -100 ? 10 : 15);
            case ROUND -> Math.round(current);
            case SQRT -> (float) Math.sqrt(current);
            case SQUARE -> current * current;
            case SUBTRACT -> current - RANDOM.nextInt(2, 100);
        };

        return Pair.of(operation, value);
    }

    public static List<Float> getNonDivisors(int number) {
        final List<Float> nonDivisors = new ArrayList<>();
        for (float i = 2; i < number; i++) {
            if (number % i > 0) {
                nonDivisors.add(i);
            }
        }

        if(nonDivisors.isEmpty())
            nonDivisors.add(2.0F);

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
            case CEIL, ROUND, FLOOR -> String.format(format, current);
            case DIVIDE -> String.format(format, (int) current, (int) (current / result));
            case MODULO, SUBTRACT -> String.format(format, (int) current, (int) (current - result));
            case MULTIPLY -> String.format(format, (int) current, (int) (result / current));
            default -> String.format(format, (int) current);
        };
    }

    private static float getRandom(List<Float> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }
}
