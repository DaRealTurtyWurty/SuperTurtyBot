package dev.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.ArrayList;
import java.util.List;

public class UserConfig {
    private long guild;
    private long user;

    // Might need this in the future? Discord doesn't want to state how it works.
    // private boolean canStoreUserData;

    private List<Long> optInChannels;
    
    public UserConfig() {
        this(0L, 0L);
    }

    public UserConfig(long guildId, long userId) {
        this.guild = guildId;
        this.user = userId;

        // this.canStoreUserData = true;
        this.optInChannels = new ArrayList<>();
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

    public List<Long> getOptInChannels() {
        return this.optInChannels;
    }

    public void setOptInChannels(List<Long> optInChannels) {
        this.optInChannels = optInChannels;
    }
}
