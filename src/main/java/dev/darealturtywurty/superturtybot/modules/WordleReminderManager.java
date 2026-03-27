package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.WordleStreakData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.WordleProfile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WordleReminderManager {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, ScheduledFuture<?>> SCHEDULED_REMINDERS = new ConcurrentHashMap<>();

    private static volatile JDA jda;

    private WordleReminderManager() {
        throw new UnsupportedOperationException("WordleReminderManager is a utility class and cannot be instantiated!");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static void start(JDA jda) {
        WordleReminderManager.jda = jda;
        if (RUNNING.getAndSet(true))
            return;

        ShutdownHooks.register(SCHEDULER::shutdown);

        for (WordleProfile profile : Database.getDatabase().wordleProfiles.find().into(new ArrayList<>())) {
            for (WordleStreakData streakData : profile.getStreaks()) {
                if (!isPendingReminder(streakData))
                    continue;

                scheduleReminder(profile.getUser(), streakData.getGuild(), streakData.getReminderAt());
            }
        }
    }

    public static void scheduleReminder(long userId, long guildId, long reminderAt) {
        if (jda == null || reminderAt <= 0L)
            return;

        String reminderKey = createReminderKey(userId, guildId);
        unschedule(reminderKey);

        long delayMillis = Math.max(0L, reminderAt - System.currentTimeMillis());
        ScheduledFuture<?> future = SCHEDULER.schedule(() -> fireReminder(userId, guildId), delayMillis, TimeUnit.MILLISECONDS);
        SCHEDULED_REMINDERS.put(reminderKey, future);
    }

    private static void fireReminder(long userId, long guildId) {
        String reminderKey = createReminderKey(userId, guildId);
        SCHEDULED_REMINDERS.remove(reminderKey);

        WordleProfile profile = Database.getDatabase().wordleProfiles.find(Filters.eq("user", userId)).first();
        if (profile == null)
            return;

        WordleStreakData streakData = profile.getStreaks()
                .stream()
                .filter(streak -> streak.getGuild() == guildId)
                .findFirst()
                .orElse(null);
        if (streakData == null || !isPendingReminder(streakData))
            return;

        long reminderAt = streakData.getReminderAt();
        if (reminderAt > System.currentTimeMillis()) {
            scheduleReminder(userId, guildId, reminderAt);
            return;
        }

        if (jda == null) {
            markReminderSent(userId, guildId);
            return;
        }

        jda.retrieveUserById(userId).queue(
                user -> sendReminder(user, streakData),
                failure -> {
                    Constants.LOGGER.warn("Failed to retrieve user {} for Wordle reminder", userId, failure);
                    markReminderSent(userId, guildId);
                });
    }

    private static void sendReminder(User user, WordleStreakData streakData) {
        MessageChannel destination = findReminderDestination(streakData);
        if (destination != null) {
            destination.sendMessage(formatChannelReminderMessage(user.getIdLong(), streakData.getStreak()))
                    .setAllowedMentions(List.of(Message.MentionType.USER))
                    .queue(
                            success -> markReminderSent(user.getIdLong(), streakData.getGuild()),
                            failure -> sendDirectMessage(user, streakData));
            return;
        }

        sendDirectMessage(user, streakData);
    }

    private static @Nullable MessageChannel findReminderDestination(WordleStreakData streakData) {
        if (jda == null || streakData.getGuild() == 0L || streakData.getReminderChannelId() == 0L)
            return null;

        Guild guild = jda.getGuildById(streakData.getGuild());
        if (guild == null)
            return null;

        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, streakData.getReminderChannelId());
        if (channel != null)
            return channel;

        ThreadChannel thread = guild.getThreadChannelById(streakData.getReminderChannelId());
        return thread;
    }

    private static void sendDirectMessage(User user, WordleStreakData streakData) {
        user.openPrivateChannel().queue(
                channel -> channel.sendMessage(formatDirectReminderMessage(streakData.getGuild(), streakData.getStreak())).queue(
                        success -> markReminderSent(user.getIdLong(), streakData.getGuild()),
                        failure -> {
                            Constants.LOGGER.warn("Failed to send Wordle reminder to user {}", user.getIdLong(), failure);
                            markReminderSent(user.getIdLong(), streakData.getGuild());
                        }),
                failure -> {
                    Constants.LOGGER.warn("Failed to open DM for Wordle reminder for user {}", user.getIdLong(), failure);
                    markReminderSent(user.getIdLong(), streakData.getGuild());
                });
    }

    private static String formatChannelReminderMessage(long userId, int streak) {
        return "<@%d> you can play Wordle again here. Your current streak is %d day%s.".formatted(userId, streak, streak == 1 ? "" : "s");
    }

    private static String formatDirectReminderMessage(long guildId, int streak) {
        if (guildId == 0L)
            return "You can play Wordle again in DMs. Your current streak is %d day%s.".formatted(streak, streak == 1 ? "" : "s");

        Guild guild = jda == null ? null : jda.getGuildById(guildId);
        String guildName = guild == null ? "that server" : guild.getName();
        return "You can play Wordle again in %s. Your current streak there is %d day%s.".formatted(guildName, streak, streak == 1 ? "" : "s");
    }

    private static boolean isPendingReminder(WordleStreakData streakData) {
        return streakData.getReminderAt() > 0L && !streakData.isReminderSent();
    }

    private static void markReminderSent(long userId, long guildId) {
        Database.getDatabase().wordleProfiles.updateOne(
                Filters.and(
                        Filters.eq("user", userId),
                        Filters.eq("streaks.guild", guildId)),
                Updates.combine(
                        Updates.set("streaks.$.reminderSent", true),
                        Updates.set("streaks.$.reminderAt", 0L)));
    }

    private static void unschedule(String reminderKey) {
        ScheduledFuture<?> future = SCHEDULED_REMINDERS.remove(reminderKey);
        if (future != null)
            future.cancel(false);
    }

    private static String createReminderKey(long userId, long guildId) {
        return userId + ":" + guildId;
    }
}
