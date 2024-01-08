package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
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
import java.util.List;
import java.util.Set;

public class ClearWarningsCommand extends CoreCommand {
    public ClearWarningsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to clear warns from", true),
            new OptionData(OptionType.STRING, "reason", "The reason for clearing the warnings", false));
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
        return "Clears all warns from a user";
    }

    @Override
    public String getHowToUse() {
        return "/clearwarns [user]";
    }

    @Override
    public String getName() {
        return "clearwarns";
    }

    @Override
    public String getRichName() {
        return "Clear Warnings";
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
            reply(event, "You require the `Ban Members` permission to use this command!", false, true);
            return;
        }
        
        final String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);

        final User user = event.getOption("user", null, OptionMapping::getAsUser);
        if (user == null) {
            reply(event, "❌ You must supply a valid user!", false, true);
            return;
        }

        user.openPrivateChannel().queue(channel -> channel.sendMessage(
            "Your warnings on `" + event.getGuild().getName() + "` have been cleared, with reason: `" + reason + "`!")
            .queue(success -> {
            }, error -> {
            }));
        
        final Set<Warning> warns = WarnManager.clearWarnings(event.getGuild(), user, event.getUser());

        final var embed = new EmbedBuilder();
        embed.setColor(Color.GREEN);
        embed.setTitle(user.getName() + "'s warns has been cleared!");
        embed.setDescription("Warns Removed: " + warns.size() + "\nRemoved By: " + event.getMember().getAsMention()
            + "\nWith Reason: " + reason);
        reply(event, embed, false);

        final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
        if (Boolean.TRUE.equals(logging.getKey())) {
            BanCommand.log(logging.getValue(), event.getMember().getAsMention() + " has cleared " + user.getAsMention()
                + "'s warnings, with reason: `" + reason + "`!", true);
        }
    }
}
