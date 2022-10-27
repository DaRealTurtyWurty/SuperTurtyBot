package dev.darealturtywurty.superturtybot.database.pojos.collections;

import lombok.Data;

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
    
    public Economy() {
        this(0, 0);
    }
    
    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;
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
        PROGRAMMER, YOUTUBER, MUSICIAN, ARTIST, DOCTOR, MATHEMATICIAN;
    }
}
