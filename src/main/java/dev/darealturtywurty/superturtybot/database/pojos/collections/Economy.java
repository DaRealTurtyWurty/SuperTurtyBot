package dev.darealturtywurty.superturtybot.database.pojos.collections;

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
    
    public Economy() {
        this(0, 0);
    }
    
    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;
        
        this.wallet = 0;
        this.bank = 0;
        
        this.nextRob = 0;
        this.nextWork = 0;
        this.nextCrime = 0;
        this.nextSexWork = 0;
        
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
    
    public long getNextCrime() {
        return this.nextCrime;
    }
    
    public long getNextDaily() {
        return this.nextDaily;
    }
    
    public long getNextMonthly() {
        return this.nextMonthly;
    }
    
    public long getNextRob() {
        return this.nextRob;
    }
    
    public long getNextSexWork() {
        return this.nextSexWork;
    }
    
    public long getNextWeekly() {
        return this.nextWeekly;
    }
    
    public long getNextWork() {
        return this.nextWork;
    }

    public long getNextYearly() {
        return this.nextYearly;
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

    public void setNextCrime(long nextCrime) {
        this.nextCrime = nextCrime;
    }

    public void setNextDaily(long nextDaily) {
        this.nextDaily = nextDaily;
    }

    public void setNextMonthly(long nextMonthly) {
        this.nextMonthly = nextMonthly;
    }

    public void setNextRob(long nextRob) {
        this.nextRob = nextRob;
    }

    public void setNextSexWork(long nextSexWork) {
        this.nextSexWork = nextSexWork;
    }

    public void setNextWeekly(long nextWeekly) {
        this.nextWeekly = nextWeekly;
    }

    public void setNextWork(long nextWork) {
        this.nextWork = nextWork;
    }
    
    public void setNextYearly(long nextYearly) {
        this.nextYearly = nextYearly;
    }
    
    public void setUser(long user) {
        this.user = user;
    }
    
    public void setWallet(int wallet) {
        this.wallet = wallet;
    }
}
