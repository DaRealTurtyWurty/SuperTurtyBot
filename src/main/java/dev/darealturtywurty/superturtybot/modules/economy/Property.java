package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registerable;
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
public class Property implements Registerable {
    private String name;
    private long owner;
    private String description;
    private BigInteger originalPrice;
    private List<BigInteger> upgradePrices;
    private Rent rent;
    private Loan mortgage;
    private long buyDate;
    private long renter;
    private long rentEndsAt;
    private String renterName;
    private List<RenterOffer> renterOffers = new ArrayList<>();
    private long nextBestRenterAt;

    private List<Long> previousOwners;
    private BigInteger estateTax;
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
        this.renter = builder.renter;
        this.rentEndsAt = builder.rentEndsAt;
        this.renterName = builder.renterName;
        this.renterOffers = builder.renterOffers;
        this.nextBestRenterAt = builder.nextBestRenterAt;

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

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }

    public boolean hasRenter() {
        return this.renter != -1;
    }

    public boolean isRentActive() {
        return hasRenter() && this.rentEndsAt > System.currentTimeMillis();
    }

    public void clearRenter() {
        this.renter = -1;
        this.rentEndsAt = 0L;
        this.renterName = null;
        if (this.renterOffers != null) {
            this.renterOffers.clear();
        }
    }

    public BigInteger calculateCurrentWorth() {
        BigInteger worth = this.originalPrice;
        if (this.upgradeLevel > 0) {
            for (int i = 0; i < this.upgradeLevel; i++) {
                worth = worth.add(this.upgradePrices.get(i));
            }
        }

        long daysOwned = Math.max(0, (System.currentTimeMillis() - this.buyDate) / 86_400_000L);
        if (daysOwned > 0) {
            worth = worth.add(this.originalPrice.multiply(BigInteger.valueOf(daysOwned)).divide(BigInteger.valueOf(1000)));
        }

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
        private final BigInteger estateTax;

        private long owner = -1;
        private Rent rent;
        private Loan mortgage;
        private int upgradeLevel = 0;
        private long renter = -1;
        private long rentEndsAt = 0L;
        private String renterName;
        private List<RenterOffer> renterOffers = new ArrayList<>();
        private long nextBestRenterAt = 0L;

        public Builder(String name, String description, BigInteger price, BigInteger estateTax) {
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

        public Builder renter(long renter) {
            this.renter = renter;
            return this;
        }

        public Builder rentEndsAt(long rentEndsAt) {
            this.rentEndsAt = rentEndsAt;
            return this;
        }

        public Builder renterName(String renterName) {
            this.renterName = renterName;
            return this;
        }

        public Builder renterOffer(RenterOffer offer) {
            this.renterOffers.add(offer);
            return this;
        }

        public Builder nextBestRenterAt(long nextBestRenterAt) {
            this.nextBestRenterAt = nextBestRenterAt;
            return this;
        }

        public Property build() {
            return new Property(this);
        }
    }
}
