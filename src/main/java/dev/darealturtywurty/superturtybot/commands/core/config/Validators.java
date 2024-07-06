package dev.darealturtywurty.superturtybot.commands.core.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;
import java.util.function.BiPredicate;

public final class Validators {
    public static final BiPredicate<SlashCommandInteractionEvent, String> TEXT_CHANNEL_VALIDATOR = (event, str) -> {
        final Guild guild = event.getGuild();
        if (guild == null)
            return false;

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
            return !possibleMatches.isEmpty();
        }

        return true;
    };

    public static final BiPredicate<SlashCommandInteractionEvent, String> GUILD_CHANNEL_VALIDATOR = (event, str) -> {
        final Guild guild = event.getGuild();
        if (guild == null)
            return false;

        GuildChannel channel = guild.getChannelById(StandardGuildChannel.class, str);
        if (channel != null)
            return !channel.getType().isThread() && channel.getType().isGuild() && channel.getGuild().getIdLong() == guild.getIdLong();

        if (MentionType.CHANNEL.getPattern().matcher(str).matches()) {
            final String id = str.replace("<#", "").replace(">", "");
            channel = guild.getChannelById(StandardGuildChannel.class, id);
            return channel != null;
        }

        return false;
    };

    public static final BiPredicate<SlashCommandInteractionEvent, String> EMOJI_VALIDATOR = (event,
        str) -> Message.MentionType.EMOJI.getPattern().matcher(str).matches();

    public static final BiPredicate<SlashCommandInteractionEvent, String> ROLE_VALIDATOR = (event, str) -> {
        final Guild guild = event.getGuild();
        if(guild == null)
            return false;
        
        if (MentionType.ROLE.getPattern().matcher(str).matches()) {
            final String id = str.replace("<@&", "").replace(">", "");
            return guild.getRoleById(id) != null;
        }

        return guild.getRolesByName(str, false).isEmpty();
    };
    public static final BiPredicate<SlashCommandInteractionEvent, String> USER_VALIDATOR = (event, str) -> {
        JDA jda = event.getJDA();
        if (MentionType.USER.getPattern().matcher(str).matches()) {
            final String id = str.replace("<@!", "").replace(">", "");
            return jda.getUserById(id) != null;
        }

        return jda.getUsersByName(str, false).isEmpty();
    };
}
