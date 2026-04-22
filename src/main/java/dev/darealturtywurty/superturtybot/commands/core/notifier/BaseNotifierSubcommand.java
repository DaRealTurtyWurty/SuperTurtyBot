package dev.darealturtywurty.superturtybot.commands.core.notifier;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public abstract class BaseNotifierSubcommand extends SubcommandCommand {
    protected BaseNotifierSubcommand(String name, String description) {
        super(name, description);
    }

    protected static boolean validateManageServer(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (!event.isFromGuild() || guild == null || member == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return false;
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return false;
        }

        return true;
    }

    protected static ChannelMentionContext requireChannelAndMention(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return null;
        }

        GuildChannelUnion rawChannel = event.getOption("discord_channel", OptionMapping::getAsChannel);
        if (rawChannel == null) {
            reply(event, "❌ You must provide a Discord channel!", false, true);
            return null;
        }

        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class,
                rawChannel.getIdLong());
        if (channel == null) {
            reply(event, "❌ You must choose a text or announcement channel!", false, true);
            return null;
        }

        if (!channel.canTalk() || !guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            reply(event,
                    "❌ I need permission to send messages and embeds in that channel before this notifier can work.",
                    false, true);
            return null;
        }

        IMentionable rawMention = event.getOption("who_to_ping", OptionMapping::getAsMentionable);
        if (rawMention == null) {
            reply(event, "❌ You must provide someone to ping!", false, true);
            return null;
        }

        return new ChannelMentionContext(channel.getIdLong(), rawMention.getAsMention());
    }

    protected static OptionData discordChannelOption() {
        return new OptionData(OptionType.CHANNEL, "discord_channel", "The channel to send notifications to", true);
    }

    protected static OptionData mentionOption() {
        return new OptionData(OptionType.MENTIONABLE, "who_to_ping",
                "Who should be pinged when a notification happens", true);
    }

    protected static OptionData unsubscribeOption() {
        return new OptionData(OptionType.BOOLEAN, "unsubscribe", "Whether to unsubscribe this notifier", false);
    }
}
