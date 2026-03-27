package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
            if (!isValid(reminder))
                continue;

            scheduleReminder(reminder);
        }
    }

    public static Reminder createReminder(long guildId, long userId, long channelId, String reminderText, long time) {
        Reminder reminder = new Reminder(generateReminderId(), guildId, userId, reminderText, channelId, time, System.currentTimeMillis());
        Database.getDatabase().reminders.insertOne(reminder);
        scheduleReminder(reminder);
        return reminder;
    }

    public static List<Reminder> getRemindersForUser(long userId) {
        return Database.getDatabase().reminders.find(Filters.eq("user", userId))
                .sort(Sorts.ascending("time"))
                .into(new ArrayList<>());
    }

    public static boolean deleteReminder(long userId, String reminderId) {
        Reminder reminder = Database.getDatabase().reminders.find(Filters.and(
                Filters.eq("user", userId),
                Filters.eq("id", reminderId.toUpperCase(Locale.ROOT)))).first();
        if (reminder == null)
            return false;

        unschedule(reminder.getId());
        Database.getDatabase().reminders.deleteOne(Filters.eq("id", reminder.getId()));
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
        if (jda == null)
            return;

        unschedule(reminder.getId());

        long delayMillis = Math.max(0L, reminder.getTime() - System.currentTimeMillis());
        ScheduledFuture<?> future = SCHEDULER.schedule(() -> fireReminder(reminder.getId()), delayMillis, TimeUnit.MILLISECONDS);
        SCHEDULED_REMINDERS.put(reminder.getId(), future);
    }

    private static void fireReminder(String reminderId) {
        SCHEDULED_REMINDERS.remove(reminderId);

        Reminder reminder = Database.getDatabase().reminders.find(Filters.eq("id", reminderId)).first();
        if (reminder == null)
            return;

        User user = jda.getUserById(reminder.getUser());
        if (user == null) {
            deleteReminderRecord(reminderId);
            return;
        }

        MessageChannel destination = findGuildDestination(reminder);
        if (destination != null) {
            sendReminder(destination, reminder, () -> sendDirectMessage(user, reminder));
            return;
        }

        sendDirectMessage(user, reminder);
    }

    private static MessageChannel findGuildDestination(Reminder reminder) {
        if (reminder.getGuild() == 0L || reminder.getChannel() == 0L)
            return null;

        Guild guild = jda.getGuildById(reminder.getGuild());
        if (guild == null)
            return null;

        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, reminder.getChannel());
        if (channel != null)
            return channel;

        ThreadChannel thread = guild.getThreadChannelById(reminder.getChannel());
        return thread;
    }

    private static void sendReminder(MessageChannel channel, Reminder reminder, Runnable fallback) {
        channel.sendMessage(formatReminderMessage(reminder))
                .setAllowedMentions(List.of(Message.MentionType.USER))
                .queue(
                        success -> deleteReminderRecord(reminder.getId()),
                        failure -> fallback.run());
    }

    private static void sendDirectMessage(User user, Reminder reminder) {
        user.openPrivateChannel().queue(
                channel -> channel.sendMessage(formatReminderMessage(reminder))
                        .setAllowedMentions(List.of(Message.MentionType.USER))
                        .queue(
                                success -> deleteReminderRecord(reminder.getId()),
                                failure -> {
                                    Constants.LOGGER.warn("Failed to send reminder {} to user {}", reminder.getId(), reminder.getUser(), failure);
                                    deleteReminderRecord(reminder.getId());
                                }),
                failure -> {
                    Constants.LOGGER.warn("Failed to open DM for reminder {} and user {}", reminder.getId(), reminder.getUser(), failure);
                    deleteReminderRecord(reminder.getId());
                });
    }

    private static String formatReminderMessage(Reminder reminder) {
        return "⏰ <@%d> reminder: %s".formatted(reminder.getUser(), reminder.getReminder());
    }

    private static void deleteReminderRecord(String reminderId) {
        unschedule(reminderId);
        Database.getDatabase().reminders.deleteOne(Filters.eq("id", reminderId));
    }

    private static void unschedule(String reminderId) {
        if (reminderId == null || reminderId.isBlank())
            return;

        ScheduledFuture<?> future = SCHEDULED_REMINDERS.remove(reminderId);
        if (future != null)
            future.cancel(false);
    }

    private static String generateReminderId() {
        for (int attempts = 0; attempts < 10; attempts++) {
            String candidate = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
            Reminder existing = Database.getDatabase().reminders.find(Filters.eq("id", candidate)).first();
            if (existing == null)
                return candidate;
        }

        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
