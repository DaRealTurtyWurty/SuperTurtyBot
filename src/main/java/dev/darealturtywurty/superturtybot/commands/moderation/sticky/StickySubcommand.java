package dev.darealturtywurty.superturtybot.commands.moderation.sticky;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

abstract class StickySubcommand extends SubcommandCommand {
    protected static final int MAX_STICKY_MESSAGE_LENGTH = 2000;

    protected StickySubcommand(String name, String description) {
        super(name, description);
    }

    protected static boolean validateManageMessages(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return false;
        }

        if (!event.getMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
            reply(event, "❌ You do not have permission to manage sticky messages!", false, true);
            return false;
        }

        return true;
    }

    protected static GuildMessageChannel requireChannel(SlashCommandInteractionEvent event) {
        GuildChannelUnion rawChannel = event.getOption("channel", OptionMapping::getAsChannel);
        if (rawChannel == null || !rawChannel.getType().isMessage()) {
            reply(event, "❌ You must provide a valid message channel!", false, true);
            return null;
        }

        return rawChannel.asGuildMessageChannel();
    }

    protected static OptionData channelOption() {
        return new OptionData(OptionType.CHANNEL, "channel", "The support channel to manage a sticky for", true);
    }
}
