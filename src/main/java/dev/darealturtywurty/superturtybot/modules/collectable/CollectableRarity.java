package dev.darealturtywurty.superturtybot.modules.collectable;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum CollectableRarity {
    COMMON(0x888888),
    UNCOMMON(0x00CC00),
    RARE(0x0000DD),
    EPIC(0xAA00AA),
    LEGENDARY(0xFFAA00),
    MYTHICAL(0xFF00FF),
    SPECIAL(0xEE0000);

    private final int color;
    private final String name;

    CollectableRarity(int color, String name) {
        this.color = color;
        this.name = name;
    }

    CollectableRarity(int color) {
        this.color = color;
        this.name = name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }

    public int calculateWeight() {
        int ordinal = ordinal();
        int size = values().length;

        return (size - ordinal) * 10;
    }
}
