package io.github.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.jetbrains.annotations.Nullable;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.modules.StatTracker.DataRetriever;
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
        embed.addField("Created On", formatTime(jda.getSelfUser().getTimeCreated()), false);
        if (fromGuild) {
            embed.addField("Joined At", formatTime(guild.getSelfMember().getTimeJoined()), false);
        }

        embed.addField("Server Count", DataRetriever.getGuildCount() + "", false);
        embed.addField("Member Count", DataRetriever.getMemberCount() + "", false);
        int messagesSent = 0, messagesEdited = 0, messagesDeleted = 0, reactionsAdded = 0, reactionsRemoved = 0,
            slashCommandsRan = 0, normalCommandsRan = 0, messageCtxCommandsRan = 0, userCtxCommandsRan = 0;
        for (final long guildId : DataRetriever.getGuildIDs()) {
            final var stats = DataRetriever.getGuildStats(guildId);
            messagesSent += stats.getMessagesSent();
            messagesEdited += stats.getMessagesEdited();
            messagesDeleted += stats.getMessagesDeleted();
            reactionsAdded += stats.getReactionsAdded();
            reactionsRemoved += stats.getReactionsRemoved();
            slashCommandsRan += stats.getSlashCommandsRan();
            normalCommandsRan += stats.getNormalCommandsRan();
            messageCtxCommandsRan += stats.getMessageCtxCommandsRan();
            userCtxCommandsRan += stats.getUserCtxCommandsRan();
        }
        
        embed.addField("", "__**Observed Stats**__", false);
        embed.addField("Messages Sent", messagesSent + "", true);
        embed.addField("Messages Edited", messagesEdited + "", true);
        embed.addField("Messages Deleted", messagesDeleted + "", true);
        embed.addField("Reactions Added", reactionsAdded + "", true);
        embed.addField("Reactions Removed", reactionsRemoved + "", true);
        embed.addField("Slash Commands", slashCommandsRan + "", true);
        embed.addField("Msg Ctx Commands", messageCtxCommandsRan + "", true);
        embed.addField("User Ctx Commands", userCtxCommandsRan + "", true);
        embed.addField("Normal Commands", normalCommandsRan + "", true);

        embed.setFooter("Note: These statistics only update whilst the bot is online!");
        embed.setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl());
        
        return embed;
    }

    // TODO: Utility class
    private static String formatTime(OffsetDateTime time) {
        return time.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
