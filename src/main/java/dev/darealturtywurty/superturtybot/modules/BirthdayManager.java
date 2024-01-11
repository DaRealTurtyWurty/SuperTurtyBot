package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BirthdayManager {
    private static final Map<Short, List<Long>> DAY_TO_USERS_MAP = new HashMap<>();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private BirthdayManager() {
        throw new UnsupportedOperationException("BirthdayManager is a utility class and cannot be instantiated!");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static void init(JDA jda) {
        if (RUNNING.getAndSet(true))
            return;

        // populate the map
        List<Birthday> startupBirthdays = Database.getDatabase().birthdays.find().into(new ArrayList<>());
        for (Birthday birthday : startupBirthdays) {
            short dayOfYear = (short) LocalDate.of(
                    birthday.getYear(),
                    birthday.getMonth(),
                    birthday.getDay()).getDayOfYear();
            DAY_TO_USERS_MAP.computeIfAbsent(dayOfYear, k -> new ArrayList<>()).add(birthday.getUser());
        }

        // add the task
        DailyTaskScheduler.addTask(new DailyTask(() -> {
            short dayOfYear = (short) LocalDate.now().getDayOfYear();
            if (!DAY_TO_USERS_MAP.containsKey(dayOfYear))
                return;

            List<Long> users = DAY_TO_USERS_MAP.get(dayOfYear);
            if (users.isEmpty())
                return;

            List<Birthday> birthdays = users.stream()
                    .map(BirthdayManager::getBirthday)
                    .filter(Objects::nonNull)
                    .toList();
            if (birthdays.isEmpty()) {
                DAY_TO_USERS_MAP.remove(dayOfYear);
                return;
            }

            List<GuildConfig> enabledGuilds = Database.getDatabase().guildConfig.find(
                    Filters.and(
                            Filters.eq("announceBirthdays", true),
                            Filters.ne("birthdayChannel", 0L)
                    )).into(new ArrayList<>());
            for (GuildConfig guildConfig : enabledGuilds) {
                // check if the guild has the birthday channel set
                long birthdayChannelId = guildConfig.getBirthdayChannel();
                if (birthdayChannelId == 0L)
                    continue;

                TextChannel birthdayChannel = jda.getTextChannelById(birthdayChannelId);
                if (birthdayChannel == null) {
                    guildConfig.setBirthdayChannel(0L);
                    Database.getDatabase().guildConfig.updateOne(
                            Filters.eq("guild", guildConfig.getGuild()),
                            Updates.set("birthdayChannel", 0L));
                    continue;
                }

                // for each user, see if they are in the guild
                for (long userId : users) {
                    if (birthdayChannel.getGuild().isMember(UserSnowflake.fromId(userId))) {
                        Optional<Birthday> optBirthday = birthdays.stream()
                                .filter(b -> b.getUser() == userId)
                                .findFirst();
                        if (optBirthday.isEmpty()) {
                            removeBirthday(userId);
                            continue;
                        }

                        Birthday birthday = optBirthday.get();
                        birthdayChannel.sendMessageFormat(
                                "🎉 %s is celebrating their %d birthday today! Happy birthday!",
                                "<@" + birthday.getUser() + ">",
                                TimeUtils.calculateAge(
                                        birthday.getDay(),
                                        birthday.getMonth(),
                                        birthday.getYear()
                                )).queue();
                    }
                }
            }
        }, 20, 20));
    }

    public static Birthday getBirthday(long userId) {
        return Database.getDatabase().birthdays.find(Filters.eq("user", userId)).first();
    }

    public static void addBirthday(long userId, short dayOfYear) {
        DAY_TO_USERS_MAP.computeIfAbsent(dayOfYear, k -> new ArrayList<>()).add(userId);
    }

    public static void removeBirthday(long userId) {
        Map<Short, List<Long>> map = new HashMap<>(DAY_TO_USERS_MAP);
        for (Map.Entry<Short, List<Long>> entry : map.entrySet()) {
            List<Long> users = entry.getValue();
            if (users.contains(userId)) {
                users.remove(userId);
                if (users.isEmpty()) {
                    DAY_TO_USERS_MAP.remove(entry.getKey());
                }
            }
        }
    }
}
