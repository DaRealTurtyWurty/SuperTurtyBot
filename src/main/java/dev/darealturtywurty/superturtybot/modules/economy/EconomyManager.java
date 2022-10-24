package dev.darealturtywurty.superturtybot.modules.economy;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.mongodb.client.model.Filters;

import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class EconomyManager {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    
    public static int addMoney(Guild guild, User user, int amount) {
        return addMoney(guild, user, amount, false);
    }
    
    public static int addMoney(Guild guild, User user, int amount, boolean bank) {
        final Economy economy = fetchAccount(guild, user);
        if (economy == null)
            return -1;
        
        if (bank) {
            economy.addBank(amount);
            return economy.getBank();
        }
        
        economy.addWallet(amount);
        return economy.getWallet();
    }
    
    public static Optional<Economy> createAccount(Guild guild, User user) {
        return createAccount(guild, user, false);
    }
    
    public static Optional<Economy> createAccount(Guild guild, User user, boolean bypassCheck) {
        if (!bypassCheck && getAccount(guild, user).isEmpty())
            return Optional.empty();
        
        final var economy = new Economy(guild.getIdLong(), user.getIdLong());
        
        // TOOD: From guild config
        economy.setBank(100);
        Database.getDatabase().economy.insertOne(economy);
        return Optional.of(economy);
    }
    
    public static Economy fetchAccount(Guild guild, User user) {
        return getAccount(guild, user).orElse(createAccount(guild, user, true).get());
    }
    
    public static Optional<Economy> getAccount(Guild guild, User user) {
        return Optional.ofNullable(Database.getDatabase().economy
            .find(Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()))).first());
    }
    
    public static int getBalance(Guild guild, User user) {
        return fetchAccount(guild, user).getBalance();
    }
    
    public static int getBank(Guild guild, User user) {
        return fetchAccount(guild, user).getBank();
    }
    
    public static int getWallet(Guild guild, User user) {
        return fetchAccount(guild, user).getWallet();
    }
    
    public static int removeMoney(Guild guild, User user, int amount) {
        return removeMoney(guild, user, amount, false);
    }
    
    public static int removeMoney(Guild guild, User user, int amount, boolean bank) {
        final Economy economy = fetchAccount(guild, user);
        if (economy == null)
            return -1;
        
        if (bank) {
            economy.removeBank(amount);
            return economy.getBank();
        }
        
        economy.removeWallet(amount);
        return economy.getWallet();
    }
}
