package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.warnings.WarningSanctionAction;
import dev.darealturtywurty.superturtybot.database.pojos.warnings.WarningSanctionConfig;
import dev.darealturtywurty.superturtybot.modules.TempBanManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class WarningSanctions {
    private WarningSanctions() {
    }

    public static void applyForWarningCount(@NotNull Guild guild, @NotNull User user, @NotNull User moderator,
                                            int warningCount) {
        if (warningCount <= 0)
            return;

        GuildData config = GuildData.getOrCreateGuildData(guild);
        List<WarningSanctionConfig> sanctions = config.getEffectiveWarningSanctions().stream()
                .filter(sanction -> sanction != null && sanction.getWarningCount() == warningCount)
                .sorted(Comparator
                        .comparingInt(WarningSanctionConfig::getWarningCount)
                        .thenComparing(WarningSanctionConfig::getType, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(WarningSanctionConfig::getId, Comparator.nullsLast(String::compareTo)))
                .toList();

        for (WarningSanctionConfig sanction : sanctions) {
            applySanction(guild, user, moderator, warningCount, sanction);
        }
    }

    public static void syncAfterWarningCountChange(@NotNull Guild guild, @NotNull User user, @NotNull User moderator,
                                                   int previousWarningCount, int currentWarningCount) {
        GuildData config = GuildData.getOrCreateGuildData(guild);
        WarningSanctionConfig previousBan = getHighestApplicable(config, WarningSanctionAction.BAN, previousWarningCount);
        WarningSanctionConfig currentBan = getHighestApplicable(config, WarningSanctionAction.BAN, currentWarningCount);
        WarningSanctionConfig previousTempBan = getHighestApplicable(config, WarningSanctionAction.TEMPBAN, previousWarningCount);
        WarningSanctionConfig currentTempBan = getHighestApplicable(config, WarningSanctionAction.TEMPBAN, currentWarningCount);

        if (currentBan != null) {
            TempBanManager.clearTempBan(guild.getIdLong(), user.getIdLong());
        } else if (currentTempBan != null) {
            if (previousBan != null || !sameTempBan(previousTempBan, currentTempBan))
                applyTempBan(guild, user, moderator, currentWarningCount, currentTempBan, false);
        } else if (previousBan != null) {
            revokeBan(guild, user, moderator);
        } else if (previousTempBan != null) {
            revokeTempBan(guild, user, moderator);
        }

        WarningSanctionConfig previousTimeout = getHighestApplicable(config, WarningSanctionAction.TIMEOUT, previousWarningCount);
        WarningSanctionConfig currentTimeout = getHighestApplicable(config, WarningSanctionAction.TIMEOUT, currentWarningCount);
        if (previousTimeout != null && currentTimeout == null) {
            revokeTimeout(guild, user, moderator);
        } else if (currentTimeout != null && !sameTimeout(previousTimeout, currentTimeout))
            applyTimeout(guild, user, moderator, currentWarningCount, currentTimeout, false);
    }

    private static @Nullable WarningSanctionConfig getHighestApplicable(GuildData config, WarningSanctionAction action,
                                                                        int warningCount) {
        if (warningCount <= 0)
            return null;

        return config.getEffectiveWarningSanctions().stream()
                .filter(sanction -> sanction != null && action == sanction.getAction() && sanction.getWarningCount() <= warningCount)
                .max(Comparator
                        .comparingInt(WarningSanctionConfig::getWarningCount)
                        .thenComparingLong(WarningSanctionConfig::getDurationMinutes)
                        .thenComparingInt(WarningSanctionConfig::getDeleteMessageDays))
                .orElse(null);
    }

    private static boolean sameTimeout(@Nullable WarningSanctionConfig left, @Nullable WarningSanctionConfig right) {
        if (left == right)
            return true;

        if (left == null || right == null)
            return false;

        return left.getWarningCount() == right.getWarningCount()
                && left.getDurationMinutes() == right.getDurationMinutes();
    }

    private static boolean sameTempBan(@Nullable WarningSanctionConfig left, @Nullable WarningSanctionConfig right) {
        if (left == right)
            return true;

        if (left == null || right == null)
            return false;

        return left.getWarningCount() == right.getWarningCount()
                && left.getDurationMinutes() == right.getDurationMinutes()
                && left.getDeleteMessageDays() == right.getDeleteMessageDays();
    }

    private static void applySanction(Guild guild, User user, User moderator, int warningCount,
                                      WarningSanctionConfig sanction) {
        WarningSanctionAction action = sanction.getAction();
        if (action == null)
            return;

        switch (action) {
            case TIMEOUT -> applyTimeout(guild, user, moderator, warningCount, sanction, true);
            case KICK -> applyKick(guild, user, moderator, warningCount, sanction);
            case TEMPBAN -> applyTempBan(guild, user, moderator, warningCount, sanction, true);
            case BAN -> applyBan(guild, user, moderator, warningCount, sanction);
        }
    }

    private static void applyTimeout(Guild guild, User user, User moderator, int warningCount,
                                     WarningSanctionConfig sanction, boolean announce) {
        if (sanction.getDurationMinutes() <= 0)
            return;

        Member target = guild.getMember(user);
        Member self = guild.getSelfMember();
        if (target == null || !self.canInteract(target)
                || !self.hasPermission(WarningSanctionAction.TIMEOUT.getRequiredPermissions())) {
            return;
        }

        String durationText = formatDuration(sanction.getDurationMinutes());
        String reason = createReason(warningCount);
        guild.timeoutFor(user, Duration.ofMinutes(sanction.getDurationMinutes()))
                .reason(reason)
                .queue(ignored -> {
                    if (announce) {
                        sendDm(user, "You have been put on timeout for " + durationText + " in `" + guild.getName()
                                + "` for reason: `" + reason + "`!");
                        log(guild, moderator.getAsMention() + " has timed-out " + user.getAsMention()
                                + " for " + durationText + " after reaching " + warningCount + " warnings!", false);
                    } else {
                        log(guild, moderator.getAsMention() + " has updated the timeout for " + user.getAsMention()
                                + " to " + durationText + " after their warnings changed to " + currentLabel(warningCount) + "!", false);
                    }
                }, ignored -> {
                });
    }

    private static void applyKick(Guild guild, User user, User moderator, int warningCount,
                                  WarningSanctionConfig sanction) {
        Member target = guild.getMember(user);
        Member self = guild.getSelfMember();
        if (target == null || !self.canInteract(target)
                || !self.hasPermission(WarningSanctionAction.KICK.getRequiredPermissions())) {
            return;
        }

        String reason = createReason(warningCount);
        sendDm(user, "You have been kicked from `" + guild.getName() + "` for reason: `" + reason + "`!");
        guild.kick(user)
                .reason(reason)
                .queue(ignored -> log(guild, moderator.getAsMention() + " has kicked " + user.getAsMention()
                        + " after reaching " + sanction.getWarningCount() + " warnings!", false), ignored -> {
                });
    }

    private static void applyBan(Guild guild, User user, User moderator, int warningCount,
                                 WarningSanctionConfig sanction) {
        Member self = guild.getSelfMember();
        Member target = guild.getMember(user);
        if ((target != null && !self.canInteract(target))
                || !self.hasPermission(WarningSanctionAction.BAN.getRequiredPermissions())) {
            return;
        }

        String reason = createReason(warningCount);
        sendDm(user, "You have been banned from `" + guild.getName() + "` for reason: `" + reason + "`!");
        guild.ban(user, sanction.getDeleteMessageDays(), TimeUnit.DAYS)
                .reason(reason)
                .queue(ignored -> {
                    TempBanManager.clearTempBan(guild.getIdLong(), user.getIdLong());
                    log(guild, moderator.getAsMention() + " has banned " + user.getAsMention()
                            + " after reaching " + sanction.getWarningCount() + " warnings!", false);
                },
                        ignored -> {
                        });
    }

    private static void applyTempBan(Guild guild, User user, User moderator, int warningCount,
                                     WarningSanctionConfig sanction, boolean announce) {
        if (sanction.getDurationMinutes() <= 0)
            return;

        Member self = guild.getSelfMember();
        Member target = guild.getMember(user);
        if ((target != null && !self.canInteract(target))
                || !self.hasPermission(WarningSanctionAction.TEMPBAN.getRequiredPermissions())) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(sanction.getDurationMinutes());
        String durationText = formatDuration(sanction.getDurationMinutes());
        String reason = createReason(warningCount);
        if (announce)
            sendDm(user, "You have been temporarily banned from `" + guild.getName() + "` for " + durationText
                    + " for reason: `" + reason + "`!");

        guild.ban(user, sanction.getDeleteMessageDays(), TimeUnit.DAYS)
                .reason(reason)
                .queue(ignored -> {
                    TempBanManager.createOrUpdateTempBan(guild.getIdLong(), user.getIdLong(), moderator.getIdLong(),
                            reason, sanction.getDeleteMessageDays(), expiresAt);
                    if (announce) {
                        log(guild, moderator.getAsMention() + " has temporarily banned " + user.getAsMention()
                                + " for " + durationText + " after reaching " + sanction.getWarningCount() + " warnings!", false);
                    } else {
                        log(guild, moderator.getAsMention() + " has updated the temporary ban for " + user.getAsMention()
                                + " to " + durationText + " after their warnings changed to " + currentLabel(warningCount) + "!", false);
                    }
                }, ignored -> {
                });
    }

    private static void revokeTimeout(Guild guild, User user, User moderator) {
        guild.removeTimeout(user).queue(success -> {
            sendDm(user, "Your timeout on `" + guild.getName() + "` has been removed!");
            log(guild, moderator.getAsMention() + " has removed the time-out from " + user.getAsMention() + "!", true);
        }, ignored -> {
        });
    }

    private static void revokeBan(Guild guild, User user, User moderator) {
        guild.unban(user).queue(success -> {
            sendDm(user, "You have been unbanned from `" + guild.getName() + "`!");
            log(guild, moderator.getAsMention() + " has unbanned " + user.getAsMention() + "!", true);
        }, ignored -> {
        });
    }

    private static void revokeTempBan(Guild guild, User user, User moderator) {
        guild.unban(user).queue(success -> {
            TempBanManager.clearTempBan(guild.getIdLong(), user.getIdLong());
            sendDm(user, "You have been unbanned from `" + guild.getName() + "`!");
            log(guild, moderator.getAsMention() + " has removed the temporary ban from " + user.getAsMention() + "!", true);
        }, ignored -> {
        });
    }

    private static void sendDm(User user, String message) {
        user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue(ignored -> {
        }, ignored -> {
        }), ignored -> {
        });
    }

    private static void log(Guild guild, String message, boolean positive) {
        Pair<Boolean, TextChannel> logging = BanCommand.canLog(guild);
        if (Boolean.TRUE.equals(logging.getKey()))
            BanCommand.log(logging.getValue(), message, positive);
    }

    private static String createReason(int warningCount) {
        return "Reached " + warningCount + " warnings!";
    }

    private static String currentLabel(int warningCount) {
        return warningCount == 1 ? "1 warning" : warningCount + " warnings";
    }

    private static String formatDuration(long durationMinutes) {
        if (durationMinutes % (24L * 60L) == 0L) {
            long days = durationMinutes / (24L * 60L);
            return days == 1L ? "1 day" : days + " days";
        }

        if (durationMinutes % 60L == 0L) {
            long hours = durationMinutes / 60L;
            return hours == 1L ? "1 hour" : hours + " hours";
        }

        return durationMinutes == 1L ? "1 minute" : durationMinutes + " minutes";
    }
}
