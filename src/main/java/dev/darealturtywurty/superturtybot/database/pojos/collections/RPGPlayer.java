package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.stream.LongStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RPGPlayer {
    private long guild;
    private long user;

    private int health;
    private int maxHealth;
    private int xp;
    private int level;
    private int gold;

    private boolean isDead;
    private boolean isExploring;
    private boolean isFighting;
    private boolean isCaving;
    private boolean isDungeoning;

    private long respawnTime;
    private long exploreTime;
    private long fightTime;
    private long caveTime;
    private long dungeonTime;

    private Inventory inventory;

    public RPGPlayer(long guild, long user) {
        this.guild = guild;
        this.user = user;

        this.health = 100;
        this.maxHealth = 100;
        this.xp = 0;
        this.level = 1;
        this.gold = 100;

        this.isDead = false;
        this.isExploring = false;
        this.isFighting = false;
        this.isCaving = false;
        this.isDungeoning = false;

        this.respawnTime = 0;
        this.exploreTime = 0;
        this.fightTime = 0;
        this.caveTime = 0;
        this.dungeonTime = 0;

        this.inventory = new Inventory();
    }

    public boolean isOccupied() {
        return this.isExploring || this.isFighting || this.isCaving || this.isDungeoning;
    }

    public long getOccupiedTime() {
        return max(this.exploreTime, this.fightTime, this.caveTime, this.dungeonTime);
    }

    private static long max(long... longs) {
        return LongStream.of(longs).max().orElse(0);
    }
}
