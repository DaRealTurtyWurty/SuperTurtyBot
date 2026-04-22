package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.TempBan;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TempBanManager {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, ScheduledFuture<?>> SCHEDULED_UNBANS = new ConcurrentHashMap<>();

    private static volatile JDA jda;

    private TempBanManager() {
        throw new UnsupportedOperationException("TempBanManager is a utility class and cannot be instantiated!");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static void start(JDA jda) {
        TempBanManager.jda = jda;
        if (RUNNING.getAndSet(true))
            return;

        ShutdownHooks.register(SCHEDULER::shutdown);

        var tempBans = Database.getDatabase().tempBans.find()
                .sort(Sorts.ascending("expiresAt"))
                .into(new ArrayList<>());
        for (TempBan tempBan : tempBans) {
            if (!isValid(tempBan)) {
                deleteRecord(tempBan);
                continue;
            }

            scheduleUnban(tempBan);
        }
    }

    public static TempBan createOrUpdateTempBan(long guildId, long userId, long moderatorId, String reason, int deleteDays,
                                                long expiresAt) {
        TempBan tempBan = new TempBan(createId(guildId, userId), guildId, userId, moderatorId, reason, deleteDays,
                expiresAt, System.currentTimeMillis());
        Database.getDatabase().tempBans.replaceOne(
                Filters.eq("_id", tempBan.getId()),
                tempBan,
                new ReplaceOptions().upsert(true));
        scheduleUnban(tempBan);
        return tempBan;
    }

    public static void clearTempBan(long guildId, long userId) {
        String id = createId(guildId, userId);
        unschedule(id);
        Database.getDatabase().tempBans.deleteOne(Filters.eq("_id", id));
    }

    private static boolean isValid(TempBan tempBan) {
        return tempBan != null
                && tempBan.getId() != null
                && !tempBan.getId().isBlank()
                && tempBan.getGuild() != 0L
                && tempBan.getUser() != 0L
                && tempBan.getModerator() != 0L
                && tempBan.getReason() != null
                && !tempBan.getReason().isBlank()
                && tempBan.getDeleteDays() >= 0
                && tempBan.getDeleteDays() <= 7
                && tempBan.getExpiresAt() > 0L
                && tempBan.getCreatedAt() > 0L;
    }

    private static void scheduleUnban(TempBan tempBan) {
        if (jda == null || !isValid(tempBan))
            return;

        unschedule(tempBan.getId());

        long delayMillis = Math.max(0L, tempBan.getExpiresAt() - System.currentTimeMillis());
        ScheduledFuture<?> future = SCHEDULER.schedule(() -> {
            try {
                fireUnban(tempBan.getGuild(), tempBan.getUser(), tempBan.getId());
            } catch (Exception exception) {
                Constants.LOGGER.error("Failed to process tempban expiry {}", tempBan.getId(), exception);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
        SCHEDULED_UNBANS.put(tempBan.getId(), future);
    }

    private static void fireUnban(long guildId, long userId, String tempBanId) {
        SCHEDULED_UNBANS.remove(tempBanId);

        TempBan tempBan = Database.getDatabase().tempBans.find(Filters.eq("_id", tempBanId)).first();
        if (tempBan == null)
            return;

        if (!isValid(tempBan)) {
            deleteRecord(tempBan);
            return;
        }

        if (tempBan.getExpiresAt() > System.currentTimeMillis()) {
            scheduleUnban(tempBan);
            return;
        }

        if (jda == null) {
            scheduleUnban(tempBan);
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            deleteRecord(tempBan);
            return;
        }

        guild.unban(UserSnowflake.fromId(userId)).reason("Temporary ban expired").queue(
                success -> {
                    notifyUserAndLog(tempBan, guild);
                    deleteRecord(tempBan);
                },
                failure -> {
                    Constants.LOGGER.warn("Failed to unban {} in {} after tempban expiry", userId, guildId, failure);
                    deleteRecord(tempBan);
                });
    }

    private static void notifyUserAndLog(TempBan tempBan, Guild guild) {
        jda.retrieveUserById(tempBan.getUser()).queue(
                user -> {
                    user.openPrivateChannel().queue(
                            channel -> channel.sendMessage("You have been unbanned from `" + guild.getName()
                                    + "` because your temporary ban expired!").queue(
                                            ignored -> {
                                            },
                                            ignored -> {
                                            }),
                            ignored -> {
                            });
                    logExpiry(tempBan, guild, user);
                },
                failure -> logExpiry(tempBan, guild, null));
    }

    private static void logExpiry(TempBan tempBan, Guild guild, User user) {
        final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
        if (!Boolean.TRUE.equals(logging.getKey()))
            return;

        String target = user != null ? user.getAsMention() : "<@" + tempBan.getUser() + ">";
        String moderator = "<@" + tempBan.getModerator() + ">";
        BanCommand.log(logging.getValue(),
                target + "'s temporary ban set by " + moderator + " has expired and they have been unbanned!",
                true);
    }

    private static void deleteRecord(TempBan tempBan) {
        if (tempBan == null)
            return;

        unschedule(tempBan.getId());
        Database.getDatabase().tempBans.deleteOne(Filters.eq("_id", tempBan.getId()));
    }

    private static void unschedule(String tempBanId) {
        if (tempBanId == null || tempBanId.isBlank())
            return;

        ScheduledFuture<?> future = SCHEDULED_UNBANS.remove(tempBanId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private static String createId(long guildId, long userId) {
        return guildId + ":" + userId;
    }
}
