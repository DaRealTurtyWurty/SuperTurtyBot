package dev.darealturtywurty.superturtybot.database.pojos.collections;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.database.Database;
import lombok.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
public class Reminder {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private long guild;
    private long user;

    private String reminder;
    private String message;
    private long channel;
    private long time;

    public Reminder(long guild, long user, String reminder, String message, long channel, long time) {
        this.guild = guild;
        this.user = user;
        this.reminder = reminder;
        this.message = message;
        this.channel = channel;
        this.time = time;

        ShutdownHooks.register(scheduler::shutdown);
    }

    public Reminder() {
        this(0, 0, "", "", 0, 0);
    }

    public void schedule(JDA jda) {
        scheduler.schedule(() -> {
            Database.getDatabase().reminders.deleteOne(Filters.and(
                    Filters.eq("guild", guild),
                    Filters.eq("user", this.user),
                    Filters.eq("reminder", reminder),
                    Filters.eq("message", message),
                    Filters.eq("channel", channel),
                    Filters.eq("time", time))
            );

            User user = jda.getUserById(this.user);
            if(user == null)
                return;

            Guild guild = jda.getGuildById(this.guild);
            if (guild == null) {
                user.openPrivateChannel().queue(this::remindChannel, throwable -> {});
                return;
            }

            TextChannel channel = guild.getTextChannelById(this.channel);
            if (channel == null) {
                user.openPrivateChannel().queue(this::remindChannel, throwable -> {});
                return;
            }

            Member member = guild.getMember(user);
            if(member == null) {
                user.openPrivateChannel().queue(this::remindChannel, throwable -> {});
                return;
            }

            if(!channel.canTalk() || !channel.canTalk(member)) {
                user.openPrivateChannel().queue(this::remindChannel, throwable -> {});
                return;
            }

            remindChannel(channel);
        }, time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    private void remindChannel(MessageChannel channel) {
        channel.sendMessage(message.replace("{@user}", "<@%d>".formatted(user)).replace("{reminder}", reminder)).queue();
    }

    public boolean cancel() {
        Database.getDatabase().reminders.deleteOne(Filters.and(
                Filters.eq("guild", guild),
                Filters.eq("user", this.user),
                Filters.eq("reminder", reminder),
                Filters.eq("message", message),
                Filters.eq("channel", channel),
                Filters.eq("time", time))
        );

        List<Runnable> failed = scheduler.shutdownNow();
        if(!failed.isEmpty()) {
            scheduler.shutdown();
        }

        return scheduler.isShutdown();
    }
}
