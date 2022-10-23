package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class WarnCommand extends CoreCommand {
    public WarnCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to warn", true),
            new OptionData(OptionType.STRING, "reason", "The reason for why you want to warn that user", false));
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
        return "Warns a user";
    }

    @Override
    public String getHowToUse() {
        return "/warn [user]\n/warn [user] [reason]";
    }

    @Override
    public String getName() {
        return "warn";
    }

    @Override
    public String getRichName() {
        return "Add Warning";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("This command can only be used inside of a server!")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.deferReply(true).setContent("You require the `Ban Members` permission to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final User user = event.getOption("user").getAsUser();
        final String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
        
        user.openPrivateChannel()
            .queue(channel -> channel
                .sendMessage("You have been warned on `" + event.getGuild().getName() + "` for `" + reason + "`!")
                .queue(success -> {
                }, error -> {
                }));
        
        final Warning warn = WarnManager.addWarn(user, event.getGuild(), event.getMember(), reason);

        event.getJDA().retrieveUserById(warn.getWarner()).queue(warner -> {
            final var embed = new EmbedBuilder();
            embed.setTitle(user.getName() + " has been warned!");
            embed.setFooter("Warned At: "
                + StringUtils.formatTime(Instant.ofEpochMilli(warn.getWarnedAt()).atOffset(ZoneOffset.UTC)));
            embed.setDescription(
                "Reason: " + warn.getReason() + "\nWarned By: " + warner.getAsMention() + "\nUUID: " + warn.getUuid());
            embed.setColor(Color.RED);
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();

            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has warned "
                    + user.getAsMention() + " for reason: `" + reason + "`!", false);
            }
        });
    }
}
