package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registry;

public class ShopItemRegistry {
    public static final Registry<ShopItem> SHOP_ITEMS = new Registry<>();

    public static final ShopItem APPLE = SHOP_ITEMS.register("apple",
            new ShopItem.Builder().emoji("🍎").description("An apple a day keeps the doctor away!").price(50).build());
}
