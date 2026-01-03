package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.registry.Registry;

import java.math.BigInteger;
import java.util.List;

public class PropertyRegistry {
    public static final Registry<Property> PROPERTIES = new Registry<>();

    public static final Property STARTER_FLAT = PROPERTIES.register("Starter Flat",
            new Property.Builder("Starter Flat", "A compact starter flat with basic amenities.",
                    BigInteger.valueOf(50_000), BigInteger.valueOf(2_000))
                    .rent(new Rent(BigInteger.valueOf(2_500)))
                    .upgradePrice(BigInteger.valueOf(10_000))
                    .upgradePrice(BigInteger.valueOf(20_000))
                    .upgradePrice(BigInteger.valueOf(30_000))
                    .build());

    public static final Property SUBURBAN_HOME = PROPERTIES.register("Suburban Home",
            new Property.Builder("Suburban Home", "A comfortable family home in a quiet suburb.",
                    BigInteger.valueOf(200_000), BigInteger.valueOf(7_500))
                    .rent(new Rent(BigInteger.valueOf(7_500)))
                    .upgradePrice(BigInteger.valueOf(25_000))
                    .upgradePrice(BigInteger.valueOf(50_000))
                    .upgradePrice(BigInteger.valueOf(75_000))
                    .build());

    public static final Property CITY_PENTHOUSE = PROPERTIES.register("City Penthouse",
            new Property.Builder("City Penthouse", "A luxury penthouse with skyline views.",
                    BigInteger.valueOf(1_000_000), BigInteger.valueOf(25_000))
                    .rent(new Rent(BigInteger.valueOf(25_000)))
                    .upgradePrice(BigInteger.valueOf(150_000))
                    .upgradePrice(BigInteger.valueOf(250_000))
                    .upgradePrice(BigInteger.valueOf(400_000))
                    .build());

    public static Property getByName(String name) {
        for (var entry : PROPERTIES.getRegistry().entrySet()) {
            Property property = entry.getValue();
            if (property.getName().equalsIgnoreCase(name) || entry.getKey().equalsIgnoreCase(name)) {
                return property;
            }
        }

        return null;
    }

    public static Property createForOwner(Property template, long ownerId) {
        if (template == null)
            return null;

        Property.Builder builder = new Property.Builder(
                template.getName(),
                template.getDescription(),
                template.getOriginalPrice(),
                template.getEstateTax())
                .owner(ownerId);

        if (template.getRent() != null) {
            builder.rent(new Rent(template.getRent().getBaseRent()));
        }

        List<BigInteger> upgradePrices = template.getUpgradePrices();
        if (upgradePrices != null) {
            for (BigInteger price : upgradePrices) {
                builder.upgradePrice(price);
            }
        }

        return builder.build();
    }
}
