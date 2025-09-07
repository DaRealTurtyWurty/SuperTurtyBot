package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;

public class BotInfoCommand extends CoreCommand {
    public BotInfoCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
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
        reply(event, embed, false);
    }

    private EmbedBuilder createEmbed(JDA jda, @Nullable Guild guild) {
        final boolean fromGuild = guild != null;
        final var embed = new EmbedBuilder();
        embed.setColor(fromGuild ? guild.getSelfMember().getColor() : Color.CYAN);
        embed.setTimestamp(Instant.now());
        embed.setTitle("Bot Information");
        embed.addField("Mention", jda.getSelfUser().getAsMention(), false);
        embed.addField("Created", TimeFormat.RELATIVE.format(jda.getSelfUser().getTimeCreated()), false);
        if (fromGuild) {
            embed.addField("Joined", TimeFormat.RELATIVE.format(guild.getSelfMember().getTimeJoined()), false);
        }

        embed.addField("", "**__Counts__**:", false);
        embed.addField("Members", String.valueOf(jda.getUserCache().size()), true);
        embed.addField("Roles", String.valueOf(jda.getRoleCache().size()), true);
        embed.addField("Emojis", String.valueOf(jda.getEmojiCache().size()), true);
        embed.addField("Servers", String.valueOf(jda.getGuildCache().size()), true);
        embed.addField("Categories", String.valueOf(jda.getCategoryCache().size()), true);
        embed.addField("Channels", String.valueOf(jda.getVoiceChannelCache().size() + jda.getTextChannelCache().size()
                + jda.getThreadChannelCache().size()), true);

        embed.addField("", "**__Commands__**:", false);
        embed.addField("Slash",
                String.valueOf(getCommandCount(CommandType.SLASH)),
                true);
        embed.addField("Prefix",
                String.valueOf(getCommandCount(CommandType.PREFIX)),
                true);
        embed.addField("Context",
                String.valueOf(getCommandCount(CommandType.MSG_CONTEXT) + getCommandCount(CommandType.USER_CONTEXT)),
                true);

        embed.setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl());

        return embed;
    }

    public static long getCommandCount(CommandType type) {
        return switch (type) {
            case SLASH -> CommandHook.INSTANCE.getCommands().stream()
                    .filter(cmd -> cmd.types.slash()).count();
            case PREFIX -> CommandHook.INSTANCE.getCommands().stream()
                    .filter(cmd -> cmd.types.normal()).count();
            case MSG_CONTEXT -> CommandHook.INSTANCE.getCommands().stream()
                    .filter(cmd -> cmd.types.messageCtx()).count();
            case USER_CONTEXT -> CommandHook.INSTANCE.getCommands().stream()
                    .filter(cmd -> cmd.types.userCtx()).count();
        };
    }

    public enum CommandType {
        SLASH,
        PREFIX,
        MSG_CONTEXT,
        USER_CONTEXT;
    }
}
