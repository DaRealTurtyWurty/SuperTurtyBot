package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RankCard {
    private MongoColor backgroundColor;
    private MongoColor outlineColor;
    private MongoColor rankTextColor;
    private MongoColor levelTextColor;
    private MongoColor xpOutlineColor;
    private MongoColor xpEmptyColor;
    private MongoColor xpFillColor;
    private MongoColor avatarOutlineColor;
    private MongoColor percentTextColor;
    private MongoColor xpTextColor;
    private MongoColor nameTextColor;
    
    private String backgroundImage;
    private String outlineImage;
    private String xpEmptyImage;
    private String xpOutlineImage;
    private String xpFillImage;
    private String avatarOutlineImage;
    
    private float outlineOpacity;
    
    public RankCard() {
        this.backgroundColor = new MongoColor(46, 67, 71);
        this.outlineColor = new MongoColor(28, 33, 48);
        this.rankTextColor = new MongoColor(255, 61, 127);
        this.levelTextColor = new MongoColor(245, 223, 152);
        this.xpOutlineColor = new MongoColor(163, 184, 8);
        this.xpEmptyColor = new MongoColor(81, 149, 72);
        this.xpFillColor = new MongoColor(136, 196, 37);
        this.avatarOutlineColor = new MongoColor(0, 0, 0);
        this.percentTextColor = new MongoColor(190, 242, 2);
        this.xpTextColor = new MongoColor(255, 184, 132);
        this.nameTextColor = new MongoColor(192, 209, 194);
        
        this.backgroundImage = "";
        this.outlineImage = "";
        this.xpEmptyImage = "";
        this.xpOutlineImage = "";
        this.xpFillImage = "";
        this.avatarOutlineImage = "";
        
        this.outlineOpacity = 1.0f;
    }
}