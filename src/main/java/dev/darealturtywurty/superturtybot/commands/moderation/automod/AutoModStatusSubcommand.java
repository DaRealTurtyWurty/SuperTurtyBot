package dev.darealturtywurty.superturtybot.commands.moderation.automod;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;

public class AutoModStatusSubcommand extends AutoModSubcommand {
    public AutoModStatusSubcommand() {
        super("status", "Shows the current automod configuration");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildData config = requireConfig(event);
        if (config == null || event.getGuild() == null)
            return;

        var embed = new EmbedBuilder();
        embed.setTitle("AutoMod Status");
        embed.setColor(Color.ORANGE);
        embed.setTimestamp(Instant.now());
        embed.setFooter(event.getGuild().getName(), event.getGuild().getIconUrl());
        embed.addField("Invite Guard", bool(config.isDiscordInviteGuardEnabled()), true);
        embed.addField("Invite Whitelist", formatChannelList(event.getGuild(),
                GuildData.getLongs(config.getDiscordInviteWhitelistChannels())), false);
        embed.addField("Scam Detection", bool(config.isScamDetectionEnabled()), true);
        embed.addField("Image Spam AutoBan", bool(config.isImageSpamAutoBanEnabled()), true);
        embed.addField("Image Spam Window", config.getImageSpamWindowSeconds() + " seconds", true);
        embed.addField("Image Spam Min Images", String.valueOf(config.getImageSpamMinImages()), true);
        embed.addField("New Member Threshold", config.getImageSpamNewMemberThresholdHours() + " hours", true);
        reply(event, embed);
    }
}
