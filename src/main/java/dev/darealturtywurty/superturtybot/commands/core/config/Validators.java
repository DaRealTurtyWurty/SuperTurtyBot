package dev.darealturtywurty.superturtybot.commands.core.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiPredicate;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class Validators {
    public static final BiPredicate<SlashCommandInteractionEvent, String> TEXT_CHANNEL_VALIDATOR = (event, str) -> {
        final Guild guild = event.getGuild();
        TextChannel channel = guild.getTextChannelById(str);
        if (channel != null)
            return true;

        if (MentionType.CHANNEL.getPattern().matcher(str).matches()) {
            final String id = str.replace("<#", "").replace(">", "");
            channel = guild.getTextChannelById(id);
            return channel != null;
        }

        List<TextChannel> possibleMatches = guild.getTextChannelsByName(str, false);
        if (possibleMatches.isEmpty()) {
            possibleMatches = guild.getTextChannelsByName(str, true);
            if (possibleMatches.isEmpty())
                return false;
        }

        return true;
    };

    public static final BiPredicate<SlashCommandInteractionEvent, String> GUILD_CHANNEL_VALIDATOR = (event, str) -> {
        final Guild guild = event.getGuild();
        GuildChannel channel = guild.getChannelById(StandardGuildChannel.class, str);
        if (channel != null) {
            if (channel.getType().isThread() || !channel.getType().isGuild() || channel.getGuild().getIdLong() != guild.getIdLong())
                return false;

            return true;
        }

        if (MentionType.CHANNEL.getPattern().matcher(str).matches()) {
            final String id = str.replace("<#", "").replace(">", "");
            channel = guild.getChannelById(StandardGuildChannel.class, id);
            return channel != null;
        }

        return false;
    };

    public static final BiPredicate<SlashCommandInteractionEvent, String> EMOJI_VALIDATOR = (event,
        str) -> Message.MentionType.EMOJI.getPattern().matcher(str).matches();
}