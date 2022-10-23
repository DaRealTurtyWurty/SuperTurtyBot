package dev.darealturtywurty.superturtybot.commands.levelling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

// TODO: Preset rank card system, and store the rank cards in here for easy access
public class XPInventory extends ArrayList<RankCardItem> {
    private static final long serialVersionUID = -8380703374521460050L;
    private final List<Long> timeAdded = new ArrayList<>();

    @Override
    public void add(int index, RankCardItem item) {
        super.add(index, item);
        this.timeAdded.add(index, System.currentTimeMillis());
    }

    @Override
    public boolean add(RankCardItem item) {
        if (super.add(item))
            return this.timeAdded.add(System.currentTimeMillis());

        return false;
    }

    @NotNull
    public List<RankCardItem> find(RankCardItem.Rarity rarity) {
        return stream().filter(item -> item.rarity == rarity).toList();
    }

    @NotNull
    public List<RankCardItem> find(RankCardItem.Type type) {
        return stream().filter(item -> item.type == type).toList();
    }

    public long getTimeAdded(int index) {
        return this.timeAdded.get(index);
    }

    public long getTimeAdded(RankCardItem item) {
        return this.timeAdded.get(indexOf(item));
    }

    public boolean isNew(RankCardItem item) {
        if (!contains(item))
            return false;

        final int index = indexOf(item);
        final long time = this.timeAdded.get(index);
        final long current = System.currentTimeMillis();
        return toHours(current) - toHours(time) < 3f;
    }

    @Override
    public RankCardItem remove(int index) {
        try {
            Objects.checkIndex(index, size());
            this.timeAdded.remove(index);
            return super.remove(index);
        } catch (final IndexOutOfBoundsException exception) {
            this.timeAdded.remove(index);
            return super.remove(index);
        }
    }

    @Override
    public boolean remove(Object item) {
        final int index = indexOf(item);
        final boolean success = super.remove(item);
        if (success && index != -1) {
            this.timeAdded.remove(index);
        }

        return success;
    }

    @NotNull
    public List<RankCardItem> sortByName(boolean decending) {
        return stream().sorted(Comparator.comparing(RankCardItem::getName)).toList();
    }

    @NotNull
    public List<RankCardItem> sortByRarity(boolean decending) {
        return stream().sorted((item0, item1) -> Integer.compare(item0.rarity.ordinal(), item1.rarity.ordinal()))
            .toList();
    }

    private static float toHours(long time) {
        return time / 1000f / 60f / 60f;
    }
}
