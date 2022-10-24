package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EconomyManager {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static Optional<Economy> createAccount(Guild guild, User user) {
        return createAccount(guild, user, false);
    }

    public static Optional<Economy> createAccount(Guild guild, User user, boolean bypassCheck) {
        if (!bypassCheck && getEconomy(guild, user) != null) return Optional.empty();

        var economy = new Economy(guild.getIdLong(), user.getIdLong());

        // TOOD: From guild config
        economy.setBank(100);
        Database.getDatabase().economy.insertOne(economy);
        return Optional.of(economy);
    }

    public static Optional<Economy> getAccount(Guild guild, User user) {

    }

    public static int addMoney(Guild guild, User user, int amount, boolean bank) {
        Economy economy = getEconomy(guild, user);
        if (economy == null) {
            return -1;
        }

        if (bank) {
            economy.addBank(amount);
            return economy.getBank();
        } else {
            economy.addWallet(amount);
            return economy.getWallet();
        }
    }

    public static int addMoney(Guild guild, User user, int amount) {
        return addMoney(guild, user, amount, false);
    }

    public static int removeMoney(Guild guild, User user, int amount, boolean bank) {
        Economy economy = getEconomy(guild, user);
        if (economy == null) {
            return -1;
        }

        if (bank) {
            economy.removeBank(amount);
            return economy.getBank();
        } else {
            economy.removeWallet(amount);
            return economy.getWallet();
        }
    }

    public static int removeMoney(Guild guild, User user, int amount) {
        return removeMoney(guild, user, amount, false);
    }

    public static int getWallet(Guild guild, User user) {
        return getEconomy(guild, user).getWallet();
    }

    public static int getBank(Guild guild, User user) {
        return getEconomy(guild, user).getBank();
    }

    public static int getBalance(Guild guild, User user) {
        return getEconomy(guild, user).getBalance();
    }

    public static Economy getEconomy(Guild guild, User user) {
        return Database.getDatabase().economy.find(
                Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()))).first();
    }
}
