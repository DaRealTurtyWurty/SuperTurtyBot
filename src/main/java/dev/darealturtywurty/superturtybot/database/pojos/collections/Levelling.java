package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.database.pojos.RankCard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
