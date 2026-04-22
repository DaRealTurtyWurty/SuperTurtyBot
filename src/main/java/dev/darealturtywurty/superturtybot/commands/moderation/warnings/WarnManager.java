package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
        GuildData config = GuildData.getOrCreateGuildData(guild);

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
            Economy account = EconomyManager.getOrCreateAccount(guild, toWarn);

            BigInteger balance = EconomyManager.getBalance(account);

            // take economyPercentage% of balance
            BigInteger toTake = new BigDecimal(balance).divide(BigDecimal.valueOf(economyPercentage), RoundingMode.DOWN).toBigInteger();

            EconomyManager.removeMoney(account, toTake, true);
            account.addTransaction(toTake.negate(), MoneyTransaction.WARNING);
            EconomyManager.updateAccount(account);
        }

        final var warn = new Warning(guild.getIdLong(), toWarn.getIdLong(), reason, warner.getIdLong(), time,
                getWarningExpiresAt(config, time), UUID.randomUUID().toString());
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

        WarningSanctions.applyForWarningCount(guild, toWarn, warner.getUser(), getActiveWarnCount(guild, toWarn));

        return warn;
    }

    public static @NotNull Set<Warning> clearWarnings(@NotNull Guild guild, @NotNull User user, @NotNull User clearer) {
        final Set<Warning> warns = getWarns(guild, user);
        final int activeWarnings = countActiveWarnings(guild, warns);
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
        Database.getDatabase().warnings.deleteMany(filter);
        WarningSanctions.syncAfterWarningCountChange(guild, user, clearer, activeWarnings, 0);
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
        int previousWarnings = getActiveWarnCount(guild, toRemoveWarn);
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
                Filters.eq("user", toRemoveWarn.getIdLong()), Filters.eq("uuid", uuid));

        final Warning removed = Database.getDatabase().warnings.findOneAndDelete(filter);
        if (removed != null) {
            int currentWarnings = getActiveWarnCount(guild, toRemoveWarn);
            WarningSanctions.syncAfterWarningCountChange(guild, toRemoveWarn, remover, previousWarnings, currentWarnings);
        }

        return removed;
    }

    public static int getActiveWarnCount(@NotNull Guild guild, @NotNull User user) {
        return countActiveWarnings(guild, getWarns(guild, user));
    }

    public static List<Warning> getWarningsSorted(@NotNull Guild guild, @NotNull User user) {
        return getWarns(guild, user).stream()
                .sorted(Comparator.comparingLong(Warning::getWarnedAt))
                .toList();
    }

    public static boolean isWarningActive(@NotNull Guild guild, @NotNull Warning warning) {
        return isWarningActive(GuildData.getOrCreateGuildData(guild), warning, System.currentTimeMillis());
    }

    public static boolean isWarningActive(@NotNull GuildData config, @NotNull Warning warning, long now) {
        long expiresAt = getWarningExpiresAt(config, warning);
        return expiresAt <= 0 || expiresAt > now;
    }

    public static long getWarningExpiresAt(@NotNull Guild guild, @NotNull Warning warning) {
        return getWarningExpiresAt(GuildData.getOrCreateGuildData(guild), warning);
    }

    public static long getWarningExpiresAt(@NotNull GuildData config, @NotNull Warning warning) {
        if (warning.getExpiresAt() > 0) {
            return warning.getExpiresAt();
        }

        return getWarningExpiresAt(config, warning.getWarnedAt());
    }

    private static int countActiveWarnings(@NotNull Guild guild, @NotNull Set<Warning> warnings) {
        GuildData config = GuildData.getOrCreateGuildData(guild);
        long now = System.currentTimeMillis();
        int count = 0;
        for (Warning warning : warnings) {
            if (isWarningActive(config, warning, now)) {
                count++;
            }
        }

        return count;
    }

    private static long getWarningExpiresAt(@NotNull GuildData config, long warnedAt) {
        int expiryDays = Math.max(0, config.getWarningExpiryDays());
        if (expiryDays == 0) {
            return 0L;
        }

        return warnedAt + TimeUnit.DAYS.toMillis(expiryDays);
    }
}
