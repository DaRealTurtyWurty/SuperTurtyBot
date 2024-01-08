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

public class KickCommand extends CoreCommand {
    public KickCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "member", "The member to kick!", true),
            new OptionData(OptionType.STRING, "reason", "The kick reason", false));
    }
    
    @Override
    public String getAccess() {
        return "Moderators (Kick Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Kicks a member";
    }
    
    @Override
    public String getHowToUse() {
        return "/kick [member]\n/kick [member] [reason]";
    }
    
    @Override
    public String getName() {
        return "kick";
    }
    
    @Override
    public String getRichName() {
        return "Kick Member";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }
        
        final Member member = event.getOption("member", null, OptionMapping::getAsMember);
        if(member == null) {
            reply(event, "❌ You must specify a member to kick!", false, true);
            return;
        }

        if (event.getMember().hasPermission(event.getGuildChannel(), Permission.KICK_MEMBERS)) {
            String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
            if (reason.length() > 512) {
                reason = reason.substring(0, 512);
            }
            
            final String finalReason = reason;
            member.getUser().openPrivateChannel()
                .queue(channel -> channel.sendMessage(
                    "You have been kicked from `" + event.getGuild().getName() + "` for reason: `" + finalReason + "`!")
                    .queue(success -> {
                    }, error -> {
                    }));
            
            event.getGuild().kick(member).reason(finalReason).queue(success -> {
                reply(event, "Successfully kicked " + member.getAsMention() + "!", false);
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has kicked "
                        + member.getAsMention() + " for reason: `" + finalReason + "`!", false);
                }
            }, error -> {
                if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                    reply(event, "I do not have permission to kick " + member.getAsMention(), false, true);
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
            reply(event, "You do not have permission to kick " + member.getAsMention(), false, true);
        }
    }
}
