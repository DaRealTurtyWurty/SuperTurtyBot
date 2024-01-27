package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.*;
import java.time.Instant;

public class ServerInfoCommand extends CoreCommand {
    public ServerInfoCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }
    
    @Override
    public String getDescription() {
        return "Retrieves information about this server";
    }
    
    @Override
    public String getName() {
        return "serverinfo";
    }
    
    @Override
    public String getRichName() {
        return "Server Info";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final Guild guild = event.getGuild();
        if (!event.isFromGuild() || guild == null) {
            reply(event, "This command can only be used inside a server!", false);
            return;
        }

        final EmbedBuilder embed = createEmbed(guild);
        reply(event, embed, false);
    }
    
    private static EmbedBuilder createEmbed(Guild guild) {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.RED);
        embed.setThumbnail(guild.getIconUrl());
        embed.setTitle(guild.getName());
        embed.setDescription(guild.getDescription());
        
        embed.addField("Boost Count", String.valueOf(guild.getBoostCount()), true);
        embed.addField("Categories", String.valueOf(guild.getCategories().size()), true);
        embed.addField("Text Channels", String.valueOf(guild.getTextChannels().size()), true);
        embed.addField("Voice Channels", String.valueOf(guild.getVoiceChannels().size()), true);
        embed.addField("Emotes", String.valueOf(guild.getEmojis().size()), true);
        embed.addField("Members", String.valueOf(guild.getMemberCount()), true);
        
        embed.addField("Created", TimeFormat.RELATIVE.format(guild.getTimeCreated()), false);
        
        embed.addField("Notification Level", StringUtils.convertNotificationLevel(guild.getDefaultNotificationLevel()),
            true);
        embed.addField("Explicit Content Level", StringUtils.convertExplicitContentLevel(guild.getExplicitContentLevel()),
            true);
        embed.addField("NSFW Level", StringUtils.convertNSFWLevel(guild.getNSFWLevel()), true);
        embed.addField("Verification Level", StringUtils.convertVerificationLevel(guild.getVerificationLevel()), true);
        
        embed.addField("Owner", guild.getOwner() == null ? "Unknown" : guild.getOwner().getAsMention(), true);
        embed.addField("Max File Size", String.format("%.2g", guild.getMaxFileSize() / 1000000f) + "MB", true);
        
        if (guild.getBoostCount() > 0) {
            final var boosters = new StringBuilder();
            guild.getBoosters().forEach(booster -> boosters.append(booster.getAsMention()).append(", "));
            boosters.delete(boosters.length() - 1, boosters.length());
            embed.addField("Boosters", boosters.toString(), false);
        }
        
        final var roles = new StringBuilder();
        guild.getRoles().forEach(role -> roles.append(role.getAsMention()).append(", "));
        roles.delete(roles.length() - 2, roles.length());
        embed.addField("Roles", roles.toString(), false);
        
        return embed;
    }
}
