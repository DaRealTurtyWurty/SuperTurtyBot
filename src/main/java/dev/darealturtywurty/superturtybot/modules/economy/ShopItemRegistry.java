package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registry;

public class ShopItemRegistry {
    public static final Registry<ShopItem> SHOP_ITEMS = new Registry<>();

    public static final ShopItem APPLE = SHOP_ITEMS.register("apple",
            new ShopItem.Builder().emoji("🍎").description("An apple a day keeps the doctor away!").price(50).build());

    public static final ShopItem BANANA = SHOP_ITEMS.register("banana",
            new ShopItem.Builder().emoji("🍌").description("Try not to get peeled!").price(75).build());

    public static final ShopItem CHERRY = SHOP_ITEMS.register("cherry",
            new ShopItem.Builder().emoji("🍒").description("A cherry on top of your day!").price(100).build());

    public static final ShopItem ORANGE = SHOP_ITEMS.register("orange",
            new ShopItem.Builder().emoji("🍊").description("A juicy orange!").price(125).build());
}
