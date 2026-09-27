package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record MarketplaceSalesConfig(int requiredSales) {
    public MarketplaceSalesConfig {
        if (requiredSales < 1)
            throw new IllegalArgumentException("Required sales must be greater than 0");
    }
}
