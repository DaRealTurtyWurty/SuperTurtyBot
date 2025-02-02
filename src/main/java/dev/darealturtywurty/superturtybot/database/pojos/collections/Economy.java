package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.economy.Loan;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class Economy {
    private long guild;
    private long user;

    private long wallet;
    private long bank;

    private long nextRob;
    private long nextWork;
    private long nextCrime;
    private long nextHeist;
    private long nextCrash;

    private long nextDaily;
    private long nextWeekly;
    private long nextMonthly;
    private long nextYearly;

    private long nextLoan;

    private Job job;
    private int jobLevel;
    private boolean readyForPromotion;

    private long totalBetLoss;
    private long totalBetWin;

    private int crimeLevel;
    private int heistLevel;
    private int totalHeists;
    private long totalCrimes;
    private long totalSuccessfulCrimes;
    private long totalCaughtCrimes;

    private List<ShopItem> shopItems = new ArrayList<>();
    private List<Loan> loans = new ArrayList<>();
    private List<Property> properties = new ArrayList<>();

    private List<MoneyTransaction> transactions = new ArrayList<>();

    public Economy() {
        this(0, 0);
    }

    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;
    }

    public void addTransaction(long amount, byte type) {
        addTransaction(System.currentTimeMillis(), amount, type);
    }

    public void addTransaction(long amount, byte type, long targetId) {
        addTransaction(System.currentTimeMillis(), amount, type, targetId);
    }

    public void addTransaction(long timestamp, long amount, byte type) {
        this.transactions.add(new MoneyTransaction(timestamp, amount, type, null));
    }

    public void addTransaction(long timestamp, long amount, byte type, @Nullable Long targetId) {
        this.transactions.add(new MoneyTransaction(timestamp, amount, type, targetId));
    }

    public void addBank(long amount) {
        this.bank += amount;
    }

    public void addWallet(long amount) {
        this.wallet += amount;
    }

    public void removeBank(long amount) {
        this.bank -= amount;
    }

    public void removeWallet(long amount) {
        this.wallet -= amount;
    }

    @Getter
    @AllArgsConstructor
    @ToString
    public enum Job {
        PROGRAMMER(100, 2, 0.45f, 450),
        YOUTUBER(300, 5, 0.1f, 1350),
        MUSICIAN(50, 20, 0.2f, 900),
        ARTIST(50, 20, 0.15f, 675),
        DOCTOR(350, 1.125f, 0.25f, 1350),
        MATHEMATICIAN(150, 1.4f, 0.35f, 225);

        private final long salary;
        private final float promotionMultiplier;
        private final float promotionChance;
        private final int workCooldownSeconds;
    }
}