package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.commons.math3.util.Pair;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WarnManager {
    private WarnManager() {
    }

    public static @NotNull Warning addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
                                           @NotNull String reason) {
        return addWarn(toWarn, guild, warner, reason, System.currentTimeMillis());
    }

    public static @NotNull Warning addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
                                           @NotNull String reason, long time) {
        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        float xpPercentage = config.getWarningXpPercentage();
        float economyPercentage = config.getWarningEconomyPercentage();

        if (xpPercentage > 0 && LevellingManager.INSTANCE.areLevelsEnabled(guild)) {
            Levelling profile = Database.getDatabase().levelling.find(Filters.and(Filters.eq("guild", guild.getIdLong()),
                    Filters.eq("user", toWarn.getIdLong()))).first();
            if (profile != null) {
                int xp = profile.getXp();

                // take xpPercentage% of xp
                int toTake = (int) (xp / xpPercentage);

                LevellingManager.INSTANCE.removeXP(guild, toWarn, toTake);
            }
        }

        if (economyPercentage > 0 && config.isEconomyEnabled()) {
            Economy account = EconomyManager.getAccount(guild, toWarn);

            int balance = EconomyManager.getBalance(account);

            // take economyPercentage% of balance
            int toTake = (int) (balance / economyPercentage);

            EconomyManager.removeMoney(account, toTake, true);
            EconomyManager.updateAccount(account);
        }

        final var warn = new Warning(guild.getIdLong(), toWarn.getIdLong(), reason, warner.getIdLong(), time,
                UUID.randomUUID().toString());
        Database.getDatabase().warnings.insertOne(warn);

        Member toWarnMember = guild.getMember(toWarn);
        if(toWarnMember == null)
            return warn;

        Member selfMember = guild.getSelfMember();
        if(!selfMember.canInteract(toWarnMember))
            return warn;

        // check that self has perms to add sanctions
        if(!selfMember.hasPermission(Permission.MANAGE_ROLES, Permission.BAN_MEMBERS, Permission.KICK_MEMBERS))
            return warn;

        addSanctions(guild, toWarn, warner.getUser());

        return warn;
    }

    public static @NotNull Set<Warning> clearWarnings(@NotNull Guild guild, @NotNull User user, @NotNull User clearer) {
        final Set<Warning> warns = getWarns(guild, user);
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
        Database.getDatabase().warnings.deleteMany(filter);
        clearSanctions(warns, guild, user, clearer);
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

    // TODO: Don't hardcode (will require the web dashboard to be created first)
    protected static void addSanctions(Guild guild, User user, User warner) {
        final Set<Warning> warnings = getWarns(guild, user);
        if (user.getIdLong() != guild.getSelfMember().getIdLong()) {
            guild.timeoutFor(user, Duration.ofHours(warnings.size() * 2L))
                    .queue(ignored -> {}, ignored -> {});
            user.openPrivateChannel().queue(channel ->
                    channel.sendMessage("You have been put on timeout for " + warnings.size() * 2 + " hours in `" + guild.getName() + "`!")
                            .queue(ignored -> {}, ignored -> {}),
                    ignored -> {});
        }

        final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
        if (Boolean.TRUE.equals(logging.getKey())) {
            BanCommand.log(logging.getValue(), warner.getAsMention() + " has timed-out " + user.getAsMention() + "!",
                    false);
        }

        if (warnings.size() == 3) {
            final String kickReason = "Reached 3 warnings!";
            user.openPrivateChannel().queue(channel ->
                    channel.sendMessage("You have been kicked from `" + guild.getName() + "` for reason: `" + kickReason + "`!")
                            .queue(ignored -> {}, ignored -> {}));
            guild.kick(user).reason(kickReason).queue(ignored -> {}, ignored -> {});
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                        warner.getAsMention() + " has banned " + user.getAsMention() + " for reason: `" + kickReason + "`!",
                        false);
            }
        } else if (warnings.size() >= 5) {
            final String banReason = "Reached 5 warnings!";
            user.openPrivateChannel().queue(channel ->
                    channel.sendMessage("You have been banned from `" + guild.getName() + "` for reason: `" + banReason + "`!")
                            .queue(ignored -> {}, ignored -> {}));
            guild.ban(user, 0, TimeUnit.DAYS).reason(banReason).queue(ignored -> {}, ignored -> {});
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(),
                        warner.getAsMention() + " has banned " + user.getAsMention() + " for reason: `" + banReason + "`!",
                        false);
            }
        }
    }

    protected static void clearSanctions(Set<Warning> warns, Guild guild, User user, User clearer) {
        if (warns.size() >= 5) {
            guild.unban(user).queue(success -> {
                user.openPrivateChannel()
                    .queue(channel ->
                            channel.sendMessage("You have been unbanned from `" + guild.getName() + "`!")
                                    .queue(ignored -> {}, ignored -> {}),
                            ignored -> {});
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(),
                            clearer.getAsMention() + " has unbanned " + user.getAsMention() + "!", true);
                }
            }, ignored -> {});

        } else {
            guild.removeTimeout(user).queue(success -> {
                user.openPrivateChannel()
                    .queue(channel ->
                            channel.sendMessage("Your timeout on `" + guild.getName() + "` has been removed!")
                                    .queue(ignored -> {}, ignored -> {}),
                            ignored -> {});
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(),
                            clearer.getAsMention() + " has removed the time-out from " + user.getAsMention() + "!",
                            true);
                }
            }, ignored -> {});
        }
    }

    protected static void removeSanctions(Guild guild, User user, User remover) {
        final Set<Warning> warnings = getWarns(guild, user);
        if (warnings.size() == 4) {
            guild.unban(user).queue(success -> {
                user.openPrivateChannel()
                    .queue(channel ->
                            channel.sendMessage("You have been unbanned from `" + guild.getName() + "`!")
                                    .queue(ignored -> {}, ignored -> {}),
                            ignored -> {});
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(),
                            remover.getAsMention() + " has unbanned " + user.getAsMention() + "!", true);
                }
            }, ignored -> {});

        } else if (warnings.size() == 3 || warnings.size() == 1 || warnings.isEmpty()) {
            guild.removeTimeout(user).queue(success -> {
                user.openPrivateChannel()
                    .queue(channel ->
                            channel.sendMessage("Your timeout on `" + guild.getName() + "` has been removed!")
                                    .queue(ignored -> {}, ignored -> {}),
                            ignored -> {});
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(),
                            remover.getAsMention() + " has removed the time-out from " + user.getAsMention() + "!",
                            true);
                }
            }, ignored -> {});
        }
    }
}
