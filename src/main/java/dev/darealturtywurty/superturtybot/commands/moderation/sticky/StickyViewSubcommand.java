package dev.darealturtywurty.superturtybot.commands.moderation.sticky;

import dev.darealturtywurty.superturtybot.database.pojos.collections.StickyMessage;
import dev.darealturtywurty.superturtybot.modules.StickyMessageManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.awt.*;
import java.time.Instant;

public class StickyViewSubcommand extends StickySubcommand {
    public StickyViewSubcommand() {
        super("view", "Views the sticky configured for a channel");
        addOptions(channelOption());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageMessages(event) || event.getGuild() == null)
            return;

        GuildMessageChannel channel = requireChannel(event);
        if (channel == null)
            return;

        StickyMessage sticky = StickyMessageManager.getSticky(event.getGuild().getIdLong(), channel.getIdLong());
        if (sticky == null) {
            reply(event, "❌ There is no sticky configured for " + channel.getAsMention() + ".", false, true);
            return;
        }

        if (sticky.hasEmbed()) {
            EmbedBuilder embed = EmbedBuilder.fromData(DataObject.fromJson(sticky.getEmbed()));
            embed.setTimestamp(Instant.ofEpochMilli(sticky.getUpdatedAt()));
            reply(event, embed, false, true);
            return;
        }

        var embed = new EmbedBuilder();
        embed.setTitle("Sticky Message");
        embed.setColor(Color.ORANGE);
        embed.setTimestamp(Instant.ofEpochMilli(sticky.getUpdatedAt()));
        embed.addField("Channel", channel.getAsMention(), false);
        embed.addField("Type", "Text", true);
        embed.addField("Owner", "<@" + sticky.getOwner() + ">", true);
        embed.setDescription(sticky.getContent());
        reply(event, embed, false, true);
    }
}
