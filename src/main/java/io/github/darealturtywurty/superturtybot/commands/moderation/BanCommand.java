package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class BanCommand extends CoreCommand {
    public BanCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to ban!", true),
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
        return "Bans a user";
    }
    
    @Override
    public String getHowToUse() {
        return "/ban [user]\n/ban [user] [deleteDays]\n/ban [user] [reason]\n/ban [user] [deleteDays] [reason]\n/ban [user] [reason] [deleteDays]";
    }
    
    @Override
    public String getName() {
        return "ban";
    }
    
    @Override
    public String getRichName() {
        return "Ban User";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild())
            return;
        
        final User user = event.getOption("user").getAsUser();
        if (event.getInteraction().getMember().hasPermission(event.getGuildChannel(), Permission.BAN_MEMBERS)) {
            boolean canInteract = true;
            if (event.getOption("user").getAsMember() != null
                && !event.getInteraction().getMember().canInteract(event.getOption("user").getAsMember())) {
                canInteract = false;
            }
            
            if (canInteract) {
                final int deleteDays = event.getOption("delete_days", 0, OptionMapping::getAsInt);
                String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
                if (reason.length() > 512) {
                    reason = reason.substring(0, 512);
                    // TODO: Confirmation of whether they still want to ban
                }
                
                event.getGuild().ban(user, deleteDays, reason).queue(v -> event.deferReply()
                    .setContent("Successfully banned " + user.getAsMention() + "!").mentionRepliedUser(false).queue(),
                    error -> {
                        if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                            event.deferReply(true).setContent("I do not have permission to ban " + user.getAsMention())
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
                event.deferReply(true).setContent("You do not have permission to ban " + user.getAsMention())
                    .mentionRepliedUser(false).queue();
            }
        }
    }
}
