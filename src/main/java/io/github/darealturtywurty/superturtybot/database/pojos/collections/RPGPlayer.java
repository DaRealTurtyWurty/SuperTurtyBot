package io.github.darealturtywurty.superturtybot.database.pojos.collections;

public class RPGPlayer {
    private long guild;
    private long user;
    
    private int health;
    private int maxHealth;
    private int xp;
    private int level;
    private int money;

    private boolean exploring;
    private boolean caving;

    private boolean fighting;
    private boolean mining;

    private boolean dead;
    
    public RPGPlayer() {
        this(0, 0);
    }
    
    public RPGPlayer(long guildId, long userId) {
        this.guild = guildId;
        this.user = userId;
        
        this.maxHealth = 100;
        this.health = this.maxHealth;
        this.xp = 0;
        this.level = 0;
        this.money = 0;

        this.exploring = false;
        this.caving = false;

        this.fighting = false;
        this.mining = false;

        this.dead = false;
    }

    public void damage(int amount) {
        setHealth(getHealth() - amount);

        if (getHealth() <= 0) {
            setDead(true);
        }
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

    public boolean isCaving() {
        return this.caving;
    }

    public boolean isDead() {
        return this.dead;
    }
    
    public boolean isExploring() {
        return this.exploring;
    }
    
    public boolean isFighting() {
        return this.fighting;
    }
    
    public boolean isMining() {
        return this.mining;
    }
    
    public void setCaving(boolean caving) {
        this.caving = caving;
    }
    
    public void setDead(boolean dead) {
        this.dead = dead;
    }
    
    public void setExploring(boolean isExploring) {
        this.exploring = isExploring;
    }
    
    public void setFighting(boolean isFighting) {
        this.fighting = isFighting;
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
        this.mining = isMining;
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
