package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReminderManager {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, ScheduledFuture<?>> SCHEDULED_REMINDERS = new ConcurrentHashMap<>();

    private static volatile JDA jda;

    private ReminderManager() {
        throw new UnsupportedOperationException("ReminderManager is a utility class and cannot be instantiated!");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static void start(JDA jda) {
        ReminderManager.jda = jda;
        if (RUNNING.getAndSet(true))
            return;

        ShutdownHooks.register(SCHEDULER::shutdown);

        List<Reminder> reminders = Database.getDatabase().reminders.find().sort(Sorts.ascending("time")).into(new ArrayList<>());
        for (Reminder reminder : reminders) {
            if (!isValid(reminder)) {
                deleteReminderRecord(reminder);
                continue;
            }

            scheduleReminder(reminder);
        }
    }

    public static Reminder createReminder(long guildId, long userId, long channelId, String reminderText, long time) {
        var reminder = new Reminder(generateReminderId(guildId, userId), guildId, userId, reminderText, channelId, time, System.currentTimeMillis());
        Database.getDatabase().reminders.insertOne(reminder);
        scheduleReminder(reminder);
        return reminder;
    }

    public static List<Reminder> getRemindersForUser(long userId) {
        List<Reminder> reminders = Database.getDatabase().reminders.find(Filters.eq("user", userId))
                .sort(Sorts.ascending("time"))
                .into(new ArrayList<>());

        reminders.removeIf(reminder -> {
            if (isValid(reminder))
                return false;

            deleteReminderRecord(reminder);
            return true;
        });

        reminders.forEach(ReminderManager::scheduleReminder);
        return reminders;
    }

    public static boolean deleteReminder(long guildId, long userId, String reminderId) {
        Reminder reminder = Database.getDatabase().reminders.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("user", userId),
                Filters.eq("_id", normalizeReminderId(reminderId)))).first();
        if (reminder == null)
            return false;

        unschedule(reminder.getId());
        Database.getDatabase().reminders.deleteOne(
                Filters.and(
                        Filters.eq("guild", guildId),
                        Filters.eq("user", userId),
                        Filters.eq("_id", reminder.getId())
                )
        );
        return true;
    }

    public static long clearReminders(long userId) {
        List<Reminder> reminders = getRemindersForUser(userId);
        reminders.forEach(reminder -> unschedule(reminder.getId()));
        return Database.getDatabase().reminders.deleteMany(Filters.eq("user", userId)).getDeletedCount();
    }

    private static boolean isValid(Reminder reminder) {
        return reminder.getId() != null
                && !reminder.getId().isBlank()
                && reminder.getUser() != 0L
                && reminder.getReminder() != null
                && !reminder.getReminder().isBlank()
                && reminder.getTime() > 0L
                && reminder.getCreatedAt() > 0L;
    }

    private static void scheduleReminder(Reminder reminder) {
        if (jda == null || reminder == null || !isValid(reminder))
            return;

        unschedule(reminder.getId());

        long delayMillis = Math.max(0L, reminder.getTime() - System.currentTimeMillis());
        ScheduledFuture<?> future = SCHEDULER.schedule(() -> {
            try {
                fireReminder(reminder.getGuild(), reminder.getUser(), reminder.getId());
            } catch (Exception exception) {
                Constants.LOGGER.error("Failed to fire reminder {}", reminder.getId(), exception);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
        SCHEDULED_REMINDERS.put(reminder.getId(), future);
    }

    private static void fireReminder(long guildId, long userId, String reminderId) {
        SCHEDULED_REMINDERS.remove(reminderId);

        Reminder reminder = Database.getDatabase().reminders.find(
                Filters.and(
                        Filters.eq("guild", guildId),
                        Filters.eq("user", userId),
                        Filters.eq("_id", reminderId))
        ).first();
        if (reminder == null)
            return;

        if (!isValid(reminder)) {
            deleteReminderRecord(reminder.getGuild(), reminder.getUser(), reminderId);
            return;
        }

        if (reminder.getTime() > System.currentTimeMillis()) {
            scheduleReminder(reminder);
            return;
        }

        if (jda == null) {
            scheduleReminder(reminder);
            return;
        }

        jda.retrieveUserById(reminder.getUser()).queue(
                user -> {
                    MessageChannel destination = findGuildDestination(reminder);
                    if (destination != null) {
                        sendReminder(destination, reminder, () -> sendDirectMessage(reminder.getGuild(), user, reminder));
                        return;
                    }

                    sendDirectMessage(reminder.getGuild(), user, reminder);
                },
                failure -> {
                    Constants.LOGGER.warn("Failed to retrieve user {} for reminder {}", reminder.getUser(), reminder.getId(), failure);
                    deleteReminderRecord(reminder.getGuild(), reminder.getUser(), reminder.getId());
                });
    }

    private static @Nullable MessageChannel findGuildDestination(Reminder reminder) {
        if (reminder.getGuild() == 0L || reminder.getChannel() == 0L)
            return null;

        Guild guild = jda.getGuildById(reminder.getGuild());
        if (guild == null)
            return null;

        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, reminder.getChannel());
        if (channel != null)
            return channel;

        return guild.getThreadChannelById(reminder.getChannel());
    }

    private static void sendReminder(MessageChannel channel, Reminder reminder, Runnable fallback) {
        channel.sendMessage(formatReminderMessage(reminder))
                .addEmbeds(createReminderEmbed(reminder, false))
                .setAllowedMentions(List.of(Message.MentionType.USER))
                .queue(
                        success -> deleteReminderRecord(reminder.getGuild(), reminder.getUser(), reminder.getId()),
                        _ -> fallback.run());
    }

    private static void sendDirectMessage(long guildId, User user, Reminder reminder) {
        user.openPrivateChannel().queue(
                channel -> channel.sendMessageEmbeds(createReminderEmbed(reminder, true))
                        .queue(
                                success -> deleteReminderRecord(guildId, user.getIdLong(), reminder.getId()),
                                failure -> {
                                    Constants.LOGGER.warn("Failed to send reminder {} to user {}", reminder.getId(), reminder.getUser(), failure);
                                    deleteReminderRecord(guildId, user.getIdLong(), reminder.getId());
                                }),
                failure -> {
                    Constants.LOGGER.warn("Failed to open DM for reminder {} and user {}", reminder.getId(), reminder.getUser(), failure);
                    deleteReminderRecord(guildId, user.getIdLong(), reminder.getId());
                });
    }

    private static String formatReminderMessage(Reminder reminder) {
        return "<@%d>".formatted(reminder.getUser());
    }

    private static MessageEmbed createReminderEmbed(Reminder reminder, boolean directMessage) {
        String description = reminder.getReminder().trim();
        if (description.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
            description = description.substring(0, MessageEmbed.DESCRIPTION_MAX_LENGTH - 3) + "...";
        }

        var embed = new EmbedBuilder()
                .setTitle("\u23F0 Reminder")
                .setDescription(description)
                .addField("Reminder ID", '`' + reminder.getId() + '`', true)
                .addField("Created", TimeFormat.RELATIVE.format(reminder.getCreatedAt()), true)
                .addField("Delivery", directMessage ? "Direct Message" : "This channel", true)
                .setColor(new Color(0xF0B232))
                .setTimestamp(Instant.ofEpochMilli(reminder.getTime()));

        if (directMessage && reminder.getGuild() != 0L && jda != null) {
            Guild guild = jda.getGuildById(reminder.getGuild());
            if (guild != null) {
                embed.addField("Server", guild.getName(), true);
            }
        }

        return embed.build();
    }

    private static void deleteReminderRecord(long guildId, long userId, String reminderId) {
        unschedule(reminderId);
        Database.getDatabase().reminders.deleteOne(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("user", userId),
                Filters.eq("_id", reminderId)));
    }

    private static void deleteReminderRecord(Reminder reminder) {
        if (reminder == null)
            return;

        unschedule(reminder.getId());
        Database.getDatabase().reminders.deleteOne(Filters.and(
                Filters.eq("user", reminder.getUser()),
                Filters.eq("time", reminder.getTime()),
                Filters.eq("createdAt", reminder.getCreatedAt())));
    }

    private static void unschedule(String reminderId) {
        if (reminderId == null || reminderId.isBlank())
            return;

        ScheduledFuture<?> future = SCHEDULED_REMINDERS.remove(reminderId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private static String generateReminderId(long guildId, long userId) {
        for (int attempts = 0; attempts < 10; attempts++) {
            String candidate = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            Reminder existing = Database.getDatabase().reminders.find(
                    Filters.and(
                            Filters.eq("guild", guildId),
                            Filters.eq("user", userId),
                            Filters.eq("_id", candidate))
            ).first();
            if (existing == null)
                return candidate;
        }

        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static @Nullable String normalizeReminderId(String reminderId) {
        if (reminderId == null)
            return null;

        String normalizedId = reminderId.trim().toUpperCase(Locale.ROOT);
        return normalizedId.isBlank() ? null : normalizedId;
    }
}
