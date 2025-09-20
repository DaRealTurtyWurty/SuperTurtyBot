package dev.darealturtywurty.superturtybot.modules.collectable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public record Answer(AnswerSegment... segments) {
    public boolean matches(String input) {
        for (AnswerSegment segment : segments) {
            if (!segment.matches(input)) {
                return false;
            }
        }

        return true;
    }

    public boolean isEmpty() {
        return segments.length == 0 || Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
    }

    public interface AnswerSegment {
        boolean matches(String input);

        boolean isEmpty();
    }

    public interface AnswerSegmentBuilder {
        AnswerSegment build();
    }

    public interface MultipleSegmentAnswerSegmentBuilder<T> {

        T segment(AnswerSegment segment);

        default T segment(AnswerSegmentBuilder segment) {
            return segment(segment.build());
        }

        default T segment(String segment, boolean caseSensitive, boolean contains) {
            return segment(new StringAnswerSegment.Builder()
                    .segment(segment)
                    .caseSensitive(caseSensitive)
                    .contains(contains));
        }

        default T segment(String segment) {
            return segment(segment, false, false);
        }

        default T segment(String segment, boolean caseSensitive) {
            return segment(segment, caseSensitive, false);
        }

        default T segments(String... segments) {
            return segments(this::segment, segments);
        }

        default T segments(AnswerSegment... segments) {
            return segments(this::segment, segments);
        }

        default <S> T segments(Function<S, T> addSegment, S... segments) {
            T self = null;
            for (S segment : segments) {
                self = addSegment.apply(segment);
            }
            return self;
        }

        default T numberSegment(double answer) {
            return segment(new NumberSegment.Builder().answer(answer).build());
        }

        default T anyNof(int number, AnswerSegment... segments) {
            var builder = new AnyNOfSegment.Builder().number(number).segments(segments);
            return segment(builder);
        }

        default T anyNof(int number, String... segments) {
            var builder = new AnyNOfSegment.Builder().number(number).segments(segments);
            return segment(builder);
        }

        default T not(AnswerSegment... segments) {
            var builder = new NotSegment.Builder().segments(segments);
            return segment(builder);
        }

        default T not(String... segments) {
            var builder = new NotSegment.Builder().segments(segments);
            return segment(builder);
        }

        default T or(AnswerSegment... segments) {
            var builder = new OrSegment.Builder().segments(segments);
            return segment(builder);
        }

        default T or(String... segments) {
            var builder = new OrSegment.Builder().segments(segments);
            return segment(builder);
        }
    }

    public record StringAnswerSegment(String segment, boolean caseSensitive,
                                      boolean contains) implements AnswerSegment {
        @Override
        public boolean matches(String input) {
            if (caseSensitive) {
                if (contains) {
                    return input.contains(segment);
                }
                return input.equals(segment);
            }

            if (contains) {
                return input.toLowerCase(Locale.ROOT).contains(segment.toLowerCase(Locale.ROOT));
            }

            return input.equalsIgnoreCase(segment);
        }

        @Override
        public boolean isEmpty() {
            return segment.isBlank();
        }

        public static class Builder implements AnswerSegmentBuilder {
            private String segment = "";
            private boolean caseSensitive = false;
            private boolean contains = false;

            public Builder segment(String segment) {
                this.segment = segment;
                return this;
            }

            public Builder caseSensitive(boolean caseSensitive) {
                this.caseSensitive = caseSensitive;
                return this;
            }

            public Builder contains(boolean contains) {
                this.contains = contains;
                return this;
            }

            @Override
            public StringAnswerSegment build() {
                if (segment == null || segment.isBlank())
                    throw new IllegalArgumentException("Segment must be set!");

                return new StringAnswerSegment(segment, caseSensitive, contains);
            }
        }
    }

    public record OrSegment(AnswerSegment... segments) implements AnswerSegment {
        @Override
        public boolean isEmpty() {
            return Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
        }

        @Override
        public boolean matches(String input) {
            for (AnswerSegment segment : segments) {
                if (segment.matches(input)) {
                    return true;
                }
            }

            return false;
        }

        public static class Builder implements AnswerSegmentBuilder, MultipleSegmentAnswerSegmentBuilder<Builder> {
            private final List<AnswerSegment> segments = new ArrayList<>();

            @Override
            public Builder segment(AnswerSegment segment) {
                segments.add(segment);
                return this;
            }

            @Override
            public OrSegment build() {
                return new OrSegment(segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public record OneOfSegment(AnswerSegment... segments) implements AnswerSegment {
        @Override
        public boolean isEmpty() {
            return Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
        }

        @Override
        public boolean matches(String input) {
            boolean found = false;
            for (AnswerSegment segment : segments) {
                if (!segment.matches(input))
                    continue;
                if (found)
                    return false;

                found = true;
            }

            return found;
        }

        public static class Builder implements AnswerSegmentBuilder, MultipleSegmentAnswerSegmentBuilder<Builder> {
            private final List<AnswerSegment> segments = new ArrayList<>();

            @Override
            public Builder segment(AnswerSegment segment) {
                segments.add(segment);
                return this;
            }

            @Override
            public OneOfSegment build() {
                return new OneOfSegment(segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public record NotSegment(AnswerSegment... segments) implements AnswerSegment {
        @Override
        public boolean isEmpty() {
            return Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
        }

        @Override
        public boolean matches(String input) {
            return Arrays.stream(segments).noneMatch(segment -> segment.matches(input));
        }

        public static class Builder implements AnswerSegmentBuilder, MultipleSegmentAnswerSegmentBuilder<Builder> {
            private final List<AnswerSegment> segments = new ArrayList<>();

            @Override
            public Builder segment(AnswerSegment segment) {
                this.segments.add(segment);
                return this;
            }

            @Override
            public NotSegment build() {
                return new NotSegment(segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public record NumberSegment(double answer) implements AnswerSegment {
        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean matches(String input) {
            try {
                return Double.parseDouble(input) == answer;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public static class Builder implements AnswerSegmentBuilder {
            private double answer;

            public Builder answer(double answer) {
                this.answer = answer;
                return this;
            }

            @Override
            public NumberSegment build() {
                return new NumberSegment(answer);
            }
        }
    }

    public record AnyNOfSegment(int number, AnswerSegment... segments) implements AnswerSegment {
        @Override
        public boolean isEmpty() {
            return number == 0 || Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
        }

        @Override
        public boolean matches(String input) {
            int count = 0;
            for (AnswerSegment segment : segments) {
                if (segment.matches(input)) {
                    count++;
                }
            }

            return count >= number;
        }

        public static class Builder implements AnswerSegmentBuilder, MultipleSegmentAnswerSegmentBuilder<Builder> {
            private int number;
            private final List<AnswerSegment> segments = new ArrayList<>();

            public Builder number(int number) {
                this.number = number;
                return this;
            }

            @Override
            public Builder segment(AnswerSegment segment) {
                segments.add(segment);
                return this;
            }

            @Override
            public Builder segments(String... segments) {
                return segments(str -> segment(str, false, true), segments);
            }

            @Override
            public AnyNOfSegment build() {
                return new AnyNOfSegment(number, segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public static class Builder<T> implements MultipleSegmentAnswerSegmentBuilder<Builder<T>> {
        private final List<AnswerSegment> segments = new ArrayList<>();
        private T parent;

        public Builder<T> start(T parent) {
            this.parent = parent;
            return this;
        }

        @Override
        public Builder<T> segment(AnswerSegment segment) {
            segments.add(segment);
            return this;
        }

        @Override
        public Builder<T> segment(String segment) {
            return segment(segment, false, true);
        }

        @Override
        public Builder<T> segment(String segment, boolean caseSensitive) {
            return segment(segment, caseSensitive, true);
        }

        public T finish() {
            return parent;
        }

        public Answer build() {
            return new Answer(segments.toArray(new AnswerSegment[0]));
        }
    }
}