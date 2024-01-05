package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MongoColor {
    private float red;
    private float green;
    private float blue;
    private float alpha;
    
    public MongoColor(float red, float green, float blue) {
        this(red, green, blue, 1.0f);
    }
    
    public MongoColor(int red, int green, int blue) {
        this(red / 255f, green / 255f, blue / 255f, 1.0f);
    }
    
    public Color asColor() {
        return new Color(this.red, this.green, this.blue, this.alpha);
    }
}