package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.economy.CrimeCommand;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EconomyManager {
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static long addMoney(Economy account, long amount) {
        return addMoney(account, amount, false);
    }

    public static long getBalance(Economy account) {
        return account.getWallet() + account.getBank();
    }

    public static long addMoney(Economy account, long amount, boolean bank) {
        if (bank) {
            account.addBank(amount);
            return account.getBank();
        } else {
            account.addWallet(amount);
            return account.getWallet();
        }
    }

    public static Economy createAccount(Guild guild, User user) {
        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(config);
        }

        final var economy = new Economy(guild.getIdLong(), user.getIdLong());
        economy.setBank(config.getDefaultEconomyBalance());
        Database.getDatabase().economy.insertOne(economy);
        return economy;
    }

    public static Economy getOrCreateAccount(Guild guild, User user) {
        Economy account = Database.getDatabase().economy.find(
                Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", user.getIdLong())
                )).first();

        if (account == null) {
            account = createAccount(guild, user);
        }

        return account;
    }

    public static long removeMoney(Economy account, long amount) {
        return removeMoney(account, amount, false);
    }

    public static long removeMoney(Economy account, long amount, boolean bank) {
        if (bank) {
            account.removeBank(amount);
            return account.getBank();
        } else {
            account.removeWallet(amount);
            return account.getWallet();
        }
    }

    public static void setMoney(Economy account, long amount) {
        setMoney(account, amount, false);
    }

    public static void setMoney(Economy account, long amount, boolean bank) {
        if (bank) {
            account.setBank(amount);
        } else {
            account.setWallet(amount);
        }
    }

    public static void withdraw(Economy account, long amount) {
        account.removeBank(amount);
        account.addWallet(amount);
    }

    public static void deposit(Economy account, long amount) {
        account.removeWallet(amount);
        account.addBank(amount);
    }

    public static void start(JDA jda) {
        if (IS_RUNNING.getAndSet(true))
            return;

        if (!PublicShop.isRunning()) {
            PublicShop.run();
        }

        DailyTaskScheduler.addTask(new DailyTask(() -> {
            List<Economy> accounts = Database.getDatabase().economy.find().into(new ArrayList<>());

            Map<Long, List<Economy>> guildsAccounts = accounts.stream()
                    .collect(Collectors.groupingBy(Economy::getGuild));
            for (Map.Entry<Long, List<Economy>> guildAccounts : guildsAccounts.entrySet()) {
                long guildId = guildAccounts.getKey();
                Guild guild = jda.getGuildById(guildId);
                if (guild == null)
                    continue;

                GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
                if (guildData == null || !guildData.isEconomyEnabled() || guildData.getEndOfDayIncomeTax().isEmpty())
                    continue;

                Map<String, Long> endOfDayIncomeTaxes = guildData.getEndOfDayIncomeTax();
                guildAccounts.getValue()
                        .stream()
                        .filter(account -> endOfDayIncomeTaxes.containsKey(String.valueOf(account.getUser())))
                        .forEach(account -> {
                            long amount = endOfDayIncomeTaxes.get(String.valueOf(account.getUser()));
                            if (amount > 0) {
                                User user = jda.getUserById(account.getUser());
                                if (user == null)
                                    return;

                                removeMoney(account, amount, true);
                                updateAccount(account);
                                endOfDayIncomeTaxes.remove(String.valueOf(account.getUser()));

                                UserConfig userConfig = Database.getDatabase().userConfig.find(Filters.eq("user", account.getUser())).first();
                                if (userConfig == null) {
                                    userConfig = new UserConfig(account.getUser());
                                    Database.getDatabase().userConfig.insertOne(userConfig);
                                }

                                UserConfig.TaxMessageType taxMessageType = userConfig.getTaxMessageType();
                                if (taxMessageType != UserConfig.TaxMessageType.OFF) {
                                    user.openPrivateChannel()
                                            .flatMap(channel -> channel.sendMessageFormat(
                                                            "You were taxed %s%d for the end of the day in %s!",
                                                            guildData.getEconomyCurrency(),
                                                            amount,
                                                            guild.getName())
                                                    .setSuppressedNotifications(taxMessageType == UserConfig.TaxMessageType.SILENT))
                                            .queue();
                                }
                            }
                        });

                guildData.setEndOfDayIncomeTax(endOfDayIncomeTaxes);
                Database.getDatabase().guildData.updateOne(Filters.eq("guild", guildId),
                        Updates.set("endOfDayIncomeTax", endOfDayIncomeTaxes));
            }
        }, 12, 0));
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

    public static boolean isOnWorkCooldown(Economy account) {
        return account.getNextWork() >= System.currentTimeMillis();
    }

    public static long work(Economy account) {
        if (isOnWorkCooldown(account)) return 0;

        long amount = getPayAmount(account);
        long earned = Math.max(10, amount);

        if (!Environment.INSTANCE.isDevelopment()) {
            account.setNextWork(System.currentTimeMillis() + (account.getJob().getWorkCooldownSeconds() * 1000L));
        }

        addMoney(account, earned, true);

        if (ThreadLocalRandom.current().nextInt(100) < (int) (account.getJob().getPromotionChance() * 100)) {
            account.setReadyForPromotion(true);
        }

        GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", account.getGuild())).first();
        if (data == null) {
            data = new GuildData(account.getGuild());
            Database.getDatabase().guildData.insertOne(data);
        }

        updateAccount(account);

        Map<String, Long> endOfDayIncome = data.getEndOfDayIncomeTax();
        long newAmount = (long) (endOfDayIncome.getOrDefault(String.valueOf(account.getUser()), 0L) + earned * data.getIncomeTax());
        endOfDayIncome.put(String.valueOf(account.getUser()), newAmount);
        data.setEndOfDayIncomeTax(endOfDayIncome);
        Database.getDatabase().guildData.updateOne(Filters.eq("guild", account.getGuild()),
                Updates.set("endOfDayIncomeTax", endOfDayIncome));

        return amount;
    }

    public static long getPayAmount(Economy account) {
        long salary = account.getJob().getSalary();
        int jobLevel = account.getJobLevel();
        float promotionMultiplier = account.getJob().getPromotionMultiplier();
        return Math.round(salary * (jobLevel + 1) * promotionMultiplier);
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
        builder.addField("Salary", String.format("$%d", Math.max(10, getPayAmount(account))), false);
        builder.addField("Promotion Multiplier", String.format("x%.2f", account.getJob().getPromotionMultiplier()),
                false);
        builder.addField("Work Cooldown", String.format("%d seconds", account.getJob().getWorkCooldownSeconds()),
                false);
        builder.addField("Next Work", TimeFormat.RELATIVE.format(account.getNextWork()), false);

        return builder;
    }

    public static long workNoJob(Economy account) {
        if (account.getNextWork() > System.currentTimeMillis()) return 0;

        final long amount = ThreadLocalRandom.current().nextInt(1000);
        account.setWallet(EconomyManager.addMoney(account, amount));
        if (!Environment.INSTANCE.isDevelopment()) {
            account.setNextWork(System.currentTimeMillis() + 3600000L);
        }
        EconomyManager.updateAccount(account);
        return amount;
    }

    public static void betWin(Economy account, long amount) {
        account.setTotalBetWin(account.getTotalBetWin() + amount);
    }

    public static void betLoss(Economy account, long amount) {
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
        if (getBalance(account) < 0) {
            score *= 0.5f;
        }

        return score;
    }

    public static Loan addLoan(Economy account, long amount) {
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

    public static boolean payLoan(Economy account, Loan loan, long amount) {
        long returned = loan.pay(amount);
        removeMoney(account, amount - returned, true);
        updateAccount(account);

        return loan.isPaidOff();
    }

    public static long getTimeToPayOff(long amount) {
        return amount / 1000;
    }

    public static float getInterestRate(long amount) {
        if (amount < 5000) return 0.5f;

        return 0.5f + (0.5f * (amount / 5000f));
    }

    public static long caughtCrime(Economy account, CrimeCommand.CrimeType level) {
        long amount = level.getAmountForLevel(account.getCrimeLevel()) / 2;
        removeMoney(account, amount, true);

        account.setTotalCrimes(account.getTotalCrimes() + 1);
        account.setTotalCaughtCrimes(account.getTotalCaughtCrimes() + 1);

        if (account.getJob() != null && ThreadLocalRandom.current().nextInt(15) == 0) {
            account.setJobLevel(Math.max(1, account.getJobLevel() - ThreadLocalRandom.current().nextInt(1, 4)));
        }

        return amount;
    }

    public static long successfulCrime(Economy account, CrimeCommand.CrimeType level) {
        int crimeLevel = account.getCrimeLevel();
        long amount = level.getAmountForLevel(crimeLevel);
        addMoney(account, amount, true);

        account.setTotalCrimes(account.getTotalCrimes() + 1);
        account.setTotalSuccessfulCrimes(account.getTotalSuccessfulCrimes() + 1);

        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            account.setCrimeLevel(account.getCrimeLevel() + 1);
        }

        return amount;
    }

    // Should exponentially increase the cost and payout of a heist based on the user's account
    public static long determineHeistSetupCost(Economy account) {
        int heistLevel = account.getHeistLevel();
        return (long) Math.pow(heistLevel, 2) * 100_000L;
    }

    private static long determineHeistPayout(Economy account) {
        int heistLevel = account.getHeistLevel();
        return (long) Math.pow(heistLevel, 2) * ThreadLocalRandom.current().nextLong(500_000, 1_000_000);
    }

    /**
     * Handles the payout and level increase of a heist
     *
     * @param account The user's economy account
     * @return A pair containing the amount earned and whether the user leveled up
     */
    public static Pair<Long, Boolean> heistCompleted(Economy account, long timeTaken) {
        long payout = determineHeistPayout(account);
        long earned = payout / (timeTaken / 10_000) + determineHeistSetupCost(account);
        addMoney(account, earned, true);

        account.setTotalHeists(account.getTotalHeists() + 1);

        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            account.setHeistLevel(account.getHeistLevel() + 1);
            return Pair.of(earned, true);
        }

        return Pair.of(earned, false);
    }
}