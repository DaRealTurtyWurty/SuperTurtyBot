package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTask;
import dev.darealturtywurty.superturtybot.core.util.discord.DailyTaskScheduler;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BirthdayManager {
    private static final Map<Short, List<Long>> DAY_TO_USERS_MAP = new HashMap<>();
    private static final List<Long> GUILD_BIRTHDAYS = new ArrayList<>();

    private BirthdayManager() {}

    public static void init(JDA jda) {
        DailyTaskScheduler.addTask(new DailyTask(() -> {
            short dayOfYear = (short) LocalDate.now().getDayOfYear();
            if (!DAY_TO_USERS_MAP.containsKey(dayOfYear))
                return;

            List<Long> users = DAY_TO_USERS_MAP.get(dayOfYear);
            if (users.isEmpty())
                return;

            for (Long userId : users) {
                Birthday birthday = getBirthday(userId);
                if (birthday == null) {
                    users.remove(userId);
                    continue;
                }

                for (Long guildId : GUILD_BIRTHDAYS) {
                    GuildConfig config = getGuildConfig(guildId);
                    if (config == null) {
                        GUILD_BIRTHDAYS.remove(guildId);
                        continue;
                    }

                    if (!config.isAnnounceBirthdays()) {
                        GUILD_BIRTHDAYS.remove(guildId);
                        continue;
                    }

                    Guild guild = jda.getGuildById(guildId);
                    if (guild == null) {
                        GUILD_BIRTHDAYS.remove(guildId);
                        continue;
                    }

                    TextChannel channel = guild.getTextChannelById(config.getBirthdayChannel());
                    if (channel == null) {
                        config.setBirthdayChannel(0L);
                        Database.getDatabase().guildConfig.replaceOne(Filters.eq("guild", guildId), config);
                        GUILD_BIRTHDAYS.remove(guildId);
                        continue;
                    }

                    channel.sendMessageFormat("🎉 Happy Birthday <@%d>! 🎂", userId).queue();
                }
            }
        }, 0, 0));
    }

    public static Birthday getBirthday(long userId) {
        return Database.getDatabase().birthdays.find(Filters.eq("user", userId)).first();
    }

    public static GuildConfig getGuildConfig(long guildId) {
        return Database.getDatabase().guildConfig.find(Filters.eq("guild", guildId)).first();
    }

    public static void addBirthday(long userId, short dayOfYear) {
        DAY_TO_USERS_MAP.computeIfAbsent(dayOfYear, k -> new ArrayList<>()).add(userId);
    }

    public static void addGuildBirthday(long guildId) {
        GUILD_BIRTHDAYS.add(guildId);
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

    public static void removeGuildBirthday(long guildId) {
        GUILD_BIRTHDAYS.remove(guildId);
    }

    public static final class Listener extends ListenerAdapter {
        private static final Listener INSTANCE = new Listener();

        private Listener() {
        }

        @Override
        public void onGuildReady(@NotNull GuildReadyEvent event) {
            Guild guild = event.getGuild();
            GuildConfig config = getGuildConfig(guild.getIdLong());
            if (config == null) {
                config = new GuildConfig(guild.getIdLong());
                config.setBirthdayChannel(guild.getSystemChannel() == null ? 0L : guild.getSystemChannel().getIdLong());
                Database.getDatabase().guildConfig.insertOne(config);
            }

            TextChannel channel = guild.getTextChannelById(config.getBirthdayChannel());
            if (config.isAnnounceBirthdays() && channel != null) {
                addGuildBirthday(guild.getIdLong());
            }
        }

        @Override
        public void onGuildLeave(@NotNull GuildLeaveEvent event) {
            removeGuildBirthday(event.getGuild().getIdLong());
        }

        @Override
        public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
            // TODO: Figure out how to remove birthday if a user leaves all servers.
        }
    }
}
