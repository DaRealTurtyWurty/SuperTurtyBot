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

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static int addMoney(Economy account, int amount) {
        return addMoney(account, amount, false);
    }

    public static int getBalance(Economy account) {
        return account.getWallet() + account.getBank();
    }

    public static int addMoney(Economy account, int amount, boolean bank) {
        if (bank) {
            account.addBank(amount);
            return account.getBank();
        } else {
            account.addWallet(amount);
            return account.getWallet();
        }
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

    public static int removeMoney(Economy account, int amount) {
        return removeMoney(account, amount, false);
    }

    public static int removeMoney(Economy account, int amount, boolean bank) {
        if (bank) {
            account.removeBank(amount);
            return account.getBank();
        } else {
            account.removeWallet(amount);
            return account.getWallet();
        }
    }

    public static void withdraw(Economy account, int amount) {
        account.removeBank(amount);
        account.addWallet(amount);
    }

    public static void deposit(Economy account, int amount) {
        account.removeWallet(amount);
        account.addBank(amount);
    }

    public static void start(JDA jda) {
        if (isRunning()) return;

        IS_RUNNING.set(true);

        EXECUTOR.scheduleAtFixedRate(() -> {
            Database.getDatabase().economy.find().into(new ArrayList<>()).stream()
                    .filter(account -> getBalance(account) < 0).forEach(account -> {
                        //TODO: Get from guild config
                        removeMoney(account, 200, true);
                        updateAccount(account);

                        User user = jda.getUserById(account.getUser());
                        if (user == null)
                            return;

                        user.openPrivateChannel().queue(channel -> {
                            channel.sendMessage(
                                    ("You have a negative balance in your bank! As such, you have been fined <>%d! " + "Your outstanding balance is %d.").replace(
                                            "<>", "$").formatted(200, -account.getBank())).queue();
                        }, error -> {
                        });
                    });
        }, 0, 24, TimeUnit.HOURS);
    }

    public static void updateAccount(Economy account) {
        Database.getDatabase().economy.replaceOne(
                Filters.and(Filters.eq("guild", account.getGuild()), Filters.eq("user", account.getUser())), account);
    }

    public static boolean hasJob(Economy account) {
        return account.getJob() != null;
    }

    public static boolean canWork(Economy account) {
        return hasJob(account) && account.getNextWork() < System.currentTimeMillis();
    }

    public static int work(Economy account) {
        if (!canWork(account)) return 0;

        final int amount = account.getJob().getSalary();
        addMoney(account, amount);
        account.setNextWork(System.currentTimeMillis() + (account.getJob().getWorkCooldownSeconds() * 1000L));
        updateAccount(account);
        return amount;
    }
}
