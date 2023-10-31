package dev.darealturtywurty.superturtybot.core.util.function;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * Represents a value of one of two possible types (a <a
 * href="https://en.wikipedia.org/wiki/Disjoint_union">disjoint union</a>). An instance of
 * {@link Either} is (constructor enforced via preconditions) to be well-defined and for whichever
 * side is defined, the left or right, the value is guaranteed to be not-null.
 * <p>
 * -
 * <p>
 * A common use of {@link Either} is as an alternative to {@link Optional} for dealing with
 * possibly erred or missing values. In this usage, {@link Optional#isEmpty} is replaced with
 * {@link Either#getLeft} which, unlike {@link Optional#isEmpty}, can contain useful information,
 * like a descriptive error message. {@link Either#getRight} takes the place of
 * {@link java.util.Optional#get}
 * <p>
 * -
 * <p>
 * {@link Either} is right-biased, which means {@link Either#getRight} is assumed to be the
 * default case upon which to operate. If it is defined for the left, operations like
 * {@link Either#toOptional} returns {@link Optional#isEmpty}, and {@link Either#map} and
 * {@link Either#flatMap} return the left value unchanged.
 * <p>
 * -
 * <p>
 * While inspired by the (first) solution presented in this <a
 * href="https://stackoverflow.com/a/26164155/501113">StackOverflow Answer</a>, this updated
 * version {@link Either} is internally implemented via a pair of {@link Optional}s, each of which
 * explicitly reject null values by throwing a {@link java.lang.NullPointerException} from both
 * factory methods, {@link Either#left} and {@link Either#right}.
 **/
public final class Either<L, R> {

    /**
     * The left side of a disjoint union, as opposed to the right side.
     *
     * @param value instance of type L to be contained
     * @param <L>   the type of the left value to be contained
     * @param <R>   the type of the right value to be contained
     * @return an instance of {@link Either} well-defined for the left side
     */
    public static <L, R> Either<L, R> left(L value) {
        return new Either<>(Optional.of(value), Optional.empty());
    }

    /**
     * The right side of a disjoint union, as opposed to the left side.
     *
     * @param value instance of type R to be contained
     * @param <L>   the type of the left value to be contained
     * @param <R>   the type of the right value to be contained
     * @return an instance of {@link Either} well-defined for the right side
     */
    public static <L, R> Either<L, R> right(R value) {
        return new Either<>(Optional.empty(), Optional.of(value));
    }

    /**
     * Reify to an {@link Either}. If defined, place the {@link Optional} value into the right side
     * of the {@link Either}, or else use the {@link Supplier} to define the left side of the
     * {@link Either}.
     *
     * @param leftSupplier  function invoked (only if rightOptional.isEmpty() returns true) to place
     *                      the returned value for the left side of the {@link Either}
     * @param rightOptional the contained value is placed into the right side of the {@link Either}
     * @param <L>           type of the instance provided by the {@link Supplier}
     * @param <R>           type of the value in the instance of the {@link Optional}
     * @return a well-defined instance of {@link Either}
     */
    public static <L, R> Either<L, R> from(Supplier<L> leftSupplier, Optional<R> rightOptional) {
        return rightOptional
                .map(Either::<L, R>right)
                .orElseGet(() -> Either.left(leftSupplier.get()));
    }

    private final Optional<L> left;
    private final Optional<R> right;

    private Either(Optional<L> left, Optional<R> right) {
        if (left.isEmpty() == right.isEmpty()) {
            throw new IllegalArgumentException("left.isEmpty() must not be equal to right.isEmpty()");
        }
        this.left = left;
        this.right = right;
    }

    /**
     * Indicates whether some other instance is "equal" to this one.
     *
     * @param o reference instance with which to compare
     * @return true if this object is the equivalent value as the o argument
     */
    @Override
    public boolean equals(Object o) {
        return (this == o) ||
                ((o instanceof Either<?, ?> that)
                        && Objects.equals(this.left, that.left)
                        && Objects.equals(this.right, that.right));
    }

    /**
     * Returns a hash code value for this instance.
     *
     * @return a hash code value for this instance
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.right);
    }

    /**
     * Returns true if this {@link Either} is defined on the left side
     *
     * @return true if the left side of this {@link Either} contains a value
     */
    public boolean isLeft() {
        return this.left.isPresent();
    }

    /**
     * Returns true if this {@link Either} is defined on the right side
     *
     * @return true if the right side of this {@link Either} contains a value
     */
    public boolean isRight() {
        return this.right.isPresent();
    }

    /**
     * If defined (which can be detected with {@link Either#isLeft}), returns the value for the left
     * side of {@link Either}, or else throws an {@link java.util.NoSuchElementException}
     *
     * @return value of type L for the left, if the left side of this Either is defined
     */
    public L getLeft() {
        return this.left.get();
    }

    /**
     * If defined (which can be detected with {@link Either#isRight}), returns the value for the
     * left side of {@link Either}, or else throws an {@link java.util.NoSuchElementException}
     *
     * @return value of type R for the left, if the right side of this Either is defined
     */
    public R getRight() {
        return this.right.get();
    }

    /**
     * Reduce to an Optional. If defined, returns the value for the right side of {@link Either} in
     * an {@link Optional#of}, or else returns {@link Optional#empty}.
     *
     * @return an {@link Optional} containing the right side if defined, or else returns
     * {@link Optional#empty}
     */
    public Optional<R> toOptional() {
        return this.right;
    }

    /**
     * If right is defined, the given map translation function is applied. Forwards call to
     * {@link Either#mapRight}.
     *
     * @param rightFunction given function which is only applied if right is defined
     * @param <T>           target type to which R is translated
     * @return result of the function translation, replacing type R with type T
     */
    public <T> Either<L, T> map(Function<? super R, ? extends T> rightFunction) {
        return mapRight(rightFunction);
    }

    /**
     * If right is defined, the given flatMap translation function is applied. Forwards call to
     * {@link Either#flatMapRight}.
     *
     * @param rightFunction given function which is only applied if right is defined
     * @param <T>           target type to which R is translated
     * @return result of the function translation, replacing type R with type T
     */
    public <T> Either<L, T> flatMap(
            Function<? super R, ? extends Either<L, ? extends T>> rightFunction) {
        return flatMapRight(rightFunction);
    }

    /**
     * If left is defined, the given map translation function is applied.
     *
     * @param leftFunction given function which is only applied if left is defined
     * @param <T>          target type to which L is translated
     * @return result of the function translation, replacing type L with type T
     */
    public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> leftFunction) {
        Objects.requireNonNull(leftFunction);
        return new Either<>(this.left.map(l -> Objects.requireNonNull(leftFunction.apply(l))),
                this.right);
    }

    /**
     * If right is defined, the given map translation function is applied.
     *
     * @param rightFunction given function which is only applied if right is defined
     * @param <T>           target type to which R is translated
     * @return result of the function translation, replacing type R with type T
     */
    public <T> Either<L, T> mapRight(Function<? super R, ? extends T> rightFunction) {
        Objects.requireNonNull(rightFunction);
        return new Either<>(this.left,
                this.right.map(r -> Objects.requireNonNull(rightFunction.apply(r))));
    }

    /**
     * If left is defined, the given flatMap translation function is applied.
     *
     * @param leftFunction given function which is only applied if left is defined
     * @param <T>          target type to which L is translated
     * @return result of the function translation, replacing type L with type T
     */
    public <T> Either<T, R> flatMapLeft(
            Function<? super L, ? extends Either<? extends T, R>> leftFunction) {
        Objects.requireNonNull(leftFunction);
        return this.left
                .<Either<T, R>>map(
                        l -> Either.left(Objects.requireNonNull(leftFunction.apply(l)).getLeft()))
                .orElse(
                        new Either<>(
                                Optional.empty(),
                                this.right));
    }

    /**
     * If right is defined, the given flatMap translation function is applied.
     *
     * @param rightFunction given function which is only applied if right is defined
     * @param <T>           target type to which R is translated
     * @return result of the function translation, replacing type R with type T
     */
    public <T> Either<L, T> flatMapRight(
            Function<? super R, ? extends Either<L, ? extends T>> rightFunction) {
        Objects.requireNonNull(rightFunction);
        return this.right
                .<Either<L, T>>map(
                        r -> Either.right(Objects.requireNonNull(rightFunction.apply(r)).getRight()))
                .orElse(
                        new Either<>(
                                this.left,
                                Optional.empty()));
    }

    /**
     * Converge the distinct types, L and R, to a common type, T. This method's implementation is
     * right-biased.
     *
     * @param leftFunction  given function which is only applied if left is defined
     * @param rightFunction given function which is only applied if right is defined
     * @param <T>           type of the returned instance
     * @return an instance of T
     */
    public <T> T converge(
            Function<? super L, ? extends T> leftFunction,
            Function<? super R, ? extends T> rightFunction
    ) {
        Objects.requireNonNull(leftFunction);
        Objects.requireNonNull(rightFunction);

        return this.right
                .<T>map(r -> Objects.requireNonNull(rightFunction.apply(r)))
                .orElseGet(() ->
                        this.left
                                .map(l -> Objects.requireNonNull(leftFunction.apply(l)))
                                .orElseThrow(() -> new IllegalStateException("should never get here")));
    }

    /**
     * Execute the given side-effecting function depending upon which side is defined. This method's
     * implementation is right-biased.
     *
     * @param leftAction  given function is only executed if left is defined
     * @param rightAction given function is only executed if right is defined
     */
    public void forEach(Consumer<? super L> leftAction, Consumer<? super R> rightAction) {
        this.right.ifPresent(rightAction);
        this.left.ifPresent(leftAction);
    }
}
