package dev.darealturtywurty.superturtybot.modules.rpg.explore.item;

import dev.darealturtywurty.superturtybot.core.util.object.WeightedRandomBag;
import dev.darealturtywurty.superturtybot.registry.Registry;

public class ItemRegistry {
    public static final Registry<Item> ITEMS = new Registry<>();

    public static final Item WOODEN_SWORD = ITEMS.register("wooden_sword",
            new Item("Wooden Sword", ":wooden_sword:", "A simple wooden sword."));
    public static final Item IRON_SWORD = ITEMS.register("iron_sword",
            new Item("Iron Sword", ":iron_sword:", "A simple iron sword.", ItemRarity.UNCOMMON));
    public static final Item DIAMOND_SWORD = ITEMS.register("diamond_sword",
            new Item("Diamond Sword", ":diamond_sword:", "A simple diamond sword.", ItemRarity.RARE));
    public static final Item WOODEN_SHIELD = ITEMS.register("wooden_shield",
            new Item("Wooden Shield", ":wooden_shield:", "A simple wooden shield."));
    public static final Item IRON_SHIELD = ITEMS.register("iron_shield",
            new Item("Iron Shield", ":iron_shield:", "A simple iron shield.", ItemRarity.UNCOMMON));
    public static final Item DIAMOND_SHIELD = ITEMS.register("diamond_shield",
            new Item("Diamond Shield", ":diamond_shield:", "A simple diamond shield.", ItemRarity.RARE));

    public static final Item HEALTH_POTION = ITEMS.register("health_potion",
            new Item("Health Potion", "❣️", "A potion that heals you for 50 health.", ItemRarity.UNCOMMON));
    public static final Item STRENGTH_POTION = ITEMS.register("strength_potion",
            new Item("Strength Potion", "💪", "A potion that increases your strength by 10.", ItemRarity.UNCOMMON));
    public static final Item DEFENSE_POTION = ITEMS.register("defense_potion",
            new Item("Defense Potion", "🛡️", "A potion that increases your defense by 10.", ItemRarity.UNCOMMON));

    public static final Item BREAD = ITEMS.register("bread",
            new Item("Bread", "🍞", "A simple loaf of bread.", ItemRarity.COMMON));
    public static final Item APPLE = ITEMS.register("apple",
            new Item("Apple", "🍎", "A simple apple.", ItemRarity.COMMON));
    public static final Item CARROT = ITEMS.register("carrot",
            new Item("Carrot", "🥕", "A simple carrot.", ItemRarity.COMMON));
    public static final Item POTATO = ITEMS.register("potato",
            new Item("Potato", "🥔", "A simple potato.", ItemRarity.COMMON));
    public static final Item FISH = ITEMS.register("fish",
            new Item("Fish", "🐟", "A simple fish.", ItemRarity.COMMON));
    public static final Item STEAK = ITEMS.register("steak",
            new Item("Steak", "🥩", "A simple steak.", ItemRarity.UNCOMMON));

    public static Item pickRandom() {
        return ITEMS.random().getValue();
    }

    public static Item pickRandomWeighted() {
        WeightedRandomBag<Item> bag = new WeightedRandomBag<>();
        for (Item item : ITEMS.getRegistry().values()) {
            bag.addEntry(item, item.getRarity().getWeight());
        }

        return bag.getRandom();
    }
}
