package io.github.darealturtywurty.superturtybot.modules.idlerpg;

public class RPGStats {
    private long guild;
    private long user;

    private int health;
    private int maxHealth;
    private int xp;
    private int level;
    private int money;
    
    private boolean isExploring;
    private boolean isFighting;
    private boolean isMining;

    public RPGStats() {
        this(0, 0);
    }

    public RPGStats(long guildId, long userId) {
        this.guild = guildId;
        this.user = userId;

        this.maxHealth = 100;
        this.health = this.maxHealth;
        this.xp = 0;
        this.level = 0;
        this.money = 0;
        
        this.isExploring = false;
        this.isFighting = false;
        this.isMining = false;
    }

    public long getGuild() {
        return this.guild;
    }

    public int getHealth() {
        return this.health;
    }

    public int getLevel() {
        return this.level;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }
    
    public int getMoney() {
        return this.money;
    }
    
    public long getUser() {
        return this.user;
    }

    public int getXp() {
        return this.xp;
    }

    public boolean isExploring() {
        return this.isExploring;
    }

    public boolean isFighting() {
        return this.isFighting;
    }

    public boolean isMining() {
        return this.isMining;
    }

    public void setExploring(boolean isExploring) {
        this.isExploring = isExploring;
    }

    public void setFighting(boolean isFighting) {
        this.isFighting = isFighting;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setMining(boolean isMining) {
        this.isMining = isMining;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}
