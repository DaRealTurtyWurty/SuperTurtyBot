package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor
@Data
public class MarketplaceListing {
    private String id;
    private long guild;
    private long seller;
    private String type;
    private String item;
    private String collection;
    private BigInteger price;
    private int days;
    private long createdAt;
    private ShopItem shopItem;

    public MarketplaceListing(String id, long guild, long seller, String type, String item,
                              String collection, BigInteger price, int days, ShopItem shopItem) {
        this.id = id;
        this.guild = guild;
        this.seller = seller;
        this.type = type;
        this.item = item;
        this.collection = collection;
        this.price = price;
        this.days = days;
        this.shopItem = shopItem;
        this.createdAt = System.currentTimeMillis();
    }
}
