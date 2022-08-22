package io.github.darealturtywurty.superturtybot.modules.idlerpg.entities;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.killsource.KillSource;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.killsource.KillSourceRegistry;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.DamageRange;

public class Enemy extends Entity {
    private final DamageRange damageRange;
    private final KillSource killSource;
    private final int regenerationSpeed;
    private final int intelligence;
    
    public Enemy(Builder builder) {
        super(builder.entityBuilder);
        this.damageRange = builder.damageRange;
        this.killSource = builder.killSource;
        this.regenerationSpeed = builder.regenerationSpeed;
        this.intelligence = builder.intelligence;
    }

    public void attack(RPGPlayer player) {
        player.damage(getDamageRange().getDamage());
    }
    
    public DamageRange getDamageRange() {
        return this.damageRange;
    }

    public static class Builder {
        private final DamageRange damageRange;
        private KillSource killSource = KillSourceRegistry.DEFAULT_ENEMY;
        private int regenerationSpeed;
        private int intelligence;
        private Entity.Builder entityBuilder;
        
        public Builder(Entity.Builder entity, DamageRange dmgRange) {
            if (entity == null)
                throw new NullPointerException("You must provide an entity builder to any entity!");

            this.entityBuilder = entity;

            if (dmgRange == null)
                throw new NullPointerException("An entity's damageRange must not be null!");
            
            this.damageRange = dmgRange;
        }

        public Builder(int health, DamageRange dmgRange) {
            this(new Entity.Builder(health), dmgRange);
        }

        public Builder(String richName, int health, DamageRange dmgRange) {
            this(new Entity.Builder(health).richName(richName), dmgRange);
        }
        
        public Builder intelligence(int intelligence) {
            this.intelligence = intelligence;
            return this;
        }

        public Builder killSource(KillSource killSource) {
            if (killSource == null)
                throw new NullPointerException("An entity's killSource must not be null!");

            this.killSource = killSource;
            return this;
        }

        public Builder regeneration(int regenSpeed) {
            this.regenerationSpeed = regenSpeed;
            return this;
        }
    }
}
