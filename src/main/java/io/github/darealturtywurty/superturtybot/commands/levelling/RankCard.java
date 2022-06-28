package io.github.darealturtywurty.superturtybot.commands.levelling;

import java.awt.Color;

import net.dv8tion.jda.api.entities.Member;

public class RankCard {
    public static final RankCard DEFAULT = new RankCard(null);
    
    public Member member;
    public Color backgroundColour = Color.decode("#2E4347"), outlineColour = Color.decode("#1C2130"),
        rankTextColour = Color.decode("#FF3D7F"), levelTextColour = Color.decode("#F5DF98"),
        xpOutlineColour = Color.decode("#A3B808"), xpEmptyColour = Color.decode("#519548"),
        xpFillColour = Color.decode("#88C425"), avatarOutlineColour = Color.BLACK,
        percentTextColour = Color.decode("#BEF202"), xpTextColour = Color.decode("#FFB884"),
        nameTextColour = Color.decode("#C0D1C2");
    
    public float outlineOpacity = 1f;
    
    // Premium
    public String backgroundImage = "", outlineImage = "", xpOutlineImage = "", xpEmptyImage = "", xpFillImage = "",
        avatarOutlineImage = "";
    
    public RankCard(final Member member) {
        this.member = member;
    }
}
