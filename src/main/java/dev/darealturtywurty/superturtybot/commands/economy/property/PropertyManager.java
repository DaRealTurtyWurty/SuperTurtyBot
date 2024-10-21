package dev.darealturtywurty.superturtybot.commands.economy.property;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Loan;
import dev.darealturtywurty.superturtybot.modules.economy.Property;

import java.util.*;

public class PropertyManager {
    private static final Map<Long, List<Property>> GUILD_PROPERTY_MARKET = new HashMap<>();

    static {
        DailyTaskScheduler.addTask(new DailyTask(() -> {
            Calendar calendar = Calendar.getInstance();
            if (GUILD_PROPERTY_MARKET.isEmpty() || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && calendar.get(Calendar.WEEK_OF_MONTH) == 4) {
                GUILD_PROPERTY_MARKET.clear();

                // Generate new properties
                List<GuildData> configuredGuilds = Database.getDatabase().guildData.find().into(new ArrayList<>());
                for (GuildData guildData : configuredGuilds) {
                    if (!guildData.isEconomyEnabled())
                        continue;

                    GUILD_PROPERTY_MARKET.put(guildData.getGuild(), new ArrayList<>());
                }

                for (Map.Entry<Long, List<Property>> entry : GUILD_PROPERTY_MARKET.entrySet()) {
                    long guildId = entry.getKey();
                    List<Property> properties = entry.getValue();

                    List<Economy> accounts = Database.getDatabase().economy.find(Filters.eq("guild", guildId)).into(new ArrayList<>());
                    for (Economy account : accounts) {
                        properties.addAll(generateForBalanceIfNotExists(guildId, account.getBank()));
                    }

                    // TODO: Save property market to database
                    // EconomyManager.saveProperties(guildId, properties);
                }
            }
        }, 0, 0));
    }

    public static boolean canBuyProperty(Economy account) {
        return account.getBank() >= 10_000 && account.getProperties().size() < EconomyManager.getCreditScore(account) * 100;
    }

    public static List<Property> getProperties(long guildId) {
        return GUILD_PROPERTY_MARKET.get(guildId);
    }

    public static List<Property> generateForBalanceIfNotExists(long guildId, long balance) {
        if(balance < 10_000)
            balance = 10_000;

        List<Property> properties = GUILD_PROPERTY_MARKET.computeIfAbsent(guildId, k -> new ArrayList<>());

        List<Property> twentyPercentBelow = new ArrayList<>();
        List<Property> tenPercentBelow = new ArrayList<>();
        List<Property> tenPercentAbove = new ArrayList<>();
        List<Property> twentyPercentAbove = new ArrayList<>();
        if (!properties.isEmpty()) {
            // look for properties around 20% below, 10% below, 10% above, and 20% above the balance
            for (Property property : properties) {
                long propertyPrice = property.calculateCostToPurchase();
                if (propertyPrice <= balance * 0.8 && propertyPrice >= balance * 0.6) {
                    twentyPercentBelow.add(property);
                } else if (propertyPrice <= balance * 0.9 && propertyPrice >= balance * 0.8) {
                    tenPercentBelow.add(property);
                } else if (propertyPrice <= balance * 1.1 && propertyPrice >= balance * 0.9) {
                    tenPercentAbove.add(property);
                } else if (propertyPrice <= balance * 1.2 && propertyPrice >= balance * 1.1) {
                    twentyPercentAbove.add(property);
                }
            }
        }

        // Generate new properties if there are none around the balance
        if (twentyPercentBelow.isEmpty()) {
            Property generated = generateProperty((long) (balance * 0.8));
            properties.add(generated);
            twentyPercentBelow.add(generated);
        }

        if (tenPercentBelow.isEmpty()) {
            Property generated = generateProperty((long) (balance * 0.9));
            properties.add(generated);
            tenPercentBelow.add(generated);
        }

        if (tenPercentAbove.isEmpty()) {
            Property generated = generateProperty((long) (balance * 1.1));
            properties.add(generated);
            tenPercentAbove.add(generated);
        }

        if (twentyPercentAbove.isEmpty()) {
            Property generated = generateProperty((long) (balance * 1.2));
            properties.add(generated);
            twentyPercentAbove.add(generated);
        }

        List<Property> allProperties = new ArrayList<>();
        allProperties.addAll(twentyPercentBelow);
        allProperties.addAll(tenPercentBelow);
        allProperties.addAll(tenPercentAbove);
        allProperties.addAll(twentyPercentAbove);
        return allProperties;
    }

    private static Property generateProperty(long roughPrice) {
        long price = (long) (roughPrice * (0.9 + Math.random() * 0.2));
        price = MathUtils.roundToNearest(price, 1000);

        String name = generateRandomPropertyName(price > 10_000_000);
        String description = generatePropertyDescription(price);
        long estateTax = calculateEstateTax(price);
        Loan mortgage = generateMortgage(price);

        return new Property.Builder(name, description, price, estateTax)
                .mortgage(mortgage)
                .build();
    }

    // TODO: Make this weighted and more realistic
    private static String generateRandomPropertyName(boolean expensive) {
        String[] expensiveAdjectives = {"Luxurious", "Beautiful", "Stunning", "Gorgeous", "Elegant", "Exquisite", "Majestic", "Grand", "Magnificent", "Opulent"};
        String[] cheapAdjectives = {"Simple", "Basic", "Modest", "Cozy", "Quaint", "Charming", "Cute", "Rustic", "Homely", "Comfortable"};
        String[] expensiveNouns = {"Mansion", "Estate", "Palace", "Castle", "Manor", "Villa", "Chateau", "Fortress", "Stronghold", "Keep"};
        String[] cheapNouns = {"House", "Cottage", "Cabin", "Shack", "Hut", "Bungalow", "Cabin", "Cabin", "Cabin", "Cabin"};

        String[] adjectives = expensive ? expensiveAdjectives : cheapAdjectives;
        String[] nouns = expensive ? expensiveNouns : cheapNouns;

        String adjective = adjectives[(int) (Math.random() * adjectives.length)];
        String noun = nouns[(int) (Math.random() * nouns.length)];
        return adjective + " " + noun;
    }

    private static String generatePropertyDescription(long price) {
        int roomCount = price > 1_000_000 ? (int) (Math.random() * 10) + 5 : (int) (Math.random() * 5) + 1;
        int bedroomCount = (int) (Math.random() * roomCount) + 1;
        int bathroomCount = (int) (Math.random() * bedroomCount) + 1;
        long carSpaces = (price / 1_000_000) + 1;
        int yearBuilt = (int) (Math.random() * 100) + 1920;

        return "This property has " + roomCount + " rooms, " + bedroomCount + " bedrooms, " + bathroomCount + " bathrooms, " + carSpaces + " car spaces, and was built in " + yearBuilt + ".";
    }

    private static long calculateEstateTax(long price) {
        return (long) (price * 0.01);
    }

    private static Loan generateMortgage(long price) {
        long mortgageAmount = (long) (price * (0.5 + Math.random() * 0.5));
        double interestRate = 0.05 + Math.random() * 0.1;
        long timeToPayOff = ((long) (Math.random() * 30) + 10) * 1000 * 60 * 60 * 24 * 30;

        return new Loan(UUID.randomUUID().toString(), mortgageAmount, interestRate, 0, timeToPayOff);
    }
}
