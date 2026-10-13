package dev.darealturtywurty.superturtybot.commands.levelling;

import dev.darealturtywurty.superturtybot.core.util.object.WeightedRandomBag;
import dev.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RankCardRewards {
    private RankCardRewards() {
    }

    public static boolean shouldDrop(int chance, int roll) {
        return roll < Math.clamp(chance, 0, 100);
    }

    public static @Nullable RankCardItem chooseReward(List<String> inventory) {
        WeightedRandomBag<RankCardItem> bag = new WeightedRandomBag<>();
        RankCardItemRegistry.RANK_CARD_ITEMS.values().stream()
            .filter(item -> !inventory.contains(item.getName()))
            .forEach(item -> bag.addEntry(item, item.rarity.chance));
        return bag.getRandom();
    }
}
