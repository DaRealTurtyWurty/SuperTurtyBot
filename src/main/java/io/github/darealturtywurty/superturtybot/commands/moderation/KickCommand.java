package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

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
        if (!event.isFromGuild())
            return;

        final Member member = event.getOption("member").getAsMember();
        if (event.getInteraction().getMember().hasPermission(event.getGuildChannel(), Permission.KICK_MEMBERS)
            && member != null) {
            String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
            if (reason.length() > 512) {
                reason = reason.substring(0, 512);
                // TODO: Confirmation of whether they still want to kick
            }

            event.getGuild().kick(member, reason).queue(v -> event.deferReply()
                .setContent("Successfully kicked " + member.getAsMention() + "!").mentionRepliedUser(false).queue(),
                error -> {
                    if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                        event.deferReply(true).setContent("I do not have permission to kick " + member.getAsMention())
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
            event.deferReply(true).setContent("You do not have permission to kick " + member.getAsMention())
                .mentionRepliedUser(false).queue();
        }
    }
}
