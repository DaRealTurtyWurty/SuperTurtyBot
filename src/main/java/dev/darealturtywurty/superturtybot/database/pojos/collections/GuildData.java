package dev.darealturtywurty.superturtybot.database.pojos.collections;

import com.google.common.primitives.Longs;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.core.config.CommandPermission;
import dev.darealturtywurty.superturtybot.database.Database;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
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

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@Data
public class GuildData {
    private long guild;

    // Channels
    private long modLogging;
    private long suggestions;
    private String optInChannels;
    private String nsfwChannels;
    private long birthdayChannel;
    private long welcomeChannel;
    private long collectorChannel;
    private String discordInviteWhitelistChannels;

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
    private boolean shouldDepleteLevels;
    private String xpBoostedChannels;
    private String xpBoostedRoles;
    private int xpBoostPercentage;
    private boolean doServerBoostsAffectXP;

    // Economy
    private String economyCurrency;
    private boolean economyEnabled;
    private BigInteger defaultEconomyBalance;
    private float incomeTax;
    private Map<String, Long> endOfDayIncomeTax;

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

    // Music
    private boolean canAddPlaylists;
    private List<CommandPermission> musicPermissions;
    private int maxSongsPerUser;

    // Chat Revival
    private boolean chatRevivalEnabled;
    private int chatRevivalTime;
    private long chatRevivalChannel;

    // AI
    private boolean aiEnabled;
    private String aiChannelWhitelist;
    private String aiUserBlacklist;

    // Misc
    private boolean shouldModeratorsJoinThreads;
    private String autoThreadChannels;
    private boolean shouldCreateGists;
    private boolean warningsModeratorOnly;
    private long patronRole;
    private boolean shouldSendStartupMessage;
    private boolean shouldSendChangelog;
    private boolean artistNsfwFilterEnabled;
    private float warningXpPercentage;
    private float warningEconomyPercentage;
    private boolean announceBirthdays;
    private List<Long> enabledBirthdayUsers;
    private boolean isCollectingEnabled;
    private boolean shouldAnnounceJoins;
    private boolean shouldAnnounceLeaves;
    private boolean imageSpamAutoBanEnabled;
    private int imageSpamWindowSeconds;
    private int imageSpamMinImages;
    private int imageSpamNewMemberThresholdHours;
    private boolean discordInviteGuardEnabled;
    private boolean scamDetectionEnabled;

    public GuildData() {
        this(0L);
    }

    public GuildData(long guildId) {
        this.guild = guildId;

        // Channels
        this.modLogging = 0L;
        this.suggestions = 0L;
        this.optInChannels = "";
        this.nsfwChannels = "";
        this.birthdayChannel = 0L;
        this.welcomeChannel = 0L;
        this.collectorChannel = 0L;
        this.discordInviteWhitelistChannels = "";

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
        this.shouldDepleteLevels = false;
        this.xpBoostedChannels = "";
        this.xpBoostedRoles = "";
        this.xpBoostPercentage = 15;
        this.doServerBoostsAffectXP = true;

        // Economy
        this.economyCurrency = "$";
        this.economyEnabled = true;
        this.defaultEconomyBalance = BigInteger.valueOf(200);
        this.incomeTax = 0.1F;
        this.endOfDayIncomeTax = new HashMap<>();

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

        // Music
        this.canAddPlaylists = true;
        this.musicPermissions = new ArrayList<>();

        // AI
        this.aiEnabled = false;
        this.aiChannelWhitelist = "";
        this.aiUserBlacklist = "";

        // Misc
        this.shouldModeratorsJoinThreads = true;
        this.autoThreadChannels = "";
        this.shouldCreateGists = true;
        this.warningsModeratorOnly = false;
        this.patronRole = 0L;
        this.shouldSendStartupMessage = true;
        this.shouldSendChangelog = true;
        this.artistNsfwFilterEnabled = true;
        this.chatRevivalEnabled = false;
        this.chatRevivalTime = 24;
        this.chatRevivalChannel = 0L;
        this.warningXpPercentage = 0F;
        this.warningEconomyPercentage = 0F;
        this.announceBirthdays = true;
        this.enabledBirthdayUsers = new ArrayList<>();
        this.isCollectingEnabled = true;
        this.shouldAnnounceJoins = true;
        this.shouldAnnounceLeaves = false;
        this.imageSpamAutoBanEnabled = false;
        this.imageSpamWindowSeconds = 10;
        this.imageSpamMinImages = 3;
        this.imageSpamNewMemberThresholdHours = 48;
        this.discordInviteGuardEnabled = true;
        this.scamDetectionEnabled = true;
    }

    public static GuildData getOrCreateGuildData(long guildId) {
        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
        if (config == null) {
            config = new GuildData(guildId);
            Database.getDatabase().guildData.insertOne(config);
        }

        return config;
    }

    public static GuildData getOrCreateGuildData(Guild guild) {
        return GuildData.getOrCreateGuildData(guild.getIdLong());
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

    public static List<Long> getLongs(String str) {
        return Stream.of(str.split("[ ;]")).map(Longs::tryParse).filter(Objects::nonNull).toList();
    }
}
