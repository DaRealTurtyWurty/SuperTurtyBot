package io.github.darealturtywurty.superturtybot.database.pojos.collections;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.primitives.Longs;

public class GuildConfig {
    private long guild;
    
    // Channels
    private long modLogging;
    private long suggestions;

    // Showcases
    private long starboard;
    private boolean starboardEnabled;
    private int minimumStars;
    private boolean botStarsCount;
    private String showcaseChannels;
    private boolean starboardMediaOnly;
    private String starEmoji;

    // Levelling
    private String levelRoles;
    private long levelCooldown;
    private int minXP;
    private int maxXP;
    private int levellingItemChance;
    private boolean levellingEnabled;
    private String disabledLevellingChannels;
    private boolean disableLevelUpMessages;
    private boolean hasLevelUpChannel;
    private long levelUpMessageChannel;
    private boolean shouldEmbedLevelUpMessage;
    
    // Misc
    private boolean shouldModeratorsJoinThreads;
    private String autoThreadChannels;
    private boolean shouldCreateGists;
    
    public GuildConfig() {
        this(0L);
    }
    
    public GuildConfig(long guildId) {
        this.guild = guildId;

        // Channels
        this.modLogging = 0L;
        this.suggestions = 0L;

        // Showcases
        this.starboard = 0L;
        this.starboardEnabled = false;
        this.minimumStars = 5;
        this.botStarsCount = true;
        this.showcaseChannels = "";
        this.starboardMediaOnly = true;
        this.starEmoji = "⭐";

        // Levelling
        this.levelRoles = "";
        this.levelCooldown = 25000L;
        this.minXP = 5;
        this.maxXP = 15;
        this.levellingItemChance = 50;
        this.levellingEnabled = true;
        this.disabledLevellingChannels = "";
        this.disableLevelUpMessages = false;
        this.hasLevelUpChannel = false;
        this.levelUpMessageChannel = 0L;
        this.shouldEmbedLevelUpMessage = true;

        // Misc
        this.shouldModeratorsJoinThreads = true;
        this.autoThreadChannels = "";
        this.shouldCreateGists = true;
    }

    public boolean areLevelUpMessagesDisabled() {
        return this.disableLevelUpMessages;
    }

    public String getAutoThreadChannels() {
        return this.autoThreadChannels;
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
    
    public long getLevelUpMessageChannel() {
        return this.levelUpMessageChannel;
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

    public boolean hasLevelUpChannel() {
        return this.hasLevelUpChannel;
    }

    public boolean isBotStarsCount() {
        return this.botStarsCount;
    }

    public boolean isLevellingEnabled() {
        return this.levellingEnabled;
    }

    public boolean shouldCreateGists() {
        return this.shouldCreateGists;
    }

    public boolean isStarboardEnabled() {
        return this.starboardEnabled;
    }
    
    public boolean isStarboardMediaOnly() {
        return this.starboardMediaOnly;
    }
    
    public void setAutoThreadChannels(String autoThreadChannels) {
        this.autoThreadChannels = autoThreadChannels;
    }
    
    public void setBotStarsCount(boolean botStarsCount) {
        this.botStarsCount = botStarsCount;
    }
    
    public void setDisabledLevellingChannels(String disabledLevellingChannels) {
        this.disabledLevellingChannels = disabledLevellingChannels;
    }

    public void setDisableLevelUpMessages(boolean disableLevelUpMessages) {
        this.disableLevelUpMessages = disableLevelUpMessages;
    }

    public void setGuild(long guild) {
        this.guild = guild;
    }
    
    public void setHasLevelUpChannel(boolean hasLevelUpChannel) {
        this.hasLevelUpChannel = hasLevelUpChannel;
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

    public void setLevelUpMessageChannel(long levelUpMessageChannel) {
        this.levelUpMessageChannel = levelUpMessageChannel;
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
    
    public void setShouldCreateGists(boolean shouldCreateGists) {
        this.shouldCreateGists = shouldCreateGists;
    }
    
    public void setShouldEmbedLevelUpMessage(boolean shouldEmbedLevelUpMessage) {
        this.shouldEmbedLevelUpMessage = shouldEmbedLevelUpMessage;
    }

    public void setShouldModeratorsJoinThreads(boolean shouldModeratorsJoinThreads) {
        this.shouldModeratorsJoinThreads = shouldModeratorsJoinThreads;
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
    
    public boolean shouldEmbedLevelUpMessage() {
        return this.shouldEmbedLevelUpMessage;
    }

    public boolean shouldModeratorsJoinThreads() {
        return this.shouldModeratorsJoinThreads;
    }
    
    public static List<Long> getChannels(String str) {
        return Stream.of(str.split("[\s;]")).map(Longs::tryParse).toList();
    }
}
