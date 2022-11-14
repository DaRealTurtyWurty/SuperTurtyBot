package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.command.JobCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.text.WordUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
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
                        if (user == null) return;

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

        int amount = Math.round(
                account.getJob().getSalary() * (account.getJobLevel() * account.getJob().getPromotionMultiplier()));
        account.setNextWork(System.currentTimeMillis() + (account.getJob().getWorkCooldownSeconds() * 1000L));
        addMoney(account, amount, true);

        if(ThreadLocalRandom.current().nextInt(100) == account.getJob().getPromotionChance() * 100) {
            account.setJobLevel(account.getJobLevel() + 1);
        }

        updateAccount(account);
        return amount;
    }

    public static boolean registerJob(Economy account, String job) {
        if (hasJob(account)) return false;

        Economy.Job found;
        try {
            found = Economy.Job.valueOf(job.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        account.setJob(found);
        account.setJobLevel(0);
        account.setNextWork(System.currentTimeMillis() + (account.getJob().getWorkCooldownSeconds() * 1000L));
        updateAccount(account);

        return true;
    }

    public static void quitJob(Economy account) {
        account.setJob(null);
        account.setJobLevel(0);
        account.setNextWork(System.currentTimeMillis());
        updateAccount(account);
    }

    public static EmbedBuilder getJobProfile(Economy account) {
        final var builder = new EmbedBuilder();
        builder.setTitle("Job Profile");
        builder.addField("Job", WordUtils.capitalize(account.getJob().name().toLowerCase()), false);
        builder.addField("Level", String.valueOf(account.getJobLevel()), false);
        builder.addField("Salary", String.format("$%d", account.getJob().getSalary()), false);
        builder.addField("Promotion Multiplier", String.format("x%.2f", account.getJob().getPromotionMultiplier()),
                false);
        builder.addField("Work Cooldown", String.format("%d seconds", account.getJob().getWorkCooldownSeconds()),
                false);
        builder.addField("Next Work", JobCommand.convertToTimestamp(account.getNextWork()), false);

        return builder;
    }

    public static int workNoJob(Economy account) {
        if(account.getNextWork() > System.currentTimeMillis()) return 0;

        final int amount = ThreadLocalRandom.current().nextInt(1000);
        account.setWallet(EconomyManager.addMoney(account, amount));
        account.setNextWork(System.currentTimeMillis() + 3600000L);
        EconomyManager.updateAccount(account);
        return amount;
    }

    public static PublicShop getPublicShop() {
        return PublicShop.INSTANCE;
    }
}
