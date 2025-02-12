package dev.darealturtywurty.superturtybot.modules.economy;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.economy.CrimeCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EconomyManager {
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static BigInteger addMoney(Economy account, BigInteger amount) {
        return addMoney(account, amount, false);
    }

    public static BigInteger getBalance(Economy account) {
        return account.getWallet().add(account.getBank());
    }

    public static BigInteger addMoney(Economy account, BigInteger amount, boolean bank) {
        if (bank) {
            account.addBank(amount);
            return account.getBank();
        } else {
            account.addWallet(amount);
            return account.getWallet();
        }
    }

    public static Optional<Economy> getAccount(Guild guild, User user) {
        return Optional.ofNullable(Database.getDatabase().economy.find(
                Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", user.getIdLong())
                )).first());
    }

    public static Economy createAccount(Guild guild, User user) {
        GuildData config = GuildData.getOrCreateGuildData(guild);

        final var economy = new Economy(guild.getIdLong(), user.getIdLong());
        economy.setBank(config.getDefaultEconomyBalance());
        economy.addTransaction(config.getDefaultEconomyBalance(), MoneyTransaction.CREATE_ACCOUNT);
        Database.getDatabase().economy.insertOne(economy);
        return economy;
    }

    public static Economy getOrCreateAccount(Guild guild, User user) {
        return getAccount(guild, user).orElseGet(() -> createAccount(guild, user));
    }

    public static BigInteger removeMoney(Economy account, BigInteger amount) {
        return removeMoney(account, amount, false);
    }

    public static BigInteger removeMoney(Economy account, BigInteger amount, boolean bank) {
        if (bank) {
            account.removeBank(amount);
            return account.getBank();
        } else {
            account.removeWallet(amount);
            return account.getWallet();
        }
    }

    public static void setMoney(Economy account, BigInteger amount) {
        setMoney(account, amount, false);
    }

    public static void setMoney(Economy account, BigInteger amount, boolean bank) {
        if (bank) {
            account.setBank(amount);
        } else {
            account.setWallet(amount);
        }
    }

    public static void withdraw(Economy account, BigInteger amount) {
        account.removeBank(amount);
        account.addWallet(amount);
        account.addTransaction(amount, MoneyTransaction.WITHDRAW);
    }

    public static void deposit(Economy account, BigInteger amount) {
        account.removeWallet(amount);
        account.addBank(amount);
        account.addTransaction(amount, MoneyTransaction.DEPOSIT);
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
                            if (amount <= 0) return;
                            User user = jda.getUserById(account.getUser());
                            if (user == null) return;

                            BigInteger amountBigInteger = BigInteger.valueOf(amount);
                            removeMoney(account, amountBigInteger, true);
                            account.addTransaction(amountBigInteger.negate(), MoneyTransaction.TAX);

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
                                                        "You were taxed %s for the end of the day in %s!",
                                                        StringUtils.numberFormat(amountBigInteger, guildData),
                                                        guild.getName())
                                                .setSuppressedNotifications(taxMessageType == UserConfig.TaxMessageType.SILENT))
                                        .queue();
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

        BigInteger earnedBigInteger = BigInteger.valueOf(earned);
        addMoney(account, earnedBigInteger, true);
        account.addTransaction(earnedBigInteger, MoneyTransaction.JOB);

        if (ThreadLocalRandom.current().nextInt(100) < (int) (account.getJob().getPromotionChance() * 100)) {
            account.setReadyForPromotion(true);
        }

        GuildData data = GuildData.getOrCreateGuildData(account.getGuild());

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
            found = Economy.Job.valueOf(job.toUpperCase(Locale.ROOT));
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
        account.setNextWork(System.currentTimeMillis() + 3600000L);
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
        if (account.getNextWork() > System.currentTimeMillis())
            return 0;

        final long amount = ThreadLocalRandom.current().nextInt(1000);
        BigInteger amountBigInteger = BigInteger.valueOf(amount);
        EconomyManager.addMoney(account, amountBigInteger);
        account.addTransaction(amountBigInteger, MoneyTransaction.WORK);
        if (!Environment.INSTANCE.isDevelopment()) {
            account.setNextWork(System.currentTimeMillis() + 3600000L);
        }

        EconomyManager.updateAccount(account);
        return amount;
    }

    public static void betWin(Economy account, BigInteger amount) {
        account.setTotalBetWin(account.getTotalBetWin().add(amount));
    }

    public static void betLoss(Economy account, BigInteger amount) {
        account.setTotalBetLoss(account.getTotalBetLoss().add(amount));
    }

    public static PublicShop getPublicShop() {
        return PublicShop.INSTANCE;
    }

    public static void setNextWork(Economy account, long time) {
        account.setNextWork(time);
    }

    public static Loan addLoan(Economy account, BigInteger amount) {
        var loan = new Loan(
                UUID.randomUUID().toString(),
                amount,
                getInterestRate(amount),
                System.currentTimeMillis(),
                System.currentTimeMillis() + getTimeToPayOff(amount));
        account.getLoans().add(loan);
        account.setNextLoan(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

        addMoney(account, amount, true);
        account.addTransaction(amount, MoneyTransaction.LOAN);
        updateAccount(account);
        return loan;
    }

    public static void payLoan(Economy account, Loan loan, BigInteger amount) {
        BigInteger returned = loan.pay(amount);
        BigInteger paid = amount.subtract(returned);
        removeMoney(account, paid, true);
        account.addTransaction(paid.negate(), MoneyTransaction.PAY_LOAN);

        updateAccount(account);
    }

    public static long getTimeToPayOff(BigInteger amount) {
        long maxMillis = TimeUnit.DAYS.toMillis(7);
        long minMillis = TimeUnit.HOURS.toMillis(12);
        long millisPerCurrency = TimeUnit.DAYS.toMillis(1) / 100_000;
        BigInteger millisFromAmount = amount.multiply(BigInteger.valueOf(millisPerCurrency));
        long upperLimit = millisFromAmount.min(BigInteger.valueOf(maxMillis)).longValueExact();
        return Math.min(minMillis, upperLimit);
    }

    public static BigDecimal getInterestRate(BigInteger amount) {
        BigDecimal half = new BigDecimal("0.5");
        if (amount.compareTo(BigInteger.valueOf(5000)) < 0) return half;

        return new BigDecimal(amount).scaleByPowerOfTen(-4).add(half);
    }

    public static BigInteger caughtCrime(Economy account, CrimeCommand.CrimeType level) {
        BigInteger amount = level.getRandomAmountForLevel(account.getCrimeLevel()).divide(BigInteger.TWO);
        removeMoney(account, amount, true);
        account.addTransaction(amount.negate(), MoneyTransaction.CRIME);

        account.setTotalCrimes(account.getTotalCrimes() + 1);
        account.setTotalCaughtCrimes(account.getTotalCaughtCrimes() + 1);

        if (account.getJob() != null && ThreadLocalRandom.current().nextInt(15) == 0) {
            account.setJobLevel(Math.max(1, account.getJobLevel() - ThreadLocalRandom.current().nextInt(1, 4)));
        }

        return amount;
    }

    public static BigInteger successfulCrime(Economy account, CrimeCommand.CrimeType level) {
        int crimeLevel = account.getCrimeLevel();
        BigInteger amount = level.getRandomAmountForLevel(crimeLevel);
        addMoney(account, amount, true);
        account.addTransaction(amount, MoneyTransaction.CRIME);

        account.setTotalCrimes(account.getTotalCrimes() + 1);
        account.setTotalSuccessfulCrimes(account.getTotalSuccessfulCrimes() + 1);

        if (ThreadLocalRandom.current().nextInt(3) == 0) {
            account.setCrimeLevel(account.getCrimeLevel() + 1);
        }

        return amount;
    }

    // Should exponentially increase the cost and payout of a heist based on the user's account
    public static long determineHeistSetupCost(Economy account) {
        int heistLevel = account.getHeistLevel() + 1;
        return 100_000L * heistLevel * heistLevel;
    }

    private static long determineHeistPayout(Economy account) {
        int heistLevel = account.getHeistLevel() + 1;
        return ThreadLocalRandom.current().nextLong(200_000, 500_000) * heistLevel * heistLevel;
    }

    /**
     * Handles the payout and level increase of a heist
     *
     * @param account The user's economy account
     * @return A pair containing the amount earned and whether the user leveled up
     */
    public static Pair<Long, Boolean> heistCompleted(Economy account, long timeTaken) {
        long payout = determineHeistPayout(account);
        long earned = payout * 10_000L / timeTaken + determineHeistSetupCost(account);
        BigInteger earnedBigInteger = BigInteger.valueOf(earned);
        addMoney(account, earnedBigInteger, true);
        account.addTransaction(earnedBigInteger, MoneyTransaction.HEIST);

        account.setTotalHeists(account.getTotalHeists() + 1);

        if (ThreadLocalRandom.current().nextInt(4) == 0) {
            account.setHeistLevel(account.getHeistLevel() + 1);
            return Pair.of(earned, true);
        }

        return Pair.of(earned, false);
    }
}