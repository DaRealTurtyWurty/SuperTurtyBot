package dev.darealturtywurty.superturtybot.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

public class WeightedRandomBag<T> {
    private final List<Entry> entries = new ArrayList<>();
    
    private double accumulatedWeight;
    private final Random rand = new Random();
    
    public void addEntry(final T object, final double weight) {
        this.accumulatedWeight += weight;

        final var entry = new Entry();
        entry.object = object;
        entry.accumulatedWeight = this.accumulatedWeight;
        entry.weight = weight;

        this.entries.add(entry);
    }

    public @Nullable Entry getRandomEntry() {
        final double randVal = this.rand.nextDouble() * this.accumulatedWeight;

        for (final Entry entry : this.entries) {
            if (entry.getAccumulatedWeight() >= randVal)
                return entry;
        }

        return null;
    }

    public @Nullable T getRandom() {
        final Entry entry = this.getRandomEntry();
        return entry == null ? null : entry.getObject();
    }

    public Entry getEntry(final T object) {
        for (final Entry entry : this.entries) {
            if (Objects.equals(entry.getObject(), object))
                return entry;
        }

        return null;
    }

    public double getAccumulatedWeight() {
        return this.accumulatedWeight;
    }
    
    public class Entry {
        private double accumulatedWeight;
        private double weight;
        private T object;

        public double getAccumulatedWeight() {
            return accumulatedWeight;
        }

        public double getWeight() {
            return weight;
        }

        public T getObject() {
            return object;
        }
    }
}
