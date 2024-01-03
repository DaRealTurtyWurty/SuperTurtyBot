package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

import dev.darealturtywurty.superturtybot.modules.counting.CountingMode;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Counting {
    private long guild;
    private long channel;
    private String countingMode;
    
    private List<UserData> users;
    private int currentCount;
    private int highestCount;
    private float nextNumber;
    private float currentNumber;
    private float saves;
    private long latestCounter;
    private long lastCountingMessageMillis;

    private String additionalData;
    
    public Counting(long guildId, long channelId, CountingMode mode) {
        this.guild = guildId;
        this.channel = channelId;
        this.countingMode = mode.name();
        
        this.users = new ArrayList<>();
        this.currentCount = 0;
        this.highestCount = 0;
        this.nextNumber = CountingMode.getStartingNumber(mode);
        this.currentNumber = 0;
        this.saves = 0;
        this.latestCounter = 0;
        this.lastCountingMessageMillis = 0;

        this.additionalData = "";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserData {
        private long user;
        
        private int currentCountSuccession;
        private int totalCounts;
        
        public UserData(long userId) {
            this.user = userId;
            
            this.currentCountSuccession = 0;
            this.totalCounts = 0;
        }
    }
}
