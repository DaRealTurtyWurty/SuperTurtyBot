package io.github.darealturtywurty.superturtybot.modules.idlerpg.entities;

import org.apache.commons.text.WordUtils;

import io.github.darealturtywurty.superturtybot.registry.Registerable;

public class Entity implements Registerable {
    private String name;
    private String richName = null;
    private int health;
    private final int maxHealth;
    
    public Entity(Builder builder) {
        this.richName = builder.richName;
        
        this.maxHealth = builder.health;
        this.health = this.maxHealth;
    }
    
    public int getHealth() {
        return this.health;
    }
    
    public int getMaxHealth() {
        return this.maxHealth;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getRichName() {
        return this.richName;
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        if (this.richName == null) {
            this.richName = WordUtils.capitalize(name.replace("_", " ").toLowerCase().trim());
        }

        return this;
    }

    public static class Builder {
        private String richName;
        private int health;

        public Builder(int health) {
            if (health <= 0)
                throw new IllegalArgumentException("An entity's health must be above 0!");

            this.health = health;
        }

        public Builder richName(String name) {
            this.richName = name;
            return this;
        }
    }
}
