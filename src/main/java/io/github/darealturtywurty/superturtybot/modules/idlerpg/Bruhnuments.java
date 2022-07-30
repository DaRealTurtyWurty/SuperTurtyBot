package io.github.darealturtywurty.superturtybot.modules.idlerpg;

import java.awt.Color;

public enum Bruhnuments {
    POSITIVE(Color.GREEN), UNDEFINED(Color.GRAY), UNKNOWN(Color.MAGENTA), NEGATIVE(Color.RED);
    
    private final Color color;
    
    Bruhnuments(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return this.color;
    }
}