package dev.darealturtywurty.superturtybot.database.pojos.collections;

public class Tag {
    private long guild;
    private long user;
    
    private String name;
    private String data;
    
    public Tag() {
        this.guild = 0;
        this.user = 0;
        
        this.name = "";
        this.data = "";
    }
    
    public Tag(long guildId, long userId, String name, String data) {
        this.guild = guildId;
        this.user = userId;
        
        this.name = name;
        this.data = data;
    }
    
    public String getData() {
        return this.data;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public String getName() {
        return this.name;
    }
    
    public long getUser() {
        return this.user;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setUser(long user) {
        this.user = user;
    }
}
