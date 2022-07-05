package io.github.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

import io.github.darealturtywurty.superturtybot.database.pojos.RankCard;

public class Levelling {
    private long guild;
    private long user;
    
    private int level;
    private int xp;
    
    private RankCard rankCard;
    private List<String> inventory;
    
    public Levelling() {
    }

    public Levelling(long guildId, long userId) {
        this.guild = guildId;
        this.user = userId;
        this.level = 0;
        this.xp = 0;
        this.rankCard = new RankCard();
        this.inventory = new ArrayList<>();
    }

    public long getGuild() {
        return this.guild;
    }

    public List<String> getInventory() {
        return this.inventory;
    }

    public int getLevel() {
        return this.level;
    }

    public RankCard getRankCard() {
        return this.rankCard;
    }

    public long getUser() {
        return this.user;
    }

    public int getXp() {
        return this.xp;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }

    public void setInventory(List<String> inventory) {
        this.inventory = inventory;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setRankCard(RankCard rankCard) {
        this.rankCard = rankCard;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}
