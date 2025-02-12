package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.*;

@Data
@AllArgsConstructor
public class RankCard {
    private Color backgroundColor;
    private Color outlineColor;
    private Color rankTextColor;
    private Color levelTextColor;
    private Color xpOutlineColor;
    private Color xpEmptyColor;
    private Color xpFillColor;
    private Color avatarOutlineColor;
    private Color percentTextColor;
    private Color xpTextColor;
    private Color nameTextColor;
    
    private String backgroundImage;
    private String outlineImage;
    private String xpEmptyImage;
    private String xpOutlineImage;
    private String xpFillImage;
    private String avatarOutlineImage;
    
    private float outlineOpacity;
    
    public RankCard() {
        this.backgroundColor = new Color(46, 67, 71);
        this.outlineColor = new Color(28, 33, 48);
        this.rankTextColor = new Color(255, 61, 127);
        this.levelTextColor = new Color(245, 223, 152);
        this.xpOutlineColor = new Color(163, 184, 8);
        this.xpEmptyColor = new Color(81, 149, 72);
        this.xpFillColor = new Color(136, 196, 37);
        this.avatarOutlineColor = new Color(0, 0, 0);
        this.percentTextColor = new Color(190, 242, 2);
        this.xpTextColor = new Color(255, 184, 132);
        this.nameTextColor = new Color(192, 209, 194);
        
        this.backgroundImage = "";
        this.outlineImage = "";
        this.xpEmptyImage = "";
        this.xpOutlineImage = "";
        this.xpFillImage = "";
        this.avatarOutlineImage = "";
        
        this.outlineOpacity = 1.0f;
    }
}