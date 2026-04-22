package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.commands.util.remindme.RemindMeSubcommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.modules.TempBanManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TempBanCommand extends CoreCommand {
    public TempBanCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "The user to temporarily ban!", true),
                new OptionData(OptionType.STRING, "duration", "How long to ban the user for (e.g. 30m, 2h, 7d)", true),
                new OptionData(OptionType.INTEGER, "delete_days", "Number of days to delete this user's messages", false)
                        .setRequiredRange(0, 7),
                new OptionData(OptionType.STRING, "reason", "The ban reason", false));
    }

    @Override
    public String getAccess() {
        return "Moderators (Ban Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Temporarily bans a user";
    }

    @Override
    public String getHowToUse() {
        return "/tempban [user] [duration]\n/tempban [user] [duration] [deleteDays]\n/tempban [user] [duration] [reason]\n/tempban [user] [duration] [deleteDays] [reason]";
    }

    @Override
    public String getName() {
        return "tempban";
    }

    @Override
    public String getRichName() {
        return "Temporary Ban User";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final User user = event.getOption("user", OptionMapping::getAsUser);
        if (user == null) {
            reply(event, "❌ You must provide a user to temporarily ban!", false, true);
            return;
        }

        if (user.getIdLong() == event.getUser().getIdLong()) {
            reply(event, "❌ You cannot temporarily ban yourself!", false, true);
            return;
        }

        if (!event.getMember().hasPermission(event.getGuildChannel(), Permission.BAN_MEMBERS)) {
            reply(event, "❌ You do not have permission to temporarily ban " + user.getAsMention(), false, true);
            return;
        }

        final Member targetMember = event.getGuild().getMember(user);
        if (targetMember != null && !event.getMember().canInteract(targetMember)) {
            reply(event, "❌ You do not have permission to temporarily ban " + user.getAsMention(), false, true);
            return;
        }

        final String durationInput = event.getOption("duration", "", OptionMapping::getAsString);
        final long durationMillis;
        try {
            durationMillis = RemindMeSubcommand.parseDurationMillis(durationInput);
        } catch (IllegalArgumentException exception) {
            reply(event, "❌ " + exception.getMessage(), false, true);
            return;
        }

        final int deleteDays = event.getOption("delete_days", 0, OptionMapping::getAsInt);
        String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
        if (reason.length() > 512) {
            reason = reason.substring(0, 512);
        }

        final long expiresAt = System.currentTimeMillis() + durationMillis;
        final String finalReason = reason;
        user.openPrivateChannel().queue(
                channel -> channel.sendMessage("You have been temporarily banned from `" + event.getGuild().getName()
                        + "` until " + TimeFormat.RELATIVE.format(expiresAt) + " for reason: `" + finalReason + "`!")
                        .queue(
                                ignored -> {
                                },
                                ignored -> {
                                }),
                ignored -> {
                });

        event.getGuild().ban(user, deleteDays, TimeUnit.DAYS).reason(finalReason).queue(
                success -> {
                    TempBanManager.createOrUpdateTempBan(event.getGuild().getIdLong(), user.getIdLong(),
                            event.getMember().getIdLong(), finalReason, deleteDays, expiresAt);
                    reply(event,
                            "✅ Successfully temporarily banned " + user.getAsMention() + " until "
                                    + TimeFormat.RELATIVE.format(expiresAt) + "!",
                            false);
                    final var logging = BanCommand.canLog(event.getGuild());
                    if (Boolean.TRUE.equals(logging.getKey())) {
                        BanCommand.log(logging.getValue(),
                                event.getMember().getAsMention() + " has temporarily banned " + user.getAsMention()
                                        + " until " + TimeFormat.RELATIVE.format(expiresAt) + " for reason: `"
                                        + finalReason + "`!",
                                false);
                    }
                },
                error -> {
                    if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                        reply(event, "I do not have permission to ban " + user.getAsMention(), false, true);
                    } else {
                        reply(event, "❌ Failed to temporarily ban " + user.getAsMention() + ": " + error.getMessage(),
                                false, true);
                    }
                });
    }
}
