package dev.darealturtywurty.superturtybot.modules.rpg.explore.item;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Getter;

@Getter
public class Item implements Registerable {
    private String name;
    private final String emoji;
    private final String description;
    private final ItemRarity rarity;

    public Item(String name, String emoji, String description) {
        this(name, emoji, description, ItemRarity.COMMON);
    }

    public Item(String name, String emoji, String description, ItemRarity rarity) {
        this.name = name;
        this.emoji = emoji;
        this.description = description;
        this.rarity = rarity;
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }
}
