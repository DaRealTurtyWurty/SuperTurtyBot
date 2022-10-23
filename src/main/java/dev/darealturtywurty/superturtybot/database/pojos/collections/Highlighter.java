package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.UUID;

public class Highlighter {
    private long guild;
    private long user;
    
    private String text;
    private boolean caseSensitive;
    private long timeAdded;
    private String uuid;
    
    public Highlighter() {
        this.guild = 0;
        this.user = 0;
        
        this.text = "";
        this.caseSensitive = false;
        this.timeAdded = 0;
        this.uuid = "";
    }
    
    public Highlighter(long guildId, long userId, String text, boolean caseSensitive) {
        this(guildId, userId, text, caseSensitive, System.currentTimeMillis(), UUID.randomUUID());
    }
    
    public Highlighter(long guildId, long userId, String text, boolean caseSensitive, long timeAdded, UUID uuid) {
        this.guild = guildId;
        this.user = userId;
        
        this.text = text;
        this.caseSensitive = caseSensitive;
        this.timeAdded = timeAdded;
        this.uuid = uuid.toString();
    }
    
    public UUID asUUID() {
        return UUID.fromString(this.uuid);
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public String getText() {
        return this.text;
    }
    
    public long getTimeAdded() {
        return this.timeAdded;
    }
    
    public long getUser() {
        return this.user;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public void setTimeAdded(long timeAdded) {
        this.timeAdded = timeAdded;
    }
    
    public void setUser(long user) {
        this.user = user;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
