package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Data;

import java.math.BigInteger;

@Data
public class ShopItem implements Registerable {
    private static int ID = 0;

    public ShopItem() {
        this(0, "", "N/A", BigInteger.ONE, BigInteger.ONE);
    }

    private ShopItem(int id, String image, String description, BigInteger startPrice, BigInteger currentPrice) {
        setId(id);
        setImage(image);
        setDescription(description);
        setPrice(currentPrice);
        setOriginalPrice(startPrice);
    }

    private int id;
    private String name = "Unknown";
    private String image;
    private String description;
    private BigInteger originalPrice;
    private BigInteger price;

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }

    public static class Builder {
        private final int id;

        private BigInteger startPrice = BigInteger.ONE;
        private BigInteger currentPrice = BigInteger.ONE;
        private String image = "";
        private String description = "N/A";

        public Builder() {
            this.id = ID++;
        }

        public Builder startPrice(BigInteger price) {
            this.startPrice = price;
            return this;
        }

        public Builder currentPrice(BigInteger price) {
            this.currentPrice = price;
            return this;
        }

        public Builder price(BigInteger price) {
            return startPrice(price).currentPrice(price);
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ShopItem build() {
            return new ShopItem(this.id, this.image, this.description, this.startPrice, this.currentPrice);
        }
    }
}