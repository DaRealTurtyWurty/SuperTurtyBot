package dev.darealturtywurty.superturtybot.modules.collectable;

import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobCollectable;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
public class Answer {
    private final AnswerSegment[] segments;

    private Answer(AnswerSegment... segments) {
        this.segments = segments;
    }

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

    @Getter
    public static class AnswerSegment {
        private final String segment;
        private final boolean caseSensitive;
        private final boolean contains;

        private AnswerSegment(String segment, boolean caseSensitive, boolean contains) {
            this.segment = segment;
            this.caseSensitive = caseSensitive;
            this.contains = contains;
        }

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

        public boolean isEmpty() {
            return segment.isBlank();
        }

        public static class Builder {
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

            public AnswerSegment build() {
                if (segment == null || segment.isBlank())
                    throw new IllegalArgumentException("Segment must be set!");

                return new AnswerSegment(segment, caseSensitive, contains);
            }
        }
    }

    public static class OrSegment extends AnswerSegment {
        private final AnswerSegment[] segments;

        private OrSegment(AnswerSegment... segments) {
            super("", false, false);
            this.segments = segments;
        }

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

        public static class Builder {
            private final List<AnswerSegment> segments = new ArrayList<>();

            public Builder segment(String segment) {
                return segment(segment, false, false);
            }

            public Builder segment(String segment, boolean caseSensitive) {
                return segment(segment, caseSensitive, false);
            }

            public Builder segment(String segment, boolean caseSensitive, boolean contains) {
                return segment(new AnswerSegment.Builder()
                        .segment(segment)
                        .caseSensitive(caseSensitive)
                        .contains(contains));
            }

            public Builder segment(AnswerSegment.Builder segment) {
                return segment(segment.build());
            }

            public Builder segment(AnswerSegment segment) {
                segments.add(segment);
                return this;
            }

