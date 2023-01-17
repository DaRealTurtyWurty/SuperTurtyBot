package dev.darealturtywurty.superturtybot.core.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either3<L, M, R> {
    private Either3() {
    }

    public abstract <A, B, C> Either3<A, B, C> mapAll(Function<? super L, ? extends A> leftMapper, Function<? super M, ? extends B> middleMapper, Function<? super R, ? extends C> rightMapper);

    public abstract <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super M, ? extends T> middleMapper, Function<? super R, ? extends T> rightMapper);

    public abstract Either3<L, M, R> ifLeft(Consumer<? super L> consumer);

    public abstract Either3<L, M, R> ifMiddle(Consumer<? super M> consumer);

    public abstract Either3<L, M, R> ifRight(Consumer<? super R> consumer);

    public abstract Optional<L> left();

    public abstract Optional<M> middle();

    public abstract Optional<R> right();

    public <T> Either3<T, M, R> mapLeft(Function<? super L, ? extends T> mapper) {
        return map(t -> left(mapper.apply(t)), Either3::middle, Either3::right);
    }

    public <T> Either3<L, T, R> mapMiddle(Function<? super M, ? extends T> mapper) {
        return map(Either3::left, t -> middle(mapper.apply(t)), Either3::right);
    }

    public <T> Either3<L, M, T> mapRight(Function<? super R, ? extends T> mapper) {
        return map(Either3::left, Either3::middle, t -> right(mapper.apply(t)));
    }

    public static <L, M, R> Either3<L, M, R> left(L value) {
        return new Left<>(value);
    }

    public static <L, M, R> Either3<L, M, R> middle(M value) {
        return new Middle<>(value);
    }

    public static <L, M, R> Either3<L, M, R> right(R value) {
        return new Right<>(value);
    }

    public L leftOrThrow() {
        return left().orElseThrow(() -> new IllegalStateException("Either3 is not a Left!"));
    }

    public M middleOrThrow() {
        return middle().orElseThrow(() -> new IllegalStateException("Either3 is not a Middle!"));
    }

    public R rightOrThrow() {
        return right().orElseThrow(() -> new IllegalStateException("Either3 is not a Right!"));
    }

    public <T> Either3<T, M, R> flatMapLeft(Function<? super L, Either3<T, M, R>> mapper) {
        return map(mapper, Either3::middle, Either3::right);
    }

    public <T> Either3<L, T, R> flatMapMiddle(Function<? super M, Either3<L, T, R>> mapper) {
        return map(Either3::left, mapper, Either3::right);
    }

    public <T> Either3<L, M, T> flatMapRight(Function<? super R, Either3<L, M, T>> mapper) {
        return map(Either3::left, Either3::middle, mapper);
    }

    private static final class Left<L, M, R> extends Either3<L, M, R> {
        private final L value;

        public Left(L value) {
            this.value = value;
        }

        @Override
        public <A, B, C> Either3<A, B, C> mapAll(Function<? super L, ? extends A> leftMapper, Function<? super M, ? extends B> middleMapper, Function<? super R, ? extends C> rightMapper) {
            return left(leftMapper.apply(value));
        }

        @Override
        public <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super M, ? extends T> middleMapper, Function<? super R, ? extends T> rightMapper) {
            return leftMapper.apply(value);
        }

        @Override
        public Either3<L, M, R> ifLeft(Consumer<? super L> consumer) {
            consumer.accept(value);
            return this;
        }

        @Override
        public Either3<L, M, R> ifMiddle(Consumer<? super M> consumer) {
            return this;
        }

        @Override
        public Either3<L, M, R> ifRight(Consumer<? super R> consumer) {
            return this;
        }

        @Override
        public Optional<L> left() {
            return Optional.of(value);
        }

        @Override
        public Optional<M> middle() {
            return Optional.empty();
        }

        @Override
        public Optional<R> right() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Left{" + "value=" + value + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Left<?, ?, ?> left = (Left<?, ?, ?>) o;
            return value.equals(left.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static final class Middle<L, M, R> extends Either3<L, M, R> {
        private final M value;

        public Middle(M value) {
            this.value = value;
        }

        @Override
        public <A, B, C> Either3<A, B, C> mapAll(Function<? super L, ? extends A> leftMapper, Function<? super M, ? extends B> middleMapper, Function<? super R, ? extends C> rightMapper) {
            return middle(middleMapper.apply(value));
        }

        @Override
        public <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super M, ? extends T> middleMapper, Function<? super R, ? extends T> rightMapper) {
            return middleMapper.apply(value);
        }

        @Override
        public Either3<L, M, R> ifLeft(Consumer<? super L> consumer) {
            return this;
        }

        @Override
        public Either3<L, M, R> ifMiddle(Consumer<? super M> consumer) {
            consumer.accept(value);
            return this;
        }

        @Override
        public Either3<L, M, R> ifRight(Consumer<? super R> consumer) {
            return this;
        }

        @Override
        public Optional<L> left() {
            return Optional.empty();
        }

        @Override
        public Optional<M> middle() {
            return Optional.of(value);
        }

        @Override
        public Optional<R> right() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Middle{" + "value=" + value + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Middle<?, ?, ?> middle = (Middle<?, ?, ?>) o;
            return value.equals(middle.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static final class Right<L, M, R> extends Either3<L, M, R> {
        private final R value;

        public Right(R value) {
            this.value = value;
        }

        @Override
        public <A, B, C> Either3<A, B, C> mapAll(Function<? super L, ? extends A> leftMapper, Function<? super M, ? extends B> middleMapper, Function<? super R, ? extends C> rightMapper) {
            return right(rightMapper.apply(value));
        }

        @Override
        public <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super M, ? extends T> middleMapper, Function<? super R, ? extends T> rightMapper) {
            return rightMapper.apply(value);
        }

        @Override
        public Either3<L, M, R> ifLeft(Consumer<? super L> consumer) {
            return this;
        }

        @Override
        public Either3<L, M, R> ifMiddle(Consumer<? super M> consumer) {
            return this;
        }

        @Override
        public Either3<L, M, R> ifRight(Consumer<? super R> consumer) {
            consumer.accept(value);
            return this;
        }

        @Override
        public Optional<L> left() {
            return Optional.empty();
        }

        @Override
        public Optional<M> middle() {
            return Optional.empty();
        }

        @Override
        public Optional<R> right() {
            return Optional.of(value);
        }

        @Override
        public String toString() {
            return "Right{" + "value=" + value + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Right<?, ?, ?> right = (Right<?, ?, ?>) o;
            return value.equals(right.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}