package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class RemoveTimeoutCommand extends CoreCommand {
    public RemoveTimeoutCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "member", "The member to remove the timeout from!", true));
    }

    @Override
    public String getAccess() {
        return "Moderators (Manage Members Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Removes a timeout from a member";
    }

    @Override
    public String getHowToUse() {
        return "/removetimeout [member]";
    }

    @Override
    public String getName() {
        return "removetimeout";
    }

    @Override
    public String getRichName() {
        return "Remove Timeout";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You can only remove a timeout from someone who is in this server!", false, true);
            return;
        }

        final Member member = event.getOption("member", event.getMember(), OptionMapping::getAsMember);
        if (member == null) {
            reply(event, "❌ You can only remove a timeout from someone who is in this server!", false, true);
            return;
        }

        if (event.getMember().hasPermission(event.getGuildChannel(), Permission.MODERATE_MEMBERS) && event.getMember().canInteract(member)) {
            member.getUser().openPrivateChannel().queue(
                    channel -> channel.sendMessage("✅ Your timeout on `" + event.getGuild().getName() + "` has been removed!").queue(
                            success -> {}, error -> {}));

            event.getGuild().removeTimeout(member).queue(success -> {
                reply(event, "✅ Successfully removed timeout from " + member.getAsMention() + "!", false);
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has removed the time-out from " + member.getAsMention() + "!", true);
                }
            }, error -> {
                if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                    reply(event, "❌ I do not have permission to remove a timeout from " + member.getAsMention(), false, true);
                } else {
                    final var embed = new EmbedBuilder();
                    embed.setTitle("Please report this to TurtyWurty#5690!", "https://discord.gg/d5cGhKQ");
                    embed.setDescription("**" + error.getMessage() + "**\n" + ExceptionUtils.getStackTrace(error));
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.red);
                    reply(event, embed, true);
                }
            });
        } else {
            reply(event, "❌ You do not have permission to remove the timeout from " + member.getAsMention(), false, true);
        }
    }
}
