package dev.darealturtywurty.superturtybot.database.pojos.collections;

public class Economy {
    private long guild;
    private long user;

    private int wallet;
    private int bank;

    public Economy() {
        this(0, 0);
    }

    public Economy(long guild, long user) {
        this.guild = guild;
        this.user = user;

        this.wallet = 0;
        this.bank = 0;
    }

    public long getGuild() {
        return this.guild;
    }

    public long getUser() {
        return this.user;
    }

    public int getWallet() {
        return this.wallet;
    }

    public int getBank() {
        return this.bank;
    }

    public void setWallet(int wallet) {
        this.wallet = wallet;
    }

    public void setBank(int bank) {
        this.bank = bank;
    }

    public void addWallet(int amount) {
        this.wallet += amount;
    }

    public void addBank(int amount) {
        this.bank += amount;
    }

    public void removeWallet(int amount) {
        this.wallet -= amount;
    }

    public void removeBank(int amount) {
        this.bank -= amount;
    }

    public int getBalance() {
        return this.wallet + this.bank;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }

    public void setUser(long user) {
        this.user = user;
    }
}