            public OrSegment build() {
                return new OrSegment(segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public static class XOrSegment extends AnswerSegment {
        private final AnswerSegment[] segments;

        private XOrSegment(AnswerSegment... segments) {
            super("", false, false);
            this.segments = segments;
        }

        @Override
        public boolean isEmpty() {
            return Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
        }

        @Override
        public boolean matches(String input) {
            boolean found = false;
            for (AnswerSegment segment : segments) {
                if (segment.matches(input)) {
                    if (found) {
                        return false;
                    }

                    found = true;
                }
            }

            return found;
        }

        public static class Builder {
            private final List<AnswerSegment> segments = new ArrayList<>();

            public Builder segment(String segment) {
                return segment(segment, false, false);
            }

            public Builder segment(String segment, boolean caseSensitive) {
                return segment(segment, caseSensitive, false);
            }

            public Builder segment(String segment, boolean caseSensitive, boolean contains) {
                return segment(new AnswerSegment.Builder()
                        .segment(segment)
                        .caseSensitive(caseSensitive)
                        .contains(contains));
            }

            public Builder segment(AnswerSegment.Builder segment) {
                return segment(segment.build());
            }

            public Builder segment(AnswerSegment segment) {
                segments.add(segment);
                return this;
            }

            public XOrSegment build() {
                return new XOrSegment(segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public static class NotSegment extends AnswerSegment {
        private final AnswerSegment[] segments;

        private NotSegment(AnswerSegment... segments) {
            super("", false, false);
            this.segments = segments;
        }

        @Override
        public boolean isEmpty() {
            return Arrays.stream(segments).allMatch(AnswerSegment::isEmpty);
        }

        @Override
        public boolean matches(String input) {
            return Arrays.stream(segments).noneMatch(segment -> segment.matches(input));
        }

        public static class Builder {
            private final List<AnswerSegment> segments = new ArrayList<>();

            public Builder segments(String... segments) {
                for (String segment : segments) {
                    segment(segment);
                }

                return this;
            }

            public Builder segment(String segment) {
                return segment(segment, false, false);
            }

            public Builder segment(String segment, boolean caseSensitive) {
                return segment(segment, caseSensitive, false);
            }

            public Builder segment(String segment, boolean caseSensitive, boolean contains) {
                return segment(new AnswerSegment.Builder()
                        .segment(segment)
                        .caseSensitive(caseSensitive)
                        .contains(contains));
            }

            public Builder segment(AnswerSegment.Builder segment) {
                this.segments.add(segment.build());
                return this;
            }

            public Builder segments(AnswerSegment... segments) {
                this.segments.addAll(Arrays.asList(segments));
                return this;
            }

            public NotSegment build() {
                return new NotSegment(segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public static class NumberSegment extends AnswerSegment {
        private final double answer;

        private NumberSegment(double answer) {
            super("", false, false);
            this.answer = answer;
        }

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

        public static class Builder {
            private double answer;

            public Builder answer(double answer) {
                this.answer = answer;
                return this;
            }

            public NumberSegment build() {
                return new NumberSegment(answer);
            }
        }
    }

    public static class AnyNofSegment extends AnswerSegment {
        private final int number;
        private final AnswerSegment[] segments;

        private AnyNofSegment(int number, AnswerSegment... segments) {
            super("", false, false);
            this.number = number;
            this.segments = segments;
        }

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

            return count == number;
        }

        public static class Builder {
            private int number;
            private final List<AnswerSegment> segments = new ArrayList<>();

            public Builder number(int number) {
                this.number = number;
                return this;
            }

            public Builder segment(String segment) {
                return segment(segment, false, false);
            }

            public Builder segment(String segment, boolean caseSensitive) {
                return segment(segment, caseSensitive, false);
            }

            public Builder segment(String segment, boolean caseSensitive, boolean contains) {
                return segment(new AnswerSegment.Builder()
                        .segment(segment)
                        .caseSensitive(caseSensitive)
                        .contains(contains));
            }

            public Builder segment(AnswerSegment.Builder segment) {
                return segment(segment.build());
            }

            public Builder segment(AnswerSegment segment) {
                segments.add(segment);
                return this;
            }

            public AnyNofSegment build() {
                return new AnyNofSegment(number, segments.toArray(new AnswerSegment[0]));
            }
        }
    }

    public static class Builder {
        private final List<AnswerSegment> segments = new ArrayList<>();
        private MinecraftMobCollectable.Builder parent;

        public Builder start(MinecraftMobCollectable.Builder parent) {
            this.parent = parent;
            return this;
        }

        public Builder segment(String segment, boolean caseSensitive, boolean contains) {
            return segment(new AnswerSegment.Builder()
                    .segment(segment)
                    .caseSensitive(caseSensitive)
                    .contains(contains));
        }

        public Builder segment(String segment) {
            return segment(segment, false, true);
        }

        public Builder segments(String... segments) {
            for (String segment : segments) {
                segment(segment);
            }

            return this;
        }

        public Builder segment(AnswerSegment.Builder segment) {
            return segment(segment.build());
        }

        public Builder segment(AnswerSegment segment) {
            segments.add(segment);
            return this;
        }

        public Builder orSegment(OrSegment.Builder segment) {
            return orSegment(segment.build());
        }

        public Builder orSegment(OrSegment segment) {
            segments.add(segment);
            return this;
        }

        public Builder xorSegment(XOrSegment.Builder segment) {
            return xorSegment(segment.build());
        }

        public Builder xorSegment(XOrSegment segment) {
            segments.add(segment);
            return this;
        }

        public Builder notSegment(NotSegment.Builder segment) {
            return notSegment(segment.build());
        }

        public Builder notSegment(NotSegment segment) {
            segments.add(segment);
            return this;
        }

        public Builder numberSegment(double answer) {
            segments.add(new NumberSegment.Builder().answer(answer).build());
            return this;
        }

        public Builder anyNof(int number, AnswerSegment... segments) {
            var builder = new AnyNofSegment.Builder().number(number);
            for (AnswerSegment segment : segments) {
                builder.segment(segment);
            }

            this.segments.add(builder.build());
            return this;
        }

        public Builder anyNof(int number, String... segments) {
            return anyNof(number, Arrays.stream(segments)
                    .map(segment -> new AnswerSegment.Builder().segment(segment).build())
                    .toArray(AnswerSegment[]::new));
        }

        public MinecraftMobCollectable.Builder finish() {
            return parent;
        }

        public Answer build() {
            return new Answer(segments.toArray(new AnswerSegment[0]));
        }
    }
}