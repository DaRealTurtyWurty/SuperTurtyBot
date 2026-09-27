package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class MarketplaceSalesRule implements QuestRule<MarketplaceSalesConfig, MarketplaceSalesState> {
    @Override
    public MarketplaceSalesState createState() {
        return new MarketplaceSalesState();
    }

    @Override
    public void apply(MarketplaceSalesConfig config, MarketplaceSalesState state, QuestEvent event) {
        switch (event) {
            case QuestEvent.MarketplaceItemListed(String listingId) -> state.listedIds.add(listingId);
            case QuestEvent.MarketplaceItemSold(String listingId) -> state.soldIds.add(listingId);
            default -> {
            }
        }
    }

    @Override
    public QuestStatus status(MarketplaceSalesConfig config, MarketplaceSalesState state) {
        int requiredSales = config.requiredSales();
        int listed = Math.min(state.listedIds.size(), requiredSales);
        long completedSales = state.soldIds.stream().filter(state.listedIds::contains).count();
        int sold = Math.toIntExact(Math.min(completedSales, requiredSales));
        int target = requiredSales * 2;
        int progress = listed + sold;
        return new QuestStatus(progress, target, sold >= requiredSales);
    }
}
