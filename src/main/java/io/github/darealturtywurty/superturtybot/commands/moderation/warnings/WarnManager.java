package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class WarnManager {
    private WarnManager() {
    }
    
    public static @NotNull Warning addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
        @NotNull String reason) {
        return addWarn(toWarn, guild, warner, reason, System.currentTimeMillis());
    }
    
    public static @NotNull Warning addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
        @NotNull String reason, long time) {
        final var warn = new Warning(guild.getIdLong(), toWarn.getIdLong(), reason, warner.getIdLong());
        Database.getDatabase().warnings.insertOne(warn);
        addSanctions(guild, toWarn, warner.getUser());
        return warn;
    }
    
    public static @NotNull Set<Warning> clearWarnings(@NotNull Guild guild, @NotNull User user) {
        final Set<Warning> warns = getWarns(guild, user);
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
        Database.getDatabase().warnings.deleteMany(filter);
        return warns;
    }
    
    public static @NotNull Set<Warning> getWarns(@NotNull Guild guild, @NotNull User user) {
        final Set<Warning> warnings = new HashSet<>();
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
        Database.getDatabase().warnings.find(filter).forEach(warnings::add);
        return warnings;
    }
    
    public static @Nullable Warning removeWarn(@NotNull User toRemoveWarn, @NotNull Guild guild, @NotNull String uuid,
        @NotNull User remover) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("user", toRemoveWarn.getIdLong()), Filters.eq("uuid", uuid));
        
        final Warning removed = Database.getDatabase().warnings.findOneAndDelete(filter);
        removeSanctions(guild, toRemoveWarn, remover);
        return removed;
    }

    // TODO: Don't hardcode
    protected static void addSanctions(Guild guild, User user, User warner) {
        final Set<Warning> warnings = getWarns(guild, user);
        if (warnings.size() == 1 || warnings.size() == 2 || warnings.size() == 4) {
            user.openPrivateChannel()
                .queue(channel -> channel.sendMessage(
                    "You have been put on timeout for " + warnings.size() * 2 + " hours in `" + guild.getName() + "`!")
                    .queue(success -> {
                    }, error -> {
                    }));
            guild.timeoutFor(user, Duration.ofHours(warnings.size() * 2)).queue();
            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                    warner.getAsMention() + " has timed-out " + user.getAsMention() + "!", false);
            }
        } else if (warnings.size() == 3) {
            final String kickReason = "Reached 3 warnings!";
            user.openPrivateChannel()
                .queue(channel -> channel
                    .sendMessage(
                        "You have been kicked from `" + guild.getName() + "` for reason: `" + kickReason + "`!")
                    .queue(success -> {
                    }, error -> {
                    }));
            guild.kick(user, kickReason).queue();
            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                    warner.getAsMention() + " has banned " + user.getAsMention() + " for reason: `" + kickReason + "`!",
                    false);
            }
        } else if (warnings.size() >= 5) {
            final String banReason = "Reached 5 warnings!";
            user.openPrivateChannel()
                .queue(channel -> channel
                    .sendMessage("You have been banned from `" + guild.getName() + "` for reason: `" + banReason + "`!")
                    .queue(success -> {
                    }, error -> {
                    }));
            guild.ban(user, 0, banReason).queue();
            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                    warner.getAsMention() + " has banned " + user.getAsMention() + " for reason: `" + banReason + "`!",
                    false);
            }
        }
    }

    protected static void removeSanctions(Guild guild, User user, User remover) {
        final Set<Warning> warnings = getWarns(guild, user);
        if (warnings.size() == 4) {
            user.openPrivateChannel().queue(channel -> channel
                .sendMessage("You have been unbanned from `" + guild.getName() + "`!").queue(success -> {
                }, error -> {
                }));
            guild.unban(user).queue();
            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                    remover.getAsMention() + " has unbanned " + user.getAsMention() + "!", true);
            }
        } else if (warnings.size() == 3 || warnings.size() == 1 || warnings.isEmpty()) {
            remover.openPrivateChannel().queue(channel -> channel
                .sendMessage("Your timeout on `" + guild.getName() + "` has been removed!").queue(success -> {
                }, error -> {
                }));
            guild.removeTimeout(user).queue();
            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                    remover.getAsMention() + " has removed the time-out from " + user.getAsMention() + "!", true);
            }
        }
    }
}
