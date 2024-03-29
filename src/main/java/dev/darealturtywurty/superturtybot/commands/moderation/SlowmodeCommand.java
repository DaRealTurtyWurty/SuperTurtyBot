package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.math3.util.Pair;

import java.util.List;

public class SlowmodeCommand extends CoreCommand {
    public SlowmodeCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.INTEGER, "time", "How long the cooldown is (in seconds)", false)
            .setRequiredRange(0, 21600).addChoice("None", 0).addChoice("5 seconds", 5).addChoice("10 seconds", 10)
            .addChoice("15 seconds", 15).addChoice("30 seconds", 30).addChoice("1 minute", 60)
            .addChoice("2 minutes", 120).addChoice("5 minutes", 300).addChoice("10 minutes", 600)
            .addChoice("15 minutes", 900).addChoice("30 minutes", 1800).addChoice("1 hour", 3600)
            .addChoice("2 hours", 7200).addChoice("6 hours", 21600));
    }

    @Override
    public String getAccess() {
        return "Moderators (Manage Channel Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Puts the current channel on slowmode";
    }

    @Override
    public String getHowToUse() {
        return "/slowmode\n/slowmode [time]";
    }

    @Override
    public String getName() {
        return "slowmode";
    }

    @Override
    public String getRichName() {
        return "Slowmode";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getChannelType() != ChannelType.TEXT || event.getMember() == null) {
            reply(event, "This command can only be used in channels that allow for slowmode!", false, true);
            return;
        }

        if(!event.getMember().hasPermission(event.getChannel().asTextChannel(), Permission.MANAGE_CHANNEL)) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return;
        }

        final int time = event.getOption("time", 5, OptionMapping::getAsInt);
        event.getChannel().asTextChannel().getManager().setSlowmode(time).queue();
        reply(event, "I have changed this channel's slowmode cooldown to " + time + " seconds!", false);
        final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
        if (Boolean.TRUE.equals(logging.getKey())) {
            BanCommand.logSlowmode(logging.getValue(),
                time <= 0
                    ? event.getMember().getAsMention() + " has removed the timeout from "
                        + event.getChannel().getAsMention() + "!"
                    : event.getMember().getAsMention() + " has put " + event.getChannel().getAsMention() + " on a "
                        + time + " second slowmode!",
                time);
        }
    }
}
