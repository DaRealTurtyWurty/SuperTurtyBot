package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.text.WordUtils;

import java.util.ArrayList;
import java.util.UUID;
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

    public static Economy createAccount(Guild guild, User user) {
        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        final var economy = new Economy(guild.getIdLong(), user.getIdLong());
        economy.setBank(config.getDefaultEconomyBalance());
        Database.getDatabase().economy.insertOne(economy);
        return economy;
    }

    public static Economy getAccount(Guild guild, User user) {
        Economy account = Database.getDatabase().economy.find(
                Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", user.getIdLong())
                )).first();

        if (account == null) {
            account = createAccount(guild, user);
            Database.getDatabase().economy.insertOne(account);
        }

        return account;
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

    public static void setMoney(Economy account, int amount) {
        setMoney(account, amount, false);
    }

    public static void setMoney(Economy account, int amount, boolean bank) {
        if (bank) {
            account.setBank(amount);
        } else {
            account.setWallet(amount);
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

        if(!PublicShop.isRunning()) {
            PublicShop.run();
        }

        // TODO: Move to DailyTask
        EXECUTOR.scheduleAtFixedRate(() -> {
            Database.getDatabase().economy.find().into(new ArrayList<>()).stream()
                    .filter(account -> getBalance(account) < 0).forEach(account -> {
                        Guild guild = jda.getGuildById(account.getGuild());
                        if (guild == null)
                            return;

                        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
                        if (config == null) {
                            config = new GuildConfig(guild.getIdLong());
                            Database.getDatabase().guildConfig.insertOne(config);
                        }

                        removeMoney(account, config.getDefaultEconomyBalance(), true);
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
                Filters.and(
                        Filters.eq("guild", account.getGuild()),
                        Filters.eq("user", account.getUser())),
                account);
    }

    public static boolean hasJob(Economy account) {
        return account.getJob() != null;
    }

    public static boolean canWork(Economy account) {
        return hasJob(account) && account.getNextWork() < System.currentTimeMillis();
    }

    public static int work(Economy account) {
        if (!canWork(account)) return 0;

        int salary = account.getJob().getSalary();
        int jobLevel = account.getJobLevel();
        float multiplier = account.getJob().getPromotionMultiplier() + 1;
        int amount = Math.round(salary * (1 + (jobLevel * multiplier)));
        int earned = Math.max(10, amount);

        account.setNextWork(System.currentTimeMillis() + (account.getJob().getWorkCooldownSeconds() * 1000L));
        addMoney(account, earned, true);

        if(ThreadLocalRandom.current().nextInt(100) < (int) (account.getJob().getPromotionChance() * 100)) {
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
        builder.addField("Next Work", TimeFormat.RELATIVE.format(account.getNextWork()), false);

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

    public static void betWin(Economy account, int amount) {
        account.setTotalBetWin(account.getTotalBetWin() + amount);
    }

    public static void betLoss(Economy account, int amount) {
        account.setTotalBetLoss(account.getTotalBetLoss() + amount);
    }

    public static PublicShop getPublicShop() {
        return PublicShop.INSTANCE;
    }

    public static void setNextWork(Economy account, long time) {
        account.setNextWork(time);
    }

    public static float getCreditScore(Economy account) {
        long totalBet = account.getTotalBetWin() - account.getTotalBetLoss();
        float score = totalBet / 1000F;
        if(getBalance(account) < 0) {
            score *= 0.5f;
        }

        return score;
    }

    public static Loan addLoan(Economy account, int amount) {
        var loan = new Loan(
                UUID.randomUUID().toString(),
                amount,
                getInterestRate(amount),
                System.currentTimeMillis(),
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(getTimeToPayOff(amount)));
        account.getLoans().add(loan);
        account.setNextLoan(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

        addMoney(account, amount, true);
        updateAccount(account);
        return loan;
    }

    public static boolean payLoan(Economy account, Loan loan, int amount) {
        int returned = loan.pay(amount);
        removeMoney(account, amount - returned, true);
        System.out.println("Amount: " + amount + ", Returned: " + returned);
        updateAccount(account);

        return loan.isPaidOff();
    }

    public static int getTimeToPayOff(int amount) {
        return amount / 1000;
    }

    public static float getInterestRate(int amount) {
        if (amount < 5000) return 0.5f;

        return 0.5f + (0.5f * (amount / 5000f));
    }
}