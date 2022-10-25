package dev.darealturtywurty.superturtybot.database.pojos.collections;

public class Economy {
    private long guild;
    private long user;
    
    private int wallet;
    private int bank;
    private long nextRobTime;
    private long nextWorkTime;
    private long nextDaily;
    private long nextWeekly;
    private long nextMonthly;
    private long nextYearly;
    
    public Economy() {
        this(0, 0);
    }
    
    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;
        
        this.wallet = 0;
        this.bank = 0;
        this.nextRobTime = 0;
        this.nextWorkTime = 0;
        this.nextDaily = 0;
        this.nextWeekly = 0;
        this.nextMonthly = 0;
        this.nextYearly = 0;
    }
    
    public void addBank(int amount) {
        this.bank += amount;
    }
    
    public void addWallet(int amount) {
        this.wallet += amount;
    }
    
    public int getBalance() {
        return this.wallet + this.bank;
    }
    
    public int getBank() {
        return this.bank;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public long getNextRobTime() {
        return this.nextRobTime;
    }
    
    public long getUser() {
        return this.user;
    }
    
    public int getWallet() {
        return this.wallet;
    }
    
    public void removeBank(int amount) {
        this.bank -= amount;
    }
    
    public void removeWallet(int amount) {
        this.wallet -= amount;
    }
    
    public void setBank(int bank) {
        this.bank = bank;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setNextRobTime(long nextRobTime) {
        this.nextRobTime = nextRobTime;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public void setWallet(int wallet) {
        this.wallet = wallet;
    }

    public void setNextWorkTime(long nextWorkTime) {
        this.nextWorkTime = nextWorkTime;
    }

    public long getNextWorkTime() {
        return this.nextWorkTime;
    }

    public long getNextDaily() {
        return this.nextDaily;
    }

    public void setNextDaily(long nextDaily) {
        this.nextDaily = nextDaily;
    }

    public long getNextWeekly() {
        return this.nextWeekly;
    }

    public void setNextWeekly(long nextWeekly) {
        this.nextWeekly = nextWeekly;
    }

    public long getNextMonthly() {
        return this.nextMonthly;
    }

    public void setNextMonthly(long nextMonthly) {
        this.nextMonthly = nextMonthly;
    }

    public long getNextYearly() {
        return this.nextYearly;
    }

    public void setNextYearly(long nextYearly) {
        this.nextYearly = nextYearly;
    }
}
