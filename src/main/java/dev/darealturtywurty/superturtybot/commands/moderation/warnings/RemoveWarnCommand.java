package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public class RemoveWarnCommand extends CoreCommand {
    public RemoveWarnCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to remove a warn from", true),
            new OptionData(OptionType.STRING, "uuid", "The ID of the warn that you want to remove", true),
            new OptionData(OptionType.STRING, "reason", "The reason for the warn removal", false));
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
        return "Removes a warn from a user";
    }
    
    @Override
    public String getHowToUse() {
        return "/removewarn [user] [warnUUID]";
    }
    
    @Override
    public String getName() {
        return "removewarn";
    }
    
    @Override
    public String getRichName() {
        return "Remove Warning";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "This command can only be used inside of a server!", false, true);
            return;
        }
        
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            reply(event, "❌ You require the `Ban Members` permission to use this command!", false, true);
            return;
        }
        
        final User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        final String uuid = event.getOption("uuid", "Unspecified", OptionMapping::getAsString);
        final Warning warn = WarnManager.removeWarn(user, event.getGuild(), uuid, event.getUser());
        if(warn == null) {
            reply(event, "❌ That user does not have a warn with that UUID!", false,true);
            return;
        }

        final String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);

        user.openPrivateChannel().queue(channel -> channel.sendMessage("Your warning (`" + warn.getUuid() + "`) on `"
            + event.getGuild().getName() + "` has been removed with reason: `" + reason + "`!").queue(success -> {
            }, error -> {
            }));
        
        event.getJDA().retrieveUserById(warn.getWarner()).queue(warner -> {
            final var embed = new EmbedBuilder();
            embed.setColor(Color.GREEN);
            embed.setTitle(user.getName() + "'s warn has been removed!");
            embed.setDescription(
                "Warn Reason: " + warn.getReason() + "\nOriginal Warner: " + warner.getAsMention() + "\nWarned At: "
                    + TimeUtils.formatTime(Instant.ofEpochMilli(warn.getWarnedAt()).atOffset(ZoneOffset.UTC))
                    + "\nWarn UUID: " + warn.getUuid() + "\nRemoved By: " + event.getMember().getAsMention()
                    + "\nRemoval Reason: " + reason);
            reply(event, embed,false);

            final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
            if (Boolean.TRUE.equals(logging.getKey())) {
                BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has removed warn `"
                    + warn.getUuid() + "` from " + user.getAsMention() + " with reason: `" + reason + "`!", true);
            }
        });
    }
}
