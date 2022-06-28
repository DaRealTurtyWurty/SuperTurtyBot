package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Timeouts a member";
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild())
            return;

        final Member member = event.getOption("member").getAsMember();
        if (member == null) {
            event.deferReply(true).setContent("You can only timeout someone who is in this server!")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (event.getInteraction().getMember().hasPermission(event.getGuildChannel(), Permission.MODERATE_MEMBERS)
            && event.getInteraction().getMember().canInteract(member)) {
            final long duration = event.getOption("duration", 15L, OptionMapping::getAsLong);
            event.getGuild().timeoutFor(member, duration, TimeUnit.MINUTES).queue(v -> event.deferReply()
                .setContent("Successfully timed-out " + member.getAsMention() + "!").mentionRepliedUser(false).queue(),
                error -> {
                    if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                        event.deferReply(true)
                            .setContent("I do not have permission to timeout " + member.getAsMention())
                            .mentionRepliedUser(false).queue();
                    } else {
                        final var embed = new EmbedBuilder();
                        embed.setTitle("Please report this to TurtyWurty#5690!", "https://discord.gg/d5cGhKQ");
                        embed.setDescription("**" + error.getMessage() + "**\n" + ExceptionUtils.getStackTrace(error));
                        embed.setTimestamp(Instant.now());
                        embed.setColor(Color.red);
                        event.deferReply(true).addEmbeds(embed.build()).mentionRepliedUser(true).queue();
                    }
                });
        } else {
            event.deferReply(true).setContent("You do not have permission to timeout " + member.getAsMention())
                .mentionRepliedUser(false).queue();
        }
    }
}
