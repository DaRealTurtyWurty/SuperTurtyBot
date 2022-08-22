package io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo;

import java.awt.Color;

public enum Outcome {
    POSITIVE(Color.GREEN), UNDEFINED(Color.GRAY), UNKNOWN(Color.MAGENTA), NEGATIVE(Color.RED);
    
    private final Color color;
    
    Outcome(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return this.color;
    }
}