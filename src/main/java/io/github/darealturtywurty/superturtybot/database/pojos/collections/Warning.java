package io.github.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.UUID;

public class Warning {
    private long guild;
    private long user;
    
    private String reason;
    private long warner;
    private long warnedAt;
    private String uuid;
    
    public Warning() {
        this.guild = 0;
        this.user = 0;
        
        this.reason = "";
        this.warner = 0;
        this.warnedAt = 0;
        this.uuid = "";
    }
    
    public Warning(long guildId, long userId, String reason, long warnerId) {
        this(guildId, userId, reason, warnerId, System.currentTimeMillis(), UUID.randomUUID());
    }
    
    public Warning(long guildId, long userId, String reason, long warnerId, long warnedAt, UUID uuid) {
        this.guild = guildId;
        this.user = userId;
        
        this.reason = reason;
        this.warner = warnerId;
        this.warnedAt = warnedAt;
        this.uuid = uuid.toString();
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public String getReason() {
        return this.reason;
    }
    
    public long getUser() {
        return this.user;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public long getWarnedAt() {
        return this.warnedAt;
    }
    
    public long getWarner() {
        return this.warner;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setUser(long user) {
        this.user = user;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public void setWarnedAt(long warnedAt) {
        this.warnedAt = warnedAt;
    }
    
    public void setWarner(long warner) {
        this.warner = warner;
    }
}
