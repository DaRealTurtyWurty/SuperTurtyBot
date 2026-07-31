package dev.darealturtywurty.superturtybot.core.util.object;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Getter
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
        if (this.entries.isEmpty())
            return null;

        final double randVal = this.rand.nextDouble() * this.accumulatedWeight;
        int low = 0;
        int high = this.entries.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (this.entries.get(middle).getAccumulatedWeight() >= randVal) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }

        return this.entries.get(low);
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
    
    @Getter
    public class Entry {
        private double accumulatedWeight;
        private double weight;
        private T object;
    }
}
