package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Economy {
    private long guild;
    private long user;

    private int wallet;
    private int bank;

    private long nextRob;
    private long nextWork;
    private long nextCrime;
    private long nextSexWork;

    private long nextDaily;
    private long nextWeekly;
    private long nextMonthly;
    private long nextYearly;

    private Job job;
    private int jobLevel;

    private List<ShopItem> shopItems;

    public Economy() {
        this(0, 0);
    }

    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;

        this.shopItems = new ArrayList<>();
    }

    public void addBank(int amount) {
        this.bank += amount;
    }

    public void addWallet(int amount) {
        this.wallet += amount;
    }

    public void removeBank(int amount) {
        this.bank -= amount;
    }

    public void removeWallet(int amount) {
        this.wallet -= amount;
    }

    public enum Job {
        PROGRAMMER(100, 2, 0.075f, 3600), YOUTUBER(300, 5, 0.01f, 10800), MUSICIAN(50, 20, 0.02f, 7200), ARTIST(50, 20,
                0.015f, 5400), DOCTOR(1000, 1.125f, 0.25f, 10800), MATHEMATICIAN(150, 1.4f, 0.75f, 1800);

        private final int salary;
        private final float promotionMultiplier;
        private final int workCooldownSeconds;
        private final float promotionChance;

        Job(int salary, float promotionMultiplier, float promotionChance, int workCooldownSeconds) {
            this.salary = salary;
            this.promotionMultiplier = promotionMultiplier;
            this.promotionChance = promotionChance;
            this.workCooldownSeconds = workCooldownSeconds;
        }

        public int getSalary() {
            return this.salary;
        }

        public float getPromotionMultiplier() {
            return this.promotionMultiplier;
        }

        public float getPromotionChance() {
            return this.promotionChance;
        }

        public int getWorkCooldownSeconds() {
            return this.workCooldownSeconds;
        }
    }
}
