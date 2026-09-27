package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MarketplaceListing;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import org.bson.conversions.Bson;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All marketplace mutations run in MongoDB transactions so payment, listing and ownership move together.
 */
public final class MarketplaceService {
    public static final String COLLECTABLE = "collectable";
    public static final String RANK_CARD = "rank_card";
    public static final String ECONOMY_ITEM = "economy_item";
    public static final String RENTAL = "rental";

    private MarketplaceService() {
    }

    public static MarketplaceListing list(long guild, long seller, String type, String collection,
                                          String item, BigInteger price, int days) {
        if (price == null || price.signum() <= 0)
            throw new IllegalArgumentException("Price must be positive.");

        if (item == null || item.isBlank())
            throw new IllegalArgumentException("Specify an item.");

        if (!List.of(COLLECTABLE, RANK_CARD, ECONOMY_ITEM, RENTAL).contains(type))
            throw new IllegalArgumentException("Unknown listing type.");

        if (RENTAL.equals(type) && (days < 1 || days > 30))
            throw new IllegalArgumentException("Rental length must be between 1 and 30 days.");

        Database db = Database.getDatabase();
        try (ClientSession session = db.startSession()) {
            return session.withTransaction(() -> {
                ShopItem escrowItem = null;
                switch (type) {
                    case COLLECTABLE -> {
                        if (collection == null || collection.isBlank())
                            throw new IllegalArgumentException("Specify a collectable collection.");

                        UserCollectables owned = db.userCollectables.find(session, Filters.eq("user", seller)).first();
                        if (owned == null || owned.getCollectables() == null)
                            throw new IllegalArgumentException("You do not own that collectable.");

                        UserCollectables.Collectables group = owned.getCollectables().stream()
                                .filter(value -> collection.equals(value.getType())).findFirst().orElse(null);
                        if (group == null || group.getCollectables() == null || !group.getCollectables().remove(item))
                            throw new IllegalArgumentException("You do not own that collectable.");

                        db.userCollectables.replaceOne(session, Filters.eq("user", seller), owned);
                    }
                    case RANK_CARD -> {
                        Levelling level = level(session, db, guild, seller);
                        if (level == null || level.getInventory() == null || !level.getInventory().remove(item))
                            throw new IllegalArgumentException("You do not own that rank-card item.");

                        db.levelling.replaceOne(session, userFilter(guild, seller), level);
                    }
                    case ECONOMY_ITEM -> {
                        Economy account = account(session, db, guild, seller);
                        if (account == null || account.getShopItems() == null)
                            throw new IllegalArgumentException("You do not own that economy item.");

                        escrowItem = account.getShopItems().stream()
                                .filter(value -> item.equals(String.valueOf(value.getId()))).findFirst().orElse(null);
                        if (escrowItem == null)
                            throw new IllegalArgumentException("You do not own that economy item.");

                        account.getShopItems().remove(escrowItem);
                        db.economy.replaceOne(session, userFilter(guild, seller), account);
                    }
                    case RENTAL -> {
                        Economy account = account(session, db, guild, seller);
                        Property property = property(account, item);
                        if (property == null || property.isRentActive() || property.getRent() == null || property.getRent().isPaused())
                            throw new IllegalArgumentException("That property is not available to rent.");
                    }
                    default -> throw new IllegalArgumentException("Unknown listing type.");
                }
                var listing = new MarketplaceListing(UUID.randomUUID().toString(), guild, seller,
                        type, item, collection == null ? "" : collection, price, days, escrowItem);
                db.marketplaceListings.insertOne(session, listing);
                return listing;
            });
        }
    }

    public static MarketplaceListing cancel(long guild, long seller, String id) {
        Database db = Database.getDatabase();
        try (ClientSession session = db.startSession()) {
            return session.withTransaction(() -> {
                MarketplaceListing listing = db.marketplaceListings.find(session, Filters.and(
                        Filters.eq("_id", id), Filters.eq("guild", guild), Filters.eq("seller", seller))).first();
                if (listing == null)
                    throw new IllegalArgumentException("Listing not found or not yours.");

                if (!RENTAL.equals(listing.getType())) {
                    deliver(session, db, listing, seller);
                }
                db.marketplaceListings.deleteOne(session, Filters.eq("_id", id));
                return listing;
            });
        }
    }

    public static MarketplaceListing buy(long guild, long buyer, String id) {
        Database db = Database.getDatabase();
        try (ClientSession session = db.startSession()) {
            return session.withTransaction(() -> {
                MarketplaceListing listing = db.marketplaceListings.find(session, Filters.and(
                        Filters.eq("_id", id), Filters.eq("guild", guild))).first();
                if (listing == null)
                    throw new IllegalArgumentException("Listing not found.");

                if (listing.getSeller() == buyer)
                    throw new IllegalArgumentException("You cannot buy your own listing.");

                Economy buyerAccount = account(session, db, guild, buyer);
                if (buyerAccount == null) {
                    buyerAccount = createAccount(session, db, guild, buyer);
                }

                if (buyerAccount.isImprisoned())
                    throw new IllegalArgumentException("You cannot buy while imprisoned.");

                BigInteger price = listing.getPrice();
                if (price == null || price.signum() <= 0)
                    throw new IllegalArgumentException("Invalid listing price.");

                if (EconomyManager.getBalance(buyerAccount).compareTo(price) < 0)
                    throw new IllegalArgumentException("You do not have enough money.");

                Economy sellerAccount = account(session, db, guild, listing.getSeller());
                if (sellerAccount == null) {
                    sellerAccount = createAccount(session, db, guild, listing.getSeller());
                }

                if (RENTAL.equals(listing.getType())) {
                    Property property = property(sellerAccount, listing.getItem());
                    if (property == null || property.isRentActive() || property.getRent() == null || property.getRent().isPaused())
                        throw new IllegalArgumentException("That property is no longer available to rent.");

                    property.setRenter(buyer);
                    property.setRenterName(null);
                    property.setRentEndsAt(System.currentTimeMillis() + listing.getDays() * 86_400_000L);
                    property.setRenterOffers(new ArrayList<>());
                } else if (ECONOMY_ITEM.equals(listing.getType())) {
                    if (buyerAccount.getShopItems() == null) {
                        buyerAccount.setShopItems(new ArrayList<>());
                    }
                    buyerAccount.getShopItems().add(listing.getShopItem());
                } else {
                    deliver(session, db, listing, buyer);
                }

                EconomyManager.removeBalance(buyerAccount, price);
                sellerAccount.addWallet(price);
                buyerAccount.addTransaction(price.negate(), MoneyTransaction.MARKETPLACE, listing.getSeller());
                sellerAccount.addTransaction(price, MoneyTransaction.MARKETPLACE, buyer);
                db.economy.replaceOne(session, userFilter(guild, buyer), buyerAccount);
                db.economy.replaceOne(session, userFilter(guild, listing.getSeller()), sellerAccount);
                db.marketplaceListings.deleteOne(session, Filters.eq("_id", id));
                return listing;
            });
        }
    }

    public static List<MarketplaceListing> browse(long guild, String type, int limit) {
        Bson filter = type == null ? Filters.eq("guild", guild)
                : Filters.and(Filters.eq("guild", guild), Filters.eq("type", type));
        return Database.getDatabase().marketplaceListings.find(filter)
                .sort(Sorts.descending("createdAt"))
                .limit(limit).into(new ArrayList<>());
    }

    public static List<MarketplaceListing> browseSeller(long guild, long seller, int limit) {
        return Database.getDatabase().marketplaceListings.find(Filters.and(
                        Filters.eq("guild", guild), Filters.eq("seller", seller)))
                .sort(Sorts.descending("createdAt"))
                .limit(limit).into(new ArrayList<>());
    }

    private static void deliver(ClientSession session, Database db, MarketplaceListing listing, long recipient) {
        switch (listing.getType()) {
            case COLLECTABLE -> {
                UserCollectables owned = db.userCollectables.find(session, Filters.eq("user", recipient)).first();
                boolean newRecord = owned == null;
                if (newRecord) {
                    owned = new UserCollectables(recipient);
                }

                if (owned.getCollectables() == null) {
                    owned.setCollectables(new ArrayList<>());
                }

                String collection = listing.getCollection();
                UserCollectables.Collectables group = owned.getCollectables().stream()
                        .filter(value -> collection.equals(value.getType())).findFirst().orElse(null);
                if (group == null) {
                    group = new UserCollectables.Collectables(collection);
                    owned.getCollectables().add(group);
                }

                if (group.getCollectables() == null) {
                    group.setCollectables(new ArrayList<>());
                }
                group.getCollectables().add(listing.getItem());
                if (newRecord) {
                    db.userCollectables.insertOne(session, owned);
                } else {
                    db.userCollectables.replaceOne(session, Filters.eq("user", recipient), owned);
                }
            }
            case RANK_CARD -> {
                Levelling level = level(session, db, listing.getGuild(), recipient);
                boolean newRecord = level == null;
                if (newRecord) {
                    level = new Levelling(listing.getGuild(), recipient);
                }

                if (level.getInventory() == null) {
                    level.setInventory(new ArrayList<>());
                }

                level.getInventory().add(listing.getItem());
                if (newRecord) {
                    db.levelling.insertOne(session, level);
                } else {
                    db.levelling.replaceOne(session, userFilter(listing.getGuild(), recipient), level);
                }
            }
            case ECONOMY_ITEM -> {
                Economy account = account(session, db, listing.getGuild(), recipient);
                if (account == null) {
                    account = new Economy(listing.getGuild(), recipient);
                    db.economy.insertOne(session, account);
                }

                if (account.getShopItems() == null) {
                    account.setShopItems(new ArrayList<>());
                }

                account.getShopItems().add(listing.getShopItem());
                db.economy.replaceOne(session, userFilter(listing.getGuild(), recipient), account);
            }
            default -> throw new IllegalArgumentException("Unknown listing type.");
        }
    }

    private static Economy account(ClientSession session, Database db, long guild, long user) {
        return db.economy.find(session, userFilter(guild, user)).first();
    }

    private static Economy createAccount(ClientSession session, Database db, long guild, long user) {
        GuildData settings = db.guildData.find(session, Filters.eq("guild", guild)).first();
        BigInteger startingBalance = settings == null || settings.getDefaultEconomyBalance() == null
                ? BigInteger.ZERO : settings.getDefaultEconomyBalance();
        Economy account = new Economy(guild, user);
        account.setBank(startingBalance);
        account.addTransaction(startingBalance, MoneyTransaction.CREATE_ACCOUNT);
        db.economy.insertOne(session, account);
        return account;
    }

    private static Levelling level(ClientSession session, Database db, long guild, long user) {
        return db.levelling.find(session, userFilter(guild, user)).first();
    }

    private static Property property(Economy account, String name) {
        return account == null || account.getProperties() == null ? null : account.getProperties().stream()
                .filter(value -> name.equalsIgnoreCase(value.getName()) && value.getOwner() == account.getUser())
                .findFirst().orElse(null);
    }

    private static Bson userFilter(long guild, long user) {
        return Filters.and(Filters.eq("guild", guild), Filters.eq("user", user));
    }
}
