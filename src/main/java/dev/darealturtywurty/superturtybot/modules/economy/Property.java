package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Property {
    private String name;
    private long owner;
    private String description;
    private BigInteger originalPrice;
    private List<BigInteger> upgradePrices;
    private Rent rent;
    private Loan mortgage;
    private long buyDate;

    private List<Long> previousOwners;
    private int estateTax;
    private int upgradeLevel;
    
    private Property(Builder builder) {
        this.name = builder.name;
        this.owner = builder.owner;
        this.description = builder.description;
        this.originalPrice = builder.price;
        this.upgradePrices = builder.upgradePrices;
        this.previousOwners = builder.previousOwners;
        this.rent = builder.rent;
        this.estateTax = builder.estateTax;
        this.upgradeLevel = builder.upgradeLevel;
        this.mortgage = builder.mortgage;

        this.buyDate = System.currentTimeMillis();
    }

    public BigInteger getUpgradePrice() {
        return this.upgradePrices.get(this.upgradeLevel);
    }

    public boolean isMortgaged() {
        return this.mortgage != null;
    }

    public boolean isPaidOff() {
        return !isMortgaged() || this.mortgage.isPaidOff();
    }
    
    public boolean hasRent() {
        return this.rent != null;
    }
    
    public boolean hasPreviousOwners() {
        return this.previousOwners != null && !this.previousOwners.isEmpty();
    }
    
    public boolean hasOwner() {
        return this.owner != -1;
    }

    public BigInteger calculateCurrentWorth() {
        BigInteger worth = this.originalPrice;
        if (this.upgradeLevel > 0) {
            for (int i = 0; i < this.upgradeLevel; i++) {
                worth = worth.add(this.upgradePrices.get(i));
            }
        }

        worth = worth.multiply(BigInteger.valueOf((long) (this.buyDate - System.currentTimeMillis() / 1000f / 60f / 60f / 24f)));

        if (this.mortgage != null && !this.mortgage.isPaidOff()) {
            worth = worth.subtract(this.mortgage.calculateAmountLeftToPay());
        }

        if (ThreadLocalRandom.current().nextInt(0, 100) < 10) {
            worth = worth.subtract(worth.divide(BigInteger.valueOf(100)));
        }

        return worth;
    }

    public static class Builder {
        private final String name;
        private final String description;
        private final BigInteger price;
        private final List<BigInteger> upgradePrices = new ArrayList<>();
        private final List<Long> previousOwners = new ArrayList<>();
        private final int estateTax;

        private long owner = -1;
        private Rent rent;
        private Loan mortgage;
        private int upgradeLevel = 0;

        public Builder(String name, String description, BigInteger price, int estateTax) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.estateTax = estateTax;
        }

        public Builder owner(long owner) {
            this.owner = owner;
            return this;
        }

        public Builder rent(Rent rent) {
            this.rent = rent;
            return this;
        }

        public Builder mortgage(Loan mortgage) {
            this.mortgage = mortgage;
            return this;
        }

        public Builder upgradePrice(BigInteger upgradePrice) {
            this.upgradePrices.add(upgradePrice);
            return this;
        }

        public Builder previousOwner(long previousOwner) {
            this.previousOwners.add(previousOwner);
            return this;
        }

        public Builder upgradeLevel(int upgradeLevel) {
            this.upgradeLevel = upgradeLevel;
            return this;
        }

        public Property build() {
            return new Property(this);
        }
    }
}
