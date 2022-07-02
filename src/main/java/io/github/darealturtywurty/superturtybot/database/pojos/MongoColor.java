package io.github.darealturtywurty.superturtybot.database.pojos;

import java.awt.Color;

public class MongoColor {
    private float red;
    private float green;
    private float blue;
    private float alpha;

    public MongoColor() {
    }

    public MongoColor(float red, float green, float blue) {
        this(red, green, blue, 1.0f);
    }
    
    public MongoColor(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public MongoColor(int red, int green, int blue) {
        this(red / 255f, green / 255f, blue / 255f, 1.0f);
    }

    public Color asColor() {
        return new Color(this.red, this.green, this.blue, this.alpha);
    }
    
    public float getAlpha() {
        return this.alpha;
    }
    
    public float getBlue() {
        return this.blue;
    }
    
    public float getGreen() {
        return this.green;
    }
    
    public float getRed() {
        return this.red;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    public void setBlue(float blue) {
        this.blue = blue;
    }
    
    public void setGreen(float green) {
        this.green = green;
    }
    
    public void setRed(float red) {
        this.red = red;
    }
}