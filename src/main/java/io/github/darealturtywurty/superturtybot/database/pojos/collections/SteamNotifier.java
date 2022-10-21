package io.github.darealturtywurty.superturtybot.database.pojos.collections;

import io.github.darealturtywurty.superturtybot.database.pojos.SteamAppNews;

public class SteamNotifier {
    private long guild;
    private long channel;
    private int appId;
    
    private String mention;

    private SteamAppNews previousData;

    public SteamNotifier() {
        this(0L, 0L, 0, "");
    }

    public SteamNotifier(long guild, long channel, int appId, String mention) {
        this.guild = guild;
        this.channel = channel;
        this.appId = appId;
        
        this.mention = mention;
        this.previousData = null;
    }

    public int getAppId() {
        return this.appId;
    }

    public long getChannel() {
        return this.channel;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public String getMention() {
        return this.mention;
    }
    
    public SteamAppNews getPreviousData() {
        return this.previousData;
    }
    
    public void setAppId(int appId) {
        this.appId = appId;
    }
    
    public void setChannel(long channel) {
        this.channel = channel;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setMention(String mention) {
        this.mention = mention;
    }
    
    public void setPreviousData(SteamAppNews previousData) {
        this.previousData = previousData;
    }
}
