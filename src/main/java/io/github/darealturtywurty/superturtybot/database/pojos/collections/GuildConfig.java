package io.github.darealturtywurty.superturtybot.database.pojos.collections;

public class GuildConfig {
    private long guild;

    private long starboard;
    private boolean starboardEnabled;
    private int minimumStars;
    private boolean botStarsCount;
    private long modLogging;

    public GuildConfig() {
        this(0);
    }

    public GuildConfig(long guildId) {
        this.guild = guildId;
        this.starboard = 0L;
        this.starboardEnabled = false;
        this.minimumStars = 5;
        this.botStarsCount = true;
        this.modLogging = 0L;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public int getMinimumStars() {
        return this.minimumStars;
    }
    
    public long getModLogging() {
        return this.modLogging;
    }
    
    public long getStarboard() {
        return this.starboard;
    }
    
    public boolean isBotStarsCount() {
        return this.botStarsCount;
    }
    
    public boolean isStarboardEnabled() {
        return this.starboardEnabled;
    }
    
    public void setBotStarsCount(boolean botStarsCount) {
        this.botStarsCount = botStarsCount;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setMinimumStars(int minimumStars) {
        this.minimumStars = minimumStars;
    }
    
    public void setModLogging(long modLogging) {
        this.modLogging = modLogging;
    }
    
    public void setStarboard(long starboard) {
        this.starboard = starboard;
    }
    
    public void setStarboardEnabled(boolean starboardEnabled) {
        this.starboardEnabled = starboardEnabled;
    }
}
