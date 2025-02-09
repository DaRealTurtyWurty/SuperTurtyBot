package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registry;

import java.math.BigInteger;

public class ShopItemRegistry {
    public static final Registry<ShopItem> SHOP_ITEMS = new Registry<>();

    public static final ShopItem APPLE = SHOP_ITEMS.register("apple",
            new ShopItem.Builder().image("/economy/items/apple.png").description("An apple a day keeps the doctor away!").price(BigInteger.valueOf(50)).build());

    public static final ShopItem BANANA = SHOP_ITEMS.register("banana",
            new ShopItem.Builder().image("/economy/items/banana.png").description("Try not to get peeled!").price(BigInteger.valueOf(75)).build());

    public static final ShopItem CHERRY = SHOP_ITEMS.register("cherry",
            new ShopItem.Builder().image("/economy/items/cherry.png").description("A cherry on top of your day!").price(BigInteger.valueOf(100)).build());

    public static final ShopItem ORANGE = SHOP_ITEMS.register("orange",
            new ShopItem.Builder().image("/economy/items/orange.png").description("A juicy orange!").price(BigInteger.valueOf(125)).build());
}