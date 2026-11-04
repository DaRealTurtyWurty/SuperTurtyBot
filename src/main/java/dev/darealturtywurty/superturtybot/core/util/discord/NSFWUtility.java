package dev.darealturtywurty.superturtybot.core.util.discord;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public final class NSFWUtility {
    private NSFWUtility() {
    }

    public static boolean isValidChannel(MessageChannelUnion channel) {
        if (channel == null)
            return false;

        if (channel.getType() == ChannelType.UNKNOWN)
            return false;

        // if it's a private channel
        // TODO: Add a check for if the user is 18+ (and utilise the user config too)
        if (channel.getType() == ChannelType.PRIVATE)
            return true;

        // if it's a text channel and is nsfw
        if (channel.getType() == ChannelType.TEXT && channel.asTextChannel().isNSFW())
            return true;

        // if it's a thread channel and the parent is nsfw
        if ((channel.getType() == ChannelType.GUILD_PUBLIC_THREAD
            || channel.getType() == ChannelType.GUILD_PRIVATE_THREAD
            || channel.getType() == ChannelType.GUILD_NEWS_THREAD
                && channel.asThreadChannel().getParentMessageChannel().asTextChannel().isNSFW()))
            return true;

        // if it's a private channel
        return channel.getType() == ChannelType.PRIVATE;
    }
}
