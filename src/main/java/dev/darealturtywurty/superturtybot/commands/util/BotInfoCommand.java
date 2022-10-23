package dev.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.time.Instant;

import org.jetbrains.annotations.Nullable;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class BotInfoCommand extends CoreCommand {
    public BotInfoCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Retrieves information about me.";
    }

    @Override
    public String getName() {
        return "botinfo";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final var embed = createEmbed(event.getJDA(), event.isFromGuild() ? event.getGuild() : null);
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }

    private EmbedBuilder createEmbed(JDA jda, @Nullable Guild guild) {
        final boolean fromGuild = guild != null;
        final var embed = new EmbedBuilder();
        embed.setColor(fromGuild ? guild.getSelfMember().getColor() : Color.CYAN);
        embed.setTimestamp(Instant.now());
        embed.setTitle("Bot Information");
        embed.addField("Name", fromGuild ? guild.getSelfMember().getEffectiveName() : jda.getSelfUser().getName(),
            false);
        embed.addField("Created On", StringUtils.formatTime(jda.getSelfUser().getTimeCreated()), false);
        if (fromGuild) {
            embed.addField("Joined At", StringUtils.formatTime(guild.getSelfMember().getTimeJoined()), false);
        }

        embed.addField("", "**__Counts__**:", false);
        embed.addField("Members", jda.getUserCache().size() + "", true);
        embed.addField("Roles", jda.getRoleCache().size() + "", true);
        embed.addField("Emojis", jda.getEmojiCache().size() + "", true);
        embed.addField("Servers", jda.getGuildCache().size() + "", true);
        embed.addField("Categories", jda.getCategoryCache().size() + "", true);
        embed.addField("Channels", jda.getVoiceChannelCache().size() + jda.getTextChannelCache().size()
            + jda.getThreadChannelCache().size() + "", true);

        embed.addField("", "**__Commands__**:", false);
        embed.addField("Slash",
            CommandHook.INSTANCE.getCommands().stream().filter(cmd -> cmd.types.slash()).count() + "", true);
        embed.addField("Prefix",
            CommandHook.INSTANCE.getCommands().stream().filter(cmd -> cmd.types.normal()).count() + "", true);
        embed.addField("Context", CommandHook.INSTANCE.getCommands().stream()
            .filter(cmd -> cmd.types.messageCtx() || cmd.types.userCtx()).count() + "", true);
        
        embed.setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl());

        return embed;
    }
}
