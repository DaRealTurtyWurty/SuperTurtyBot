package io.github.darealturtywurty.superturtybot.database.pojos.collections;

public class GuildConfig {
    private long guild;

    private long starboard;
    private boolean starboardEnabled;
    private int minimumStars;
    private boolean botStarsCount;
    private long modLogging;
    private String levelRoles;
    private long levelCooldown;
    private int minXP;
    private int maxXP;
    private int levellingItemChance;
    private boolean levellingEnabled;
    private String disabledLevellingChannels;
    private String showcaseChannels;
    private boolean starboardMediaOnly;
    private String starEmoji;
    private long suggestions;

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
        this.levelRoles = "";
        this.levelCooldown = 25000L;
        this.minXP = 5;
        this.maxXP = 15;
        this.levellingItemChance = 50;
        this.levellingEnabled = true;
        this.disabledLevellingChannels = "";
        this.showcaseChannels = "";
        this.starboardMediaOnly = true;
        this.starEmoji = "⭐";
        this.suggestions = 0L;
    }

    public String getDisabledLevellingChannels() {
        return this.disabledLevellingChannels;
    }
    
    public long getGuild() {
        return this.guild;
    }
    
    public long getLevelCooldown() {
        return this.levelCooldown;
    }
    
    public int getLevellingItemChance() {
        return this.levellingItemChance;
    }
    
    public String getLevelRoles() {
        return this.levelRoles;
    }
    
    public int getMaxXP() {
        return this.maxXP;
    }
    
    public int getMinimumStars() {
        return this.minimumStars;
    }
    
    public int getMinXP() {
        return this.minXP;
    }

    public long getModLogging() {
        return this.modLogging;
    }

    public String getShowcaseChannels() {
        return this.showcaseChannels;
    }

    public long getStarboard() {
        return this.starboard;
    }

    public String getStarEmoji() {
        return this.starEmoji;
    }
    
    public long getSuggestions() {
        return this.suggestions;
    }
    
    public boolean isBotStarsCount() {
        return this.botStarsCount;
    }

    public boolean isLevellingEnabled() {
        return this.levellingEnabled;
    }

    public boolean isStarboardEnabled() {
        return this.starboardEnabled;
    }

    public boolean isStarboardMediaOnly() {
        return this.starboardMediaOnly;
    }

    public void setBotStarsCount(boolean botStarsCount) {
        this.botStarsCount = botStarsCount;
    }
    
    public void setDisabledLevellingChannels(String disabledLevellingChannels) {
        this.disabledLevellingChannels = disabledLevellingChannels;
    }
    
    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setLevelCooldown(long levelCooldown) {
        this.levelCooldown = levelCooldown;
    }
    
    public void setLevellingEnabled(boolean levellingEnabled) {
        this.levellingEnabled = levellingEnabled;
    }
    
    public void setLevellingItemChance(int levellingItemChance) {
        this.levellingItemChance = levellingItemChance;
    }
    
    public void setLevelRoles(String levelRoles) {
        this.levelRoles = levelRoles;
    }

    public void setMaxXP(int maxXP) {
        this.maxXP = maxXP;
    }

    public void setMinimumStars(int minimumStars) {
        this.minimumStars = minimumStars;
    }
    
    public void setMinXP(int minXP) {
        this.minXP = minXP;
    }
    
    public void setModLogging(long modLogging) {
        this.modLogging = modLogging;
    }
    
    public void setShowcaseChannels(String showcaseChannels) {
        this.showcaseChannels = showcaseChannels;
    }
    
    public void setStarboard(long starboard) {
        this.starboard = starboard;
    }
    
    public void setStarboardEnabled(boolean starboardEnabled) {
        this.starboardEnabled = starboardEnabled;
    }

    public void setStarboardMediaOnly(boolean starboardMediaOnly) {
        this.starboardMediaOnly = starboardMediaOnly;
    }
    
    public void setStarEmoji(String starEmoji) {
        this.starEmoji = starEmoji;
    }
    
    public void setSuggestions(long suggestions) {
        this.suggestions = suggestions;
    }
}
