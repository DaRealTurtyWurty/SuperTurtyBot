package dev.darealturtywurty.superturtybot.core.util.object;

import org.apache.commons.lang3.tuple.Pair;

public class CoupledPair<T> extends Pair<T, T> {
    private final T left;
    private final T right;

    public CoupledPair(T left, T right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public T getLeft() {
        return this.left;
    }

    @Override
    public T getRight() {
        return this.right;
    }

    @Override
    public T setValue(T value) {
        throw new UnsupportedOperationException("Cannot set value of CoupledPair!");
    }
}
