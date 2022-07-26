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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
        if (!event.isFromGuild())
            return;

        final Member member = event.getOption("member").getAsMember();
        if (member == null) {
            event.deferReply(true).setContent("You can only remove a timeout from someone who is in this server!")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (event.getInteraction().getMember().hasPermission(event.getGuildChannel(), Permission.MODERATE_MEMBERS)
            && event.getInteraction().getMember().canInteract(member)) {
            event.getGuild().removeTimeout(member)
                .queue(v -> event.deferReply()
                    .setContent("Successfully removed timeout from " + member.getAsMention() + "!")
                    .mentionRepliedUser(false).queue(), error -> {
                        if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                            event.deferReply(true)
                                .setContent(
                                    "I do not have permission to remove a timeout from " + member.getAsMention())
                                .mentionRepliedUser(false).queue();
                        } else {
                            final var embed = new EmbedBuilder();
                            embed.setTitle("Please report this to TurtyWurty#5690!", "https://discord.gg/d5cGhKQ");
                            embed.setDescription(
                                "**" + error.getMessage() + "**\n" + ExceptionUtils.getStackTrace(error));
                            embed.setTimestamp(Instant.now());
                            embed.setColor(Color.red);
                            event.deferReply(true).addEmbeds(embed.build()).mentionRepliedUser(true).queue();
                        }
                    });
        } else {
            event.deferReply(true)
                .setContent("You do not have permission to remove the timeout from " + member.getAsMention())
                .mentionRepliedUser(false).queue();
        }
    }
}
