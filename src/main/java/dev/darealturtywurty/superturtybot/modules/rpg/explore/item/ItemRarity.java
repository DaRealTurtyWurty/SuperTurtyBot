package dev.darealturtywurty.superturtybot.modules.rpg.explore.item;

import lombok.Getter;

import java.awt.*;

@Getter
public enum ItemRarity {
    COMMON(Color.WHITE, 1),
    UNCOMMON(Color.GREEN, 0.5),
    RARE(Color.BLUE, 0.25),
    EPIC(Color.PINK, 0.1),
    LEGENDARY(Color.ORANGE, 0.05);

    private final Color color;
    private final double weight;

    ItemRarity(Color color, double weight) {
        this.color = color;
        this.weight = weight;
    }
}
