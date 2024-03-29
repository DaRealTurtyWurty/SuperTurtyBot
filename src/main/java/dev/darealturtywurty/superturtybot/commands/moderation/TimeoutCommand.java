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
import java.util.concurrent.TimeUnit;

public class TimeoutCommand extends CoreCommand {
    public TimeoutCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "member", "The member to timeout!", true),
            new OptionData(OptionType.STRING, "duration", "How long to timeout the member (in minutes)", true));
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
        return "Timeouts a member";
    }
    
    @Override
    public String getHowToUse() {
        return "/timeout [member] [duration]";
    }
    
    @Override
    public String getName() {
        return "timeout";
    }
    
    @Override
    public String getRichName() {
        return "Timeout Member";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You can only timeout someone who is in this server!", false, true);
            return;
        }
        
        final Member member = event.getOption("member", event.getMember(), OptionMapping::getAsMember);
        if (member == null) {
            reply(event, "❌ You can only timeout someone who is in this server!", false, true);
            return;
        }
        
        if (event.getMember().hasPermission(event.getGuildChannel(), Permission.MODERATE_MEMBERS)
                && event.getMember().canInteract(member)) {
            final long duration = event.getOption("duration", 15L, OptionMapping::getAsLong);
            member.getUser().openPrivateChannel()
                .queue(channel -> channel.sendMessage(
                    "You have been put on timeout for " + duration + " minutes in `" + event.getGuild().getName() + "`!")
                    .queue(success -> {
                    }, error -> {
                    }));

            event.getGuild().timeoutFor(member, duration, TimeUnit.MINUTES).queue(success -> {
                reply(event, "✅ Successfully timed-out " + member.getAsMention() + "!", false);
                final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
                if (Boolean.TRUE.equals(logging.getKey())) {
                    BanCommand.log(logging.getValue(),
                        event.getMember().getAsMention() + " has timed-out " + member.getAsMention() + "!", false);
                }
            }, error -> {
                if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                    reply(event, "I do not have permission to timeout " + member.getAsMention(), false, true);
                } else {
                    final var embed = new EmbedBuilder();
                    embed.setTitle("Please report this to TurtyWurty#5690!", "https://discord.gg/d5cGhKQ");
                    embed.setDescription("**" + error.getMessage() + "**\n" + ExceptionUtils.getStackTrace(error));
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.RED);
                    reply(event, embed, true, true);
                }
            });
        } else {
            reply(event, "❌ You do not have permission to timeout " + member.getAsMention(), false, true);
        }
    }
}
