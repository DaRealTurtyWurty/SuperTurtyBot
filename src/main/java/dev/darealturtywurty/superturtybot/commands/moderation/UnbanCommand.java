package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
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

public class UnbanCommand extends CoreCommand {
    public UnbanCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to unban!", true));
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
        return "Unbans a user";
    }
    
    @Override
    public String getHowToUse() {
        return "/unban [user]";
    }

    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public String getRichName() {
        return "Unban User";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        if (event.getMember().hasPermission(event.getGuildChannel(), Permission.BAN_MEMBERS)) {
            if (event.getGuild().getMember(user) == null) {
                user.openPrivateChannel().queue(channel -> channel
                    .sendMessage("You have been unbanned from `" + event.getGuild().getName() + "`!").queue(success -> {
                    }, error -> {
                    }));
                event.getGuild().unban(user).queue(success -> {
                    event.deferReply().setContent("Successfully unbanned " + user.getAsMention() + "!")
                        .mentionRepliedUser(false).queue();
                    final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
                    if (Boolean.TRUE.equals(logging.getKey())) {
                        BanCommand.log(logging.getValue(),
                            event.getMember().getAsMention() + " has unbanned " + user.getAsMention() + "!", true);
                    }
                }, error -> {
                    if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                        event.deferReply(true).setContent("I do not have permission to unban " + user.getAsMention())
                            .mentionRepliedUser(false).queue();
                    } else {
                        final var embed = new EmbedBuilder();
                        embed.setTitle("Please report this to TurtyWurty!", "https://discord.gg/BAYB3A38wn");
                        embed.setDescription("**" + error.getMessage() + "**\n" + ExceptionUtils.getStackTrace(error));
                        embed.setTimestamp(Instant.now());
                        embed.setColor(Color.red);
                        event.deferReply(true).addEmbeds(embed.build()).mentionRepliedUser(true).queue();
                    }
                });
            } else {
                event.deferReply(true).setContent("You do not have permission to unban " + user.getAsMention())
                    .mentionRepliedUser(false).queue();
            }
        }
    }
}