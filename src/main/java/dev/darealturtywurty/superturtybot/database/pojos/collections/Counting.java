package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

import dev.darealturtywurty.superturtybot.modules.counting.CountingMode;

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
    
    public Counting() {
    }
    
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
    }
    
    public long getChannel() {
        return this.channel;
    }
    
    public String getCountingMode() {
        return this.countingMode;
    }
    
    public int getCurrentCount() {
        return this.currentCount;
    }
    
    public float getCurrentNumber() {
        return this.currentNumber;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public int getHighestCount() {
        return this.highestCount;
    }
    
    public long getLatestCounter() {
        return this.latestCounter;
    }
    
    public float getNextNumber() {
        return this.nextNumber;
    }
    
    public float getSaves() {
        return this.saves;
    }
    
    public List<UserData> getUsers() {
        return this.users;
    }
    
    public void setChannel(long channel) {
        this.channel = channel;
    }
    
    public void setCountingMode(String countingMode) {
        this.countingMode = countingMode;
    }
    
    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }
    
    public void setCurrentNumber(float currentNumber) {
        this.currentNumber = currentNumber;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setHighestCount(int highestCount) {
        this.highestCount = highestCount;
    }
    
    public void setLatestCounter(long latestCounter) {
        this.latestCounter = latestCounter;
    }
    
    public void setNextNumber(float nextNumber) {
        this.nextNumber = nextNumber;
    }
    
    public void setSaves(float saves) {
        this.saves = saves;
    }
    
    public void setUsers(List<UserData> users) {
        this.users = users;
    }
    
    public static class UserData {
        private long user;
        
        private int currentCountSuccession;
        private int totalCounts;
        
        public UserData() {
        }
        
        public UserData(long userId) {
            this.user = userId;
            
            this.currentCountSuccession = 0;
            this.totalCounts = 0;
        }
        
        public int getCurrentCountSuccession() {
            return this.currentCountSuccession;
        }
        
        public int getTotalCounts() {
            return this.totalCounts;
        }
        
        public long getUser() {
            return this.user;
        }
        
        public void setCurrentCountSuccession(int currentCountSuccession) {
            this.currentCountSuccession = currentCountSuccession;
        }
        
        public void setTotalCounts(int totalCounts) {
            this.totalCounts = totalCounts;
        }
        
        public void setUser(long user) {
            this.user = user;
        }
    }
}
