package dev.darealturtywurty.superturtybot.modules.rpg.explore.item;

import dev.darealturtywurty.superturtybot.core.util.MathUtils;

import java.util.Arrays;

public class Inventory {
    private final ItemStack[] items = new ItemStack[36];

    /**
     * Gets all the items in the inventory
     *
     * @return A copy of the items in the inventory
     */
    public ItemStack[] getItems() {
        return Arrays.copyOf(items, items.length);
    }

    /**
     * Gets an item from the inventory
     *
     * @param slot The slot to get the item from
     * @return The item in the slot
     */
    public ItemStack getItem(int slot) {
        return items[slot];
    }

    /**
     * Adds an item to the inventory
     *
     * @param stack The item to add
     * @return The remaining item that could not be added
     */
    public ItemStack addItem(ItemStack stack) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) {
                items[i] = stack;
                return ItemStack.EMPTY;
            } else if (items[i].getItem().equals(stack.getItem())) {
                // add to stack
                final int amount = MathUtils.clamp(items[i].getAmount() + stack.getAmount(), 0, ItemStack.MAX_STACK_SIZE);
                final int remaining = stack.getAmount() - (amount - items[i].getAmount());
                items[i].setCount(amount);

                if (remaining <= 0) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /**
     * Removes an item from the inventory
     *
     * @param item   The item to remove
     * @param amount The amount of the item to remove
     * @return The removed item
     */
    public ItemStack removeItem(Item item, int amount) {
        var stack = new ItemStack(item, 0);
        for (int i = this.items.length - 1; i >= 0; i--) {
            if (this.items[i] == null) {
                continue;
            }

            if (this.items[i].getItem().equals(item)) {
                final int remaining = this.items[i].getAmount() - amount;
                if (remaining <= 0) {
                    stack.grow(this.items[i].getAmount());
                    this.items[i] = null;
                    amount = Math.abs(remaining);
                } else {
                    stack.grow(amount);
                    this.items[i].shrink(amount);
                    return stack;
                }
            }
        }

        return stack;
    }
}
