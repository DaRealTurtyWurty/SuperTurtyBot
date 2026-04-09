package dev.darealturtywurty.superturtybot.modules.modmail;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.ModmailTranscriptEntry;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ModmailBlockedUser;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ModmailTicket;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ModmailTranscriptChunk;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class ModmailManager extends ListenerAdapter {
    public static final ModmailManager INSTANCE = new ModmailManager();

    public static final String TICKET_SOURCE_SLASH_COMMAND = "slash_command";

    private static final String CATEGORY_NAME_PREFIX = "modmail";
    private static final int MAX_CHANNELS_PER_CATEGORY = 50;
    private static final ConcurrentHashMap<Long, ReentrantLock> GUILD_LOCKS = new ConcurrentHashMap<>();

    private static final EnumSet<Permission> CATEGORY_DENIED = EnumSet.of(Permission.VIEW_CHANNEL);
    private static final EnumSet<Permission> NONE = EnumSet.noneOf(Permission.class);
    private static final EnumSet<Permission> BOT_ALLOWED = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_MANAGE,
            Permission.MANAGE_CHANNEL,
            Permission.MANAGE_PERMISSIONS
    );
    private static final EnumSet<Permission> MODERATOR_ALLOWED = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EMBED_LINKS
    );
    private static final EnumSet<Permission> USER_ALLOWED = EnumSet.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EMBED_LINKS
    );

    private ModmailManager() {
    }

    public static TicketCreationResult createTicket(Member creator, String initialMessage, String source) {
        Objects.requireNonNull(creator, "creator");

        Guild guild = creator.getGuild();
        ReentrantLock lock = GUILD_LOCKS.computeIfAbsent(guild.getIdLong(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            if (isModerator(creator))
                throw new ModmailException("❌ Moderators cannot create modmail tickets for themselves.");

            if (getOpenTicketByUser(guild.getIdLong(), creator.getIdLong()).isPresent())
                throw new ModmailException("❌ You already have an open ticket in this server.");

            if (isBlocked(guild.getIdLong(), creator.getIdLong()))
                throw new ModmailException("❌ You are blocked from creating tickets in this server.");

            List<Role> moderatorRoles = getConfiguredModeratorRoles(guild);
            if (moderatorRoles.isEmpty())
                throw new ModmailException("❌ No modmail moderator roles are configured. Ask the server owner to set `/serverconfig set modmail_moderator_roles <roles>` first.");

            validateBotPermissions(guild, moderatorRoles);

            Category category = findOrCreateCategory(guild, moderatorRoles);
            long ticketNumber = getNextTicketNumber(guild.getIdLong());
            String channelName = buildChannelName(ticketNumber, creator.getUser().getName());

            TextChannel channel = guild.createTextChannel(channelName, category)
                    .setTopic("Modmail ticket #" + ticketNumber + " for user " + creator.getIdLong())
                    .addPermissionOverride(guild.getPublicRole(), NONE, CATEGORY_DENIED)
                    .addPermissionOverride(guild.getSelfMember(), BOT_ALLOWED, NONE)
                    .addPermissionOverride(creator, USER_ALLOWED, NONE)
                    .complete();

            for (Role role : moderatorRoles) {
                channel.upsertPermissionOverride(role).setAllowed(MODERATOR_ALLOWED).complete();
            }

            var ticket = new ModmailTicket(
                    guild.getIdLong(),
                    creator.getIdLong(),
                    channel.getIdLong(),
                    category.getIdLong(),
                    ticketNumber,
                    source,
                    initialMessage
            );
            Database.getDatabase().modmailTickets.insertOne(ticket);

            sendOpeningMessage(channel, creator, moderatorRoles, initialMessage, ticketNumber);
            sendConfiguredCreatedMessage(channel);
            return new TicketCreationResult(ticket, channel);
        } finally {
            lock.unlock();
        }
    }

    public static TicketCloseResult closeTicket(ModmailTicket ticket, Member closedBy, String reason) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(closedBy, "closedBy");

        var guild = closedBy.getGuild();
        var lock = GUILD_LOCKS.computeIfAbsent(guild.getIdLong(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            if (!ticket.isOpen())
                throw new ModmailException("❌ This ticket is already closed.");

            TextChannel channel = guild.getTextChannelById(ticket.getChannel());
            if (channel == null)
                throw new ModmailException("❌ The ticket channel no longer exists.");

            TranscriptArchiveResult archiveResult = archiveTranscript(channel);
            ticket.setOpen(false);
            ticket.setClosedAt(System.currentTimeMillis());
            ticket.setClosedBy(closedBy.getIdLong());
            ticket.setCloseReason(reason == null ? "" : reason.trim());
            ticket.setTranscriptChunkCount(archiveResult.chunkCount());
            ticket.setTranscriptMessageCount(archiveResult.messageCount());

            Database.getDatabase().modmailTickets.replaceOne(
                    Filters.and(Filters.eq("guild", ticket.getGuild()),
                            Filters.eq("channel", ticket.getChannel())),
                    ticket,
                    new ReplaceOptions().upsert(true));
            return new TicketCloseResult(ticket, archiveResult.messageCount());
        } finally {
            lock.unlock();
        }
    }

    public static void deleteTicketChannel(Guild guild, ModmailTicket ticket) {
        if (guild == null || ticket == null)
            return;

        TextChannel channel = guild.getTextChannelById(ticket.getChannel());
        if (channel != null) {
            channel.delete().queue();
        }
    }

    public static boolean blockUser(long guildId, long userId, long blockedBy, String reason) {
        var blockedUser = new ModmailBlockedUser(
                guildId,
                userId,
                blockedBy,
                reason == null ? "" : reason.trim(),
                System.currentTimeMillis()
        );
        return Database.getDatabase().modmailBlockedUsers.replaceOne(
                Filters.and(Filters.eq("guild", guildId),
                        Filters.eq("user", userId)),
                blockedUser,
                new ReplaceOptions().upsert(true)).wasAcknowledged();
    }

    public static boolean unblockUser(long guildId, long userId) {
        return Database.getDatabase().modmailBlockedUsers.deleteOne(
                Filters.and(Filters.eq("guild", guildId),
                        Filters.eq("user", userId))).getDeletedCount() > 0;
    }

    public static boolean isBlocked(long guildId, long userId) {
        return getBlockedUser(guildId, userId).isPresent();
    }

    public static Optional<ModmailBlockedUser> getBlockedUser(long guildId, long userId) {
        return Optional.ofNullable(Database.getDatabase().modmailBlockedUsers.find(
                Filters.and(Filters.eq("guild", guildId),
                        Filters.eq("user", userId))).first());
    }

    public static Optional<ModmailTicket> getOpenTicketByUser(long guildId, long userId) {
        return Optional.ofNullable(Database.getDatabase().modmailTickets.find(
                Filters.and(
                        Filters.eq("guild", guildId),
                        Filters.eq("user", userId),
                        Filters.eq("open", true)
                )).first());
    }

    public static Optional<ModmailTicket> getOpenTicketByChannel(long guildId, long channelId) {
        return Optional.ofNullable(Database.getDatabase().modmailTickets.find(
                Filters.and(
                        Filters.eq("guild", guildId),
                        Filters.eq("channel", channelId),
                        Filters.eq("open", true)
                )).first());
    }

    public static boolean isModerator(Member member) {
        if (member == null)
            return false;

        if (member.isOwner() || member.hasPermission(Permission.MANAGE_SERVER))
            return true;

        List<Long> configuredRoles = GuildData.getLongs(GuildData.getOrCreateGuildData(member.getGuild()).getModmailModeratorRoles());
        if (configuredRoles.isEmpty())
            return false;

        return member.getRoles().stream().map(Role::getIdLong).anyMatch(configuredRoles::contains);
    }

    public static List<Role> getConfiguredModeratorRoles(Guild guild) {
        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        return GuildData.getLongs(guildData.getModmailModeratorRoles()).stream()
                .map(guild::getRoleById)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public static String missingConfigurationMessage() {
        return "❌ Modmail is not configured yet. The server owner must set `/serverconfig set modmail_moderator_roles <roles>` first.";
    }

    private static void validateBotPermissions(Guild guild, List<Role> moderatorRoles) {
        var self = guild.getSelfMember();
        EnumSet<Permission> requiredPermissions = EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_HISTORY,
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_PERMISSIONS
        );

        if (!self.hasPermission(requiredPermissions))
            throw new ModmailException("❌ I need `View Channel`, `Send Messages`, `Read Message History`, `Manage Channels`, and `Manage Permissions` to run modmail.");

        if (moderatorRoles.isEmpty())
            throw new ModmailException(missingConfigurationMessage());
    }

    private static long getNextTicketNumber(long guildId) {
        ModmailTicket latestTicket = Database.getDatabase().modmailTickets.find(Filters.eq("guild", guildId))
                .sort(Sorts.descending("ticketNumber"))
                .limit(1)
                .first();
        return latestTicket == null ? 1L : latestTicket.getTicketNumber() + 1L;
    }

    private static Category findOrCreateCategory(Guild guild, List<Role> moderatorRoles) {
        List<Category> categories = guild.getCategories().stream()
                .filter(category -> isManagedModmailCategory(category.getName()))
                .sorted(Comparator.comparingInt(left -> parseCategoryIndex(left.getName())))
                .collect(Collectors.toCollection(ArrayList::new));

        for (Category category : categories) {
            syncCategoryPermissions(category, guild, moderatorRoles);
            long childCount = category.getChannels().stream().filter(ICategorizableChannel.class::isInstance).count();
            if (childCount < MAX_CHANNELS_PER_CATEGORY)
                return category;
        }

        String nextName = CATEGORY_NAME_PREFIX + "-" + (categories.size() + 1);
        Category category = guild.createCategory(nextName)
                .addPermissionOverride(guild.getPublicRole(), NONE, CATEGORY_DENIED)
                .addPermissionOverride(guild.getSelfMember(), BOT_ALLOWED, NONE)
                .complete();
        syncCategoryPermissions(category, guild, moderatorRoles);
        return category;
    }

    private static void syncCategoryPermissions(Category category, Guild guild, List<Role> moderatorRoles) {
        category.upsertPermissionOverride(guild.getPublicRole()).setDenied(CATEGORY_DENIED).complete();
        category.upsertPermissionOverride(guild.getSelfMember()).setAllowed(BOT_ALLOWED).complete();
        for (Role role : moderatorRoles) {
            category.upsertPermissionOverride(role).setAllowed(MODERATOR_ALLOWED).complete();
        }
    }

    private static boolean isManagedModmailCategory(String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        return lowered.equals(CATEGORY_NAME_PREFIX) || lowered.matches("^" + CATEGORY_NAME_PREFIX + "-\\d+$");
    }

    private static int parseCategoryIndex(String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        if (lowered.equals(CATEGORY_NAME_PREFIX))
            return 0;

        int separator = lowered.lastIndexOf('-');
        if (separator < 0)
            return Integer.MAX_VALUE;

        try {
            return Integer.parseInt(lowered.substring(separator + 1));
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private static String buildChannelName(long ticketNumber, String username) {
        String sanitizedName = username.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (sanitizedName.isBlank())
            sanitizedName = "user";

        String prefix = "ticket-" + ticketNumber + "-";
        int maxNameLength = 100;
        int maxUserLength = Math.max(1, maxNameLength - prefix.length());
        if (sanitizedName.length() > maxUserLength)
            sanitizedName = sanitizedName.substring(0, maxUserLength);

        return prefix + sanitizedName;
    }

    private static void sendOpeningMessage(TextChannel channel, Member creator, List<Role> moderatorRoles, String initialMessage, long ticketNumber) {
        var embed = new EmbedBuilder()
                .setTitle("Modmail Ticket #" + ticketNumber)
                .setColor(Color.CYAN)
                .setTimestamp(Instant.now())
                .setDescription(initialMessage)
                .addField("Opened By", creator.getAsMention(), false)
                .addField("User ID", String.valueOf(creator.getIdLong()), false)
                .setFooter("Moderators can close this ticket with /modmail close");

        String moderatorMentions = moderatorRoles.stream()
                .map(Role::getAsMention)
                .distinct()
                .collect(Collectors.joining(" "));

        String content = moderatorMentions.isBlank() ?
                creator.getAsMention() :
                moderatorMentions + " " + creator.getAsMention();

        channel.sendMessage(content).setEmbeds(embed.build()).queue();
    }

    private static void sendConfiguredCreatedMessage(TextChannel channel) {
        GuildData guildData = GuildData.getOrCreateGuildData(channel.getGuild());
        String configuredMessage = guildData.getModmailTicketCreatedMessage();
        if (configuredMessage == null || configuredMessage.isBlank())
            return;

        channel.sendMessage(configuredMessage).queue();
    }

    private static TranscriptArchiveResult archiveTranscript(TextChannel channel) {
        Database.getDatabase().modmailTranscriptChunks.deleteMany(Filters.and(
                Filters.eq("guild", channel.getGuild().getIdLong()),
                Filters.eq("ticketChannel", channel.getIdLong())
        ));

        var history = new MessageHistory(channel);
        int chunkIndex = 0;
        int messageCount = 0;
        while (true) {
            List<Message> batch = history.retrievePast(100).complete();
            if (batch.isEmpty())
                break;

            List<ModmailTranscriptEntry> entries = batch.stream()
                    .map(ModmailManager::toTranscriptEntry)
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.reverse(entries);
            Database.getDatabase().modmailTranscriptChunks.insertOne(new ModmailTranscriptChunk(
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    chunkIndex++,
                    entries
            ));
            messageCount += entries.size();

            if (batch.size() < 100)
                break;
        }

        return new TranscriptArchiveResult(chunkIndex, messageCount);
    }

    private static ModmailTranscriptEntry toTranscriptEntry(Message message) {
        User author = message.getAuthor();
        List<String> attachments = message.getAttachments().stream()
                .map(attachment -> attachment.getUrl() + " (" + attachment.getFileName() + ")")
                .toList();
        List<String> embeds = message.getEmbeds().stream()
                .map(ModmailManager::describeEmbed)
                .toList();
        List<String> stickers = message.getStickers().stream()
                .map(sticker -> sticker.getName() + " (" + sticker.getId() + ")")
                .toList();

        return new ModmailTranscriptEntry(
                message.getIdLong(),
                author.getIdLong(),
                author.getName() + " (" + author.getId() + ")",
                author.isBot(),
                message.getContentRaw(),
                new ArrayList<>(attachments),
                new ArrayList<>(embeds),
                new ArrayList<>(stickers),
                message.getTimeCreated().toInstant().toEpochMilli(),
                message.isEdited() && message.getTimeEdited() != null ? message.getTimeEdited().toInstant().toEpochMilli() : 0L
        );
    }

    private static String describeEmbed(MessageEmbed embed) {
        var builder = new StringBuilder();
        if (embed.getTitle() != null && !embed.getTitle().isBlank()) {
            builder.append("title=").append(embed.getTitle());
        }

        if (embed.getUrl() != null && !embed.getUrl().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append("url=").append(embed.getUrl());
        }
        if (embed.getDescription() != null && !embed.getDescription().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append("description=").append(truncate(embed.getDescription(), 180));
        }

        if (builder.isEmpty()) {
            builder.append("type=").append(embed.getType());
        }

        return builder.toString();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength)
            return value;

        return value.substring(0, maxLength - 3) + "...";
    }

    public record TicketCreationResult(ModmailTicket ticket, TextChannel channel) {
    }

    public record TicketCloseResult(ModmailTicket ticket, int archivedMessageCount) {
    }

    private record TranscriptArchiveResult(int chunkCount, int messageCount) {
    }

    public static final class ModmailException extends RuntimeException {
        public ModmailException(String message) {
            super(message);
        }
    }
}
