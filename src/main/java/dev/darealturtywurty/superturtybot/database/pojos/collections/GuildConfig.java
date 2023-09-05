package dev.darealturtywurty.superturtybot.database.pojos.collections;

import com.google.common.primitives.Longs;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.forum.update.GenericForumTagUpdateEvent;
import net.dv8tion.jda.api.events.channel.update.GenericChannelUpdateEvent;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.GenericEmojiUpdateEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.guild.update.GenericGuildUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.GenericRoleUpdateEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GenericGuildStickerUpdateEvent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class GuildConfig {
    private long guild;

    // Channels
    private long modLogging;
    private long suggestions;
    private String optInChannels;
    private String nsfwChannels;

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
    private boolean warningsModeratorOnly;
    private long patronRole;
    private boolean shouldSendStartupMessage;

    // Logging
    private long loggingChannel;
    private boolean logChannelCreate;
    private boolean logChannelDelete;
    private boolean logEmojiAdded;
    private boolean logEmojiRemoved;
    private boolean logChannelUpdate;
    private boolean logEmojiUpdate;
    private boolean logForumTagUpdate;
    private boolean logStickerUpdate;
    private boolean logGuildUpdate;
    private boolean logRoleUpdate;
    private boolean logBan;
    private boolean logInviteCreate;
    private boolean logInviteDelete;
    private boolean logMemberJoin;
    private boolean logMemberRemove;
    private boolean logStickerAdded;
    private boolean logStickerRemove;
    private boolean logTimeout;
    private boolean logUnban;
    private boolean logMessageBulkDelete;
    private boolean logMessageDelete;
    private boolean logMessageUpdate;
    private boolean logRoleCreate;
    private boolean logRoleDelete;

    // Counting
    private int maxCountingSuccession;

    public GuildConfig() {
        this(0L);
    }

    public GuildConfig(long guildId) {
        this.guild = guildId;

        // Channels
        this.modLogging = 0L;
        this.suggestions = 0L;
        this.optInChannels = "";
        this.nsfwChannels = "";

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
        this.warningsModeratorOnly = false;
        this.patronRole = 0L;
        this.shouldSendStartupMessage = true;

        // Logging
        this.loggingChannel = 0L;
        this.logChannelCreate = true;
        this.logChannelDelete = true;
        this.logEmojiAdded = true;
        this.logEmojiRemoved = true;
        this.logChannelUpdate = true;
        this.logEmojiUpdate = true;
        this.logForumTagUpdate = true;
        this.logStickerUpdate = true;
        this.logGuildUpdate = true;
        this.logRoleUpdate = true;
        this.logBan = true;
        this.logInviteCreate = true;
        this.logInviteDelete = true;
        this.logMemberJoin = true;
        this.logMemberRemove = true;
        this.logStickerAdded = true;
        this.logStickerRemove = true;
        this.logTimeout = true;
        this.logUnban = true;
        this.logMessageBulkDelete = true;
        this.logMessageDelete = true;
        this.logMessageUpdate = true;
        this.logRoleCreate = true;
        this.logRoleDelete = true;

        // Counting
        this.maxCountingSuccession = 3;
    }

    public boolean isDisableLevelUpMessages() {
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

    public long getLoggingChannel() {
        return this.loggingChannel;
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

    public boolean isHasLevelUpChannel() {
        return this.hasLevelUpChannel;
    }

    public boolean isBotStarsCount() {
        return this.botStarsCount;
    }

    public boolean isLevellingEnabled() {
        return this.levellingEnabled;
    }

    public boolean isWarningsModeratorOnly() {
        return warningsModeratorOnly;
    }

    public void setWarningsModeratorOnly(boolean warningsModeratorOnly) {
        this.warningsModeratorOnly = warningsModeratorOnly;
    }

    public boolean isShouldSendStartupMessage() {
        return shouldSendStartupMessage;
    }

    public boolean isLogBan() {
        return this.logBan;
    }

    public boolean isLogChannelCreate() {
        return this.logChannelCreate;
    }

    public boolean isLogChannelDelete() {
        return this.logChannelDelete;
    }

    public boolean isLogChannelUpdate() {
        return this.logChannelUpdate;
    }

    public boolean isLogEmojiAdded() {
        return this.logEmojiAdded;
    }

    public boolean isLogEmojiRemoved() {
        return this.logEmojiRemoved;
    }

    public boolean isLogEmojiUpdate() {
        return this.logEmojiUpdate;
    }

    public boolean isLogForumTagUpdate() {
        return this.logForumTagUpdate;
    }

    public boolean isLogGuildUpdate() {
        return this.logGuildUpdate;
    }

    public boolean isLogInviteCreate() {
        return this.logInviteCreate;
    }

    public boolean isLogInviteDelete() {
        return this.logInviteDelete;
    }

    public boolean isLogMemberJoin() {
        return this.logMemberJoin;
    }

    public boolean isLogMemberRemove() {
        return this.logMemberRemove;
    }

    public boolean isLogMessageBulkDelete() {
        return this.logMessageBulkDelete;
    }

    public boolean isLogMessageDelete() {
        return this.logMessageDelete;
    }

    public boolean isLogMessageUpdate() {
        return this.logMessageUpdate;
    }

    public boolean isLogRoleCreate() {
        return this.logRoleCreate;
    }

    public boolean isLogRoleDelete() {
        return this.logRoleDelete;
    }

    public boolean isLogRoleUpdate() {
        return this.logRoleUpdate;
    }

    public boolean isLogStickerAdded() {
        return this.logStickerAdded;
    }

    public boolean isLogStickerRemove() {
        return this.logStickerRemove;
    }

    public boolean isLogStickerUpdate() {
        return this.logStickerUpdate;
    }

    public boolean isLogTimeout() {
        return this.logTimeout;
    }

    public boolean isLogUnban() {
        return this.logUnban;
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

    public void setShouldSendStartupMessage(boolean shouldSendStartupMessage) {
        this.shouldSendStartupMessage = shouldSendStartupMessage;
    }

    public void setLogBan(boolean logBan) {
        this.logBan = logBan;
    }

    public void setLogChannelCreate(boolean logChannelCreate) {
        this.logChannelCreate = logChannelCreate;
    }

    public void setLogChannelDelete(boolean logChannelDelete) {
        this.logChannelDelete = logChannelDelete;
    }

    public void setLogChannelUpdate(boolean logChannelUpdate) {
        this.logChannelUpdate = logChannelUpdate;
    }

    public void setLogEmojiAdded(boolean logEmojiAdded) {
        this.logEmojiAdded = logEmojiAdded;
    }

    public void setLogEmojiRemoved(boolean logEmojiRemoved) {
        this.logEmojiRemoved = logEmojiRemoved;
    }

    public void setLogEmojiUpdate(boolean logEmojiUpdate) {
        this.logEmojiUpdate = logEmojiUpdate;
    }

    public void setLogForumTagUpdate(boolean logForumTagUpdate) {
        this.logForumTagUpdate = logForumTagUpdate;
    }

    public void setLoggingChannel(long loggingChannel) {
        this.loggingChannel = loggingChannel;
    }

    public void setLogGuildUpdate(boolean logGuildUpdate) {
        this.logGuildUpdate = logGuildUpdate;
    }

    public void setLogInviteCreate(boolean logInviteCreate) {
        this.logInviteCreate = logInviteCreate;
    }

    public void setLogInviteDelete(boolean logInviteDelete) {
        this.logInviteDelete = logInviteDelete;
    }

    public void setLogMemberJoin(boolean logMemberJoin) {
        this.logMemberJoin = logMemberJoin;
    }

    public void setLogMemberRemove(boolean logMemberRemove) {
        this.logMemberRemove = logMemberRemove;
    }

    public void setLogMessageBulkDelete(boolean logMessageBulkDelete) {
        this.logMessageBulkDelete = logMessageBulkDelete;
    }

    public void setLogMessageDelete(boolean logMessageDelete) {
        this.logMessageDelete = logMessageDelete;
    }

    public void setLogMessageUpdate(boolean logMessageUpdate) {
        this.logMessageUpdate = logMessageUpdate;
    }

    public void setLogRoleCreate(boolean logRoleCreate) {
        this.logRoleCreate = logRoleCreate;
    }

    public void setLogRoleDelete(boolean logRoleDelete) {
        this.logRoleDelete = logRoleDelete;
    }

    public void setLogRoleUpdate(boolean logRoleUpdate) {
        this.logRoleUpdate = logRoleUpdate;
    }

    public void setLogStickerAdded(boolean logStickerAdded) {
        this.logStickerAdded = logStickerAdded;
    }

    public void setLogStickerRemove(boolean logStickerRemove) {
        this.logStickerRemove = logStickerRemove;
    }

    public void setLogStickerUpdate(boolean logStickerUpdate) {
        this.logStickerUpdate = logStickerUpdate;
    }

    public void setLogTimeout(boolean logTimeout) {
        this.logTimeout = logTimeout;
    }

    public void setLogUnban(boolean logUnban) {
        this.logUnban = logUnban;
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

    public boolean isShouldCreateGists() {
        return this.shouldCreateGists;
    }

    public boolean isShouldEmbedLevelUpMessage() {
        return this.shouldEmbedLevelUpMessage;
    }

    public String getOptInChannels() {
        return this.optInChannels;
    }

    public void setOptInChannels(String optInChannels) {
        this.optInChannels = optInChannels;
    }

    public boolean isShouldModeratorsJoinThreads() {
        return this.shouldModeratorsJoinThreads;
    }

    public String getNsfwChannels() {
        return this.nsfwChannels;
    }

    public void setNsfwChannels(String nsfwChannels) {
        this.nsfwChannels = nsfwChannels;
    }

    public int getMaxCountingSuccession() {
        return this.maxCountingSuccession;
    }

    public void setMaxCountingSuccession(int maxCountingSuccession) {
        this.maxCountingSuccession = maxCountingSuccession;
    }

    public long getPatronRole() {
        return this.patronRole;
    }

    public void setPatronRole(long patronRole) {
        this.patronRole = patronRole;
    }

    public boolean shouldLog(Event event) {
        if (event instanceof ChannelCreateEvent) return this.logChannelCreate;
        if (event instanceof ChannelDeleteEvent) return this.logChannelDelete;
        if (event instanceof EmojiAddedEvent) return this.logEmojiAdded;
        if (event instanceof EmojiRemovedEvent) return this.logEmojiRemoved;
        if (event instanceof GenericChannelUpdateEvent) return this.logChannelUpdate;
        if (event instanceof GenericEmojiUpdateEvent) return this.logEmojiUpdate;
        if (event instanceof GenericForumTagUpdateEvent) return this.logForumTagUpdate;
        if (event instanceof GenericGuildStickerUpdateEvent) return this.logStickerUpdate;
        if (event instanceof GenericGuildUpdateEvent) return this.logGuildUpdate;
        if (event instanceof GenericRoleUpdateEvent) return this.logRoleUpdate;
        if (event instanceof GuildBanEvent) return this.logBan;
        if (event instanceof GuildInviteCreateEvent) return this.logInviteCreate;
        if (event instanceof GuildInviteDeleteEvent) return this.logInviteDelete;
        if (event instanceof GuildMemberJoinEvent) return this.logMemberJoin;
        if (event instanceof GuildMemberRemoveEvent) return this.logMemberRemove;
        if (event instanceof GuildStickerAddedEvent) return this.logStickerAdded;
        if (event instanceof GuildStickerRemovedEvent) return this.logStickerRemove;
        if (event instanceof GuildMemberUpdateTimeOutEvent) return this.logTimeout;
        if (event instanceof GuildUnbanEvent) return this.logUnban;
        if (event instanceof MessageBulkDeleteEvent) return this.logMessageBulkDelete;
        if (event instanceof MessageDeleteEvent) return this.logMessageDelete;
        if (event instanceof MessageUpdateEvent) return this.logMessageUpdate;
        if (event instanceof RoleCreateEvent) return this.logRoleCreate;
        if (event instanceof RoleDeleteEvent) return this.logRoleDelete;

        return false;
    }

    public static List<Long> getChannels(String str) {
        return Stream.of(str.split("[ ;]")).map(Longs::tryParse).filter(Objects::nonNull).toList();
    }
}
