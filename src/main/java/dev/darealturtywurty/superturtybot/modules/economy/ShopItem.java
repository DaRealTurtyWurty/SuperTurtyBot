package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Data;

@Data
public class ShopItem implements Registerable {
    private static int ID = 0;

    public ShopItem() {
        this(0, "", "N/A", 1, 1);
    }

    private ShopItem(int id, String image, String description, int startPrice, int currentPrice) {
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
    private int originalPrice;
    private int price;

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }

    public static class Builder {
        private final int id;

        private int startPrice = 1, currentPrice = 1;
        private String image = "";
        private String description = "N/A";

        public Builder() {
            this.id = ID++;
        }

        public Builder startPrice(int price) {
            this.startPrice = price;
            return this;
        }

        public Builder currentPrice(int price) {
            this.currentPrice = price;
            return this;
        }

        public Builder price(int price) {
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