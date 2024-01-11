package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BirthdayManager {
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private BirthdayManager() {
        throw new UnsupportedOperationException("BirthdayManager is a utility class and cannot be instantiated!");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static void start(JDA jda) {
        if (RUNNING.getAndSet(true))
            return;

        // add the task
        DailyTaskScheduler.addTask(new DailyTask(() -> {
            var now = LocalDate.now();
            int day = now.getDayOfMonth();
            int month = now.getMonthValue();

            List<Birthday> birthdays = Database.getDatabase().birthdays.find()
                    .filter(Filters.and(Filters.eq("day", day), Filters.eq("month", month)))
                    .into(new ArrayList<>());
            if (birthdays.isEmpty())
                return;

            List<GuildData> enabledGuilds = Database.getDatabase().guildData.find(
                    Filters.and(
                            Filters.eq("announceBirthdays", true),
                            Filters.ne("birthdayChannel", 0L)
                    )).into(new ArrayList<>());
            for (GuildData guildData : enabledGuilds) {
                List<Long> guildBirthdayUsers = guildData.getEnabledBirthdayUsers();
                long birthdayChannelId = guildData.getBirthdayChannel();
                if (guildBirthdayUsers.isEmpty() || birthdayChannelId == 0L)
                    continue;

                Guild guild = jda.getGuildById(guildData.getGuild());
                if (guild == null)
                    continue;

                TextChannel birthdayChannel = guild.getTextChannelById(birthdayChannelId);
                if (birthdayChannel == null)
                    continue;

                guild.retrieveMembersByIds(guildBirthdayUsers).onSuccess((List<Member> members) -> {
                    for (Member member : members) {
                        Birthday birthday = getBirthday(member.getIdLong());
                        if (birthday == null)
                            continue;

                        int age = TimeUtils.calculateAge(
                                birthday.getDay(),
                                birthday.getMonth(),
                                birthday.getYear()
                        );
                        birthdayChannel.sendMessageFormat(
                                "🎉 %s is celebrating their %d%s birthday today! Happy birthday!",
                                "<@" + birthday.getUser() + ">",
                                age,
                                StringUtils.getOrdinalSuffix(age)
                        ).queue();
                    }
                });
            }
        }, 0, 0));
    }

    public static Birthday getBirthday(long userId) {
        return Database.getDatabase().birthdays.find(Filters.eq("user", userId)).first();
    }

    public static Birthday addBirthday(long userId, int day, int month, int year) {
        var birthday = new Birthday(userId, day, month, year);
        Database.getDatabase().birthdays.insertOne(birthday);
        return birthday;
    }

    public static void setBirthdayAnnouncementsEnabled(long guildId, long userId, boolean enabled) {
        GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
        if (guildData == null)
            return;

        if (enabled) {
            if (guildData.getEnabledBirthdayUsers().contains(userId))
                return;

            guildData.getEnabledBirthdayUsers().add(userId);
        } else {
            guildData.getEnabledBirthdayUsers().remove(userId);
        }

        Database.getDatabase().guildData.updateOne(Filters.eq("guild", guildId),
                Updates.set("enabledBirthdayUsers", guildData.getEnabledBirthdayUsers()));
    }
}
