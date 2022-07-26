package io.github.darealturtywurty.superturtybot.database.pojos.collections;

public class UserConfig {
    private long guild;
    private long user;

    // Might need this in the future? Discord doesn't want to state how it works.
    // private boolean canStoreUserData;
    
    public UserConfig() {
        this(0L, 0L);
    }

    public UserConfig(long guildId, long userId) {
        this.guild = guildId;
        this.user = userId;
    }

    public long getGuild() {
        return this.guild;
    }

    public long getUser() {
        return this.user;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setUser(long user) {
        this.user = user;
    }
}
