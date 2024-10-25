package dev.darealturtywurty.superturtybot.commands.economy.property;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Loan;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PropertyManager {
    private static final Map<Long, List<Property>> GUILD_PROPERTY_MARKET = new HashMap<>();

    static {
        DailyTaskScheduler.addTask(new DailyTask(() -> {
            Calendar calendar = Calendar.getInstance();
            if (GUILD_PROPERTY_MARKET.isEmpty() || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && calendar.get(Calendar.WEEK_OF_MONTH) == 4) {
                generatePropertyMarket();
            }
        }, 0, 0));

        generatePropertyMarket(); // TODO: Check database first

        TextChannel channel = TurtyBot.getJda().getTextChannelById(1122294244917391411L);
        for (Property property : GUILD_PROPERTY_MARKET.get(1096109606452867243L)) {
            if(channel != null)
                channel.sendMessage(property.getAsReadableString()).queue();
        }
    }

    public static boolean canBuyProperty(Economy account) {
        return account.getBank() >= 10_000 && account.getProperties().size() < (int) Math.floor(EconomyManager.getCreditScore(account) * 100);
    }

    public static List<Property> getProperties(long guildId) {
        return GUILD_PROPERTY_MARKET.get(guildId);
    }

    public static List<Property> generateForBalanceIfNotExists(long guildId, long balance) {
        if (balance < 10_000)
            balance = 10_000;

        List<Property> properties = GUILD_PROPERTY_MARKET.computeIfAbsent(guildId, k -> new ArrayList<>());

        List<Property> twentyPercentBelow = new ArrayList<>();
        List<Property> twentyPercentAbove = new ArrayList<>();
        if (!properties.isEmpty()) {
            // look for properties around 20% below and 20% above the balance
            for (Property property : properties) {
                long propertyPrice = property.calculateCostToPurchase();
                if (propertyPrice <= balance * 0.8 && propertyPrice >= balance * 0.6) {
                    twentyPercentBelow.add(property);
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

        if (twentyPercentAbove.isEmpty()) {
            Property generated = generateProperty((long) (balance * 1.2));
            properties.add(generated);
            twentyPercentAbove.add(generated);
        }

        List<Property> allProperties = new ArrayList<>();
        allProperties.addAll(twentyPercentBelow);
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

    // Generate a random property name based on price
    public static String generateRandomPropertyName(boolean expensive) {
        String[] expensiveAdjectives = {
                "Imposing", "Regal", "Lavish", "Glorious", "Opulent", "Majestic",
                "Splendid", "Exclusive", "Prestigious", "Refined", "Grand",
                "Magnificent", "Exquisite", "Elegant", "Sophisticated", "Stunning",
                "Gorgeous", "Luxurious", "Serene", "Beautiful"
        };

        String[] cheapAdjectives = {
                "Unassuming", "Humble", "Basic", "Snug", "Petite", "Affordable",
                "Nook", "Modest", "Compact", "Simple", "Rustic", "Homely",
                "Neat", "Cheerful", "Welcoming", "Comfortable", "Charming",
                "Pleasant", "Cute", "Cozy", "Quaint"
        };

        String[] expensiveNouns = {
                "Stronghold", "Fortress", "Keep", "Castle", "Domain", "Oasis",
                "Hall", "Pavilion", "Sanctuary", "Penthouse", "Chateau",
                "Palace", "Manor", "Retreat", "Haven", "Residence", "Lodge",
                "Villa", "Estate", "Mansion"
        };

        String[] cheapNouns = {
                "Hutment", "Shack", "Trailer", "Nook", "Pod", "Den", "Shelter",
                "Cabin", "Barn", "Duplex", "Flat", "Apartment", "Studio",
                "Lodge", "Bungalow", "House", "Loft", "Digs", "Hut", "Cottage"
        };

        String[] materials = {
                "Plastic", "Straw", "Tin", "Aluminum", "Clay", "Copper", "Steel",
                "Ceramic", "Bamboo", "Concrete", "Glass", "Slate", "Brick",
                "Timber", "Wooden", "Stone", "Marble", "Metal", "Silver", "Gold"
        };

        String[] locationDescriptors = {
                "in the Arctic", "in the Outback", "in the Jungle", "in the Savannah",
                "in the Tropics", "in the Desert", "in the Wetlands", "in the Highlands",
                "in the Lowlands", "in the City", "in the Suburbs", "in the Mountains",
                "in the Forest", "in the Hills", "in the Foothills", "by the River",
                "by the Lake", "in the Valley", "near the Beach", "in the Countryside"
        };

        String[] adjectives = expensive ? expensiveAdjectives : cheapAdjectives;
        String[] nouns = expensive ? expensiveNouns : cheapNouns;

        String adjective = getRandomWeightedByIndex(adjectives);
        String noun = getRandomWeightedByIndex(nouns);
        String material = getRandomWeightedByIndex(materials);
        String locationDescriptor = getRandomWeightedByIndex(locationDescriptors);

        return String.join(" ", adjective, material, noun, locationDescriptor);
    }

    // Assign weights based on index (higher index means lower weight)
    private static String getRandomWeightedByIndex(String[] items) {
        int totalWeight = 0;

        for (int i = 0; i < items.length; i++) {
            totalWeight += (items.length - i);
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (int i = 0; i < items.length; i++) {
            cumulativeWeight += (items.length - i);
            if (randomIndex < cumulativeWeight) {
                return items[i];
            }
        }

        return items[0]; // Fallback (shouldn't occur)
    }

    private static String generatePropertyDescription(long price) {
        int bedrooms, bathrooms, garageSpaces, yearBuilt;
        boolean hasPool = false, hasTennisCourt = false, hasGuestHouse = false, hasHomeTheater = false;
        boolean hasPrivateIsland = false, hasElevator = false, hasHelipad = false;
        boolean hasGolfCourse = false, hasWineCellar = false, hasLibrary = false;
        boolean hasBowlingAlley = false, hasIndoorBasketballCourt = false, hasIndoorPool = false;
        boolean hasSmartHomeFeatures = false, hasPrivateSecurity = false, hasUndergroundParking = false, hasGym = false;

        var random = new Random();
        if (price < 500_000) {  // Low-end properties
            bedrooms = random.nextInt(1, 3);
            bathrooms = random.nextInt(1, 2);
            garageSpaces = random.nextBoolean() ? 1 : 0;
            yearBuilt = random.nextInt(Calendar.getInstance().get(Calendar.YEAR) - 150, Calendar.getInstance().get(Calendar.YEAR) - 50);
        } else if (price < 2_000_000) {  // Mid-range properties
            bedrooms = random.nextInt(3, 5);
            bathrooms = random.nextInt(2, 4);
            garageSpaces = random.nextInt(1, 3);
            yearBuilt = random.nextInt(Calendar.getInstance().get(Calendar.YEAR) - 75, Calendar.getInstance().get(Calendar.YEAR) - 20);
            hasPool = random.nextBoolean();
        } else if (price < 10_000_000) {  // High-end properties
            bedrooms = random.nextInt(5, 8);
            bathrooms = random.nextInt(4, 6);
            garageSpaces = random.nextInt(2, 4);
            yearBuilt = random.nextInt(Calendar.getInstance().get(Calendar.YEAR) - 50, Calendar.getInstance().get(Calendar.YEAR));
            hasPool = true;
            hasTennisCourt = random.nextBoolean();
            hasGuestHouse = random.nextBoolean();
            hasHomeTheater = random.nextBoolean();
        } else if (price < 500_000_000) {
            bedrooms = random.nextInt(8, 12);
            bathrooms = random.nextInt(6, 8);
            garageSpaces = random.nextInt(4, 8);
            yearBuilt = random.nextInt(Calendar.getInstance().get(Calendar.YEAR) - 25, Calendar.getInstance().get(Calendar.YEAR));
            hasPool = true;
            hasTennisCourt = true;
            hasGuestHouse = true;
            hasHomeTheater = true;
            hasElevator = random.nextBoolean();
            hasHelipad = random.nextBoolean();
            hasPrivateIsland = random.nextBoolean();
        } else if (price < 1_000_000_000_000L) {  // Billion-dollar properties
            bedrooms = random.nextInt(12, 20);
            bathrooms = random.nextInt(10, 15);
            garageSpaces = random.nextInt(8, 12);
            yearBuilt = random.nextInt(Calendar.getInstance().get(Calendar.YEAR) - 10, Calendar.getInstance().get(Calendar.YEAR));
            hasPool = true;
            hasTennisCourt = true;
            hasGuestHouse = true;
            hasHomeTheater = true;
            hasElevator = true;
            hasHelipad = true;
            hasPrivateIsland = true;
            hasGolfCourse = random.nextBoolean();
            hasWineCellar = random.nextBoolean();
            hasLibrary = random.nextBoolean();
            hasBowlingAlley = random.nextBoolean();
            hasIndoorBasketballCourt = random.nextBoolean();
            hasIndoorPool = random.nextBoolean();
        } else {  // Trillion-dollar properties
            bedrooms = random.nextInt(20, 30);
            bathrooms = random.nextInt(15, 25);
            garageSpaces = random.nextInt(30, 50);
            yearBuilt = random.nextInt(Calendar.getInstance().get(Calendar.YEAR) - 5, Calendar.getInstance().get(Calendar.YEAR));
            hasPool = true;
            hasTennisCourt = true;
            hasGuestHouse = true;
            hasHomeTheater = true;
            hasElevator = true;
            hasHelipad = true;
            hasPrivateIsland = true;
            hasGolfCourse = true;
            hasWineCellar = true;
            hasLibrary = true;
            hasBowlingAlley = true;
            hasIndoorBasketballCourt = true;
            hasIndoorPool = true;
            hasSmartHomeFeatures = random.nextBoolean();
            hasPrivateSecurity = random.nextBoolean();
            hasUndergroundParking = random.nextBoolean();
            hasGym = random.nextBoolean();
        }

        // Build the description
        var description = new StringBuilder();
        description.append(bedrooms).append(" bedroom");
        if (bedrooms > 1)
            description.append("s");

        description.append(", ").append(bathrooms).append(" bathroom");
        if (bathrooms > 1)
            description.append("s");

        description.append(", ");
        description.append(garageSpaces == 0 ? "no garage" : garageSpaces + " car garage");
        description.append(", built in ").append(yearBuilt).append(". ");

        // Add luxury features based on price
        if (hasPool) {
            description.append("Includes a pool. ");
        }

        if (hasTennisCourt) {
            description.append("Features a tennis court. ");
        }

        if (hasGuestHouse) {
            description.append("Comes with a guest house. ");
        }

        if (hasHomeTheater) {
            description.append("Equipped with a home theater. ");
        }

        if (hasElevator) {
            description.append("Includes a private elevator. ");
        }

        if (hasHelipad) {
            description.append("Features a helipad for easy access. ");
        }

        if (hasPrivateIsland) {
            description.append("Includes a private island for ultimate luxury. ");
        }

        if (hasGolfCourse) {
            description.append("Features a private golf course. ");
        }

        if (hasWineCellar) {
            description.append("Includes a wine cellar. ");
        }

        if (hasLibrary) {
            description.append("Equipped with a private library. ");
        }

        if (hasBowlingAlley) {
            description.append("Features a private bowling alley. ");
        }

        if (hasIndoorBasketballCourt) {
            description.append("Includes an indoor basketball court. ");
        }

        if (hasIndoorPool) {
            description.append("Features an indoor pool. ");
        }

        if (hasSmartHomeFeatures) {
            description.append("Equipped with smart home features for modern living. ");
        }

        if (hasPrivateSecurity) {
            description.append("Includes private security for peace of mind. ");
        }

        if (hasUndergroundParking) {
            description.append("Features underground parking for convenience. ");
        }

        if (hasGym) {
            description.append("Includes a private gym. ");
        }

        return description.toString().trim();
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

    private static void generatePropertyMarket() {
        GUILD_PROPERTY_MARKET.clear();

        List<GuildData> configuredGuilds = Database.getDatabase().guildData.find().into(new ArrayList<>());
        for (GuildData guildData : configuredGuilds) {
            if (!guildData.isEconomyEnabled() || guildData.getGuild() != 1096109606452867243L)
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

    public static Optional<Property> findProperty(long guild, String name) {
        List<Property> properties = GUILD_PROPERTY_MARKET.get(guild);
        if (properties == null)
            return Optional.empty();

        return properties.stream().filter(property -> property.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static long calculatePropertyCost(Property property) {
        return property.calculateCostToPurchase();
    }

    public static List<String> getPropertyNames(long guild) {
        List<Property> properties = GUILD_PROPERTY_MARKET.get(guild);
        if (properties == null)
            return Collections.emptyList();

        List<String> propertyNames = new ArrayList<>();
        for (Property property : properties) {
            propertyNames.add(property.getName());
        }

        return propertyNames;
    }
}
