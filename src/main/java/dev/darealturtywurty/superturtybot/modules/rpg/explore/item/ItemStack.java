package dev.darealturtywurty.superturtybot.modules.rpg.explore.item;

import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import lombok.Getter;

@Getter
public class ItemStack {
    public static final int MAX_STACK_SIZE = 99;
    public static final ItemStack EMPTY = new ItemStack(null, 0);

    private final Item item;
    private int amount;

    public ItemStack(Item item, int amount) {
        this.item = item;
        setCount(amount);
    }

    public void setCount(int amount) {
        this.amount = MathUtils.clamp(amount, 0, MAX_STACK_SIZE);
    }

    public void grow(int amount) {
        setCount(this.amount + amount);
    }

    public void shrink(int amount) {
        setCount(this.amount - amount);
    }

    public boolean isEmpty() {
        return this.amount <= 0;
    }
}
