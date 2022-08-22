package io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo;

import java.util.concurrent.ThreadLocalRandom;

public final class DamageRange {
    private final int lower, upper;
    private int average;
    
    public DamageRange(int lower, int upper) {
        this.lower = lower;
        this.upper = upper;
        this.average = Math.round((this.lower + this.upper) / 2f);
    }
    
    public DamageRange average(int average) {
        if (average < this.lower) {
            this.average = this.lower;
        } else if (average > this.upper) {
            this.average = this.upper;
        } else {
            this.average = average;
        }
        
        return this;
    }
    
    public int getAverage() {
        return this.average;
    }

    public int getDamage() {
        final int range = getRange();
        final int unclamped = this.average + ThreadLocalRandom.current().nextInt(-range / 2, range / 2);
        return Math.max(this.lower, Math.min(this.upper, unclamped));
    }
    
    public int getLower() {
        return this.lower;
    }

    public int getRange() {
        return this.upper - this.lower;
    }

    public int getUpper() {
        return this.upper;
    }
    
    public static DamageRange absolute(int value) {
        return new DamageRange(value, value).average(value);
    }
}