package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

import dev.darealturtywurty.superturtybot.database.pojos.RankCard;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Levelling {
    private long guild;
    private long user;
    
    private int level;
    private int xp;
    
    private RankCard rankCard;
    private List<String> inventory;
    
    public Levelling(long guildId, long userId) {
        this.guild = guildId;
        this.user = userId;
        this.level = 0;
        this.xp = 0;
        this.rankCard = new RankCard();
        this.inventory = new ArrayList<>();
    }
}
