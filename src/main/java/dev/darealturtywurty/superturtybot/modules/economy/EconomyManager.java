package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EconomyManager {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static int addMoney(Guild guild, User user, int amount) {
        return addMoney(guild, user, amount, false);
    }

    public static int addMoney(Guild guild, User user, int amount, boolean bank) {
        final Economy economy = fetchAccount(guild, user);
        if (economy == null) return -1;

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
        if (!bypassCheck && getAccount(guild, user).isEmpty()) return Optional.empty();

        final var economy = new Economy(guild.getIdLong(), user.getIdLong());

        // TOOD: From guild config
        economy.setBank(100);
        Database.getDatabase().economy.insertOne(economy);
        return Optional.of(economy);
    }

    public static Economy fetchAccount(Guild guild, User user) {
        final Optional<Economy> account = getAccount(guild, user);
        return account.orElseGet(() -> createAccount(guild, user, true).get());
    }

    public static Optional<Economy> getAccount(Guild guild, User user) {
        return Optional.ofNullable(Database.getDatabase().economy.find(
                Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()))).first());
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
        if (economy == null) return -1;

        if (bank) {
            economy.removeBank(amount);
            return economy.getBank();
        }

        economy.removeWallet(amount);
        return economy.getWallet();
    }

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void start(JDA jda) {
        if(isRunning())
            return;

        IS_RUNNING.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            Database.getDatabase().economy.find().into(new ArrayList<>()).stream()
                                          .filter(economy -> economy.getBalance() < 0).forEach(account -> {
                        //TODO: Get from guild config
                        account.removeBank(200);
                        jda.getUserById(account.getUser()).openPrivateChannel().queue(channel -> {
                            channel.sendMessage(
                                    ("You have a negative balance in your bank! As such, you have been fined <>%d! " + "Your outstanding balance is %d.").replace(
                                            "<>", "$").formatted(200, -account.getBank())).queue();
                        }, error -> {
                        });
                    });
        }, 0, 24, TimeUnit.HOURS);
    }

    public static void updateAccount(Economy account) {
        Database.getDatabase().economy.replaceOne(Filters.and(Filters.eq("guild", account.getGuild()),
                Filters.eq("user", account.getUser())), account);
    }
}
