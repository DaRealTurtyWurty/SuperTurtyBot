package io.github.darealturtywurty.superturtybot.modules.idlerpg.commands;

import java.time.Instant;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ProfileCommand extends RPGCommand {
    @Override
    public String getDescription() {
        return "Gets your IDLE RPG profile";
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public String getRichName() {
        return "RPG Profile";
    }

    @Override
    protected void run(MessageReceivedEvent event) {
        final RPGPlayer found = getStats(event);
        if (found == null)
            return;

        final var embed = new EmbedBuilder();
        embed.setColor(event.getMember().getColorRaw());
        embed.setTimestamp(Instant.now());
        embed.setFooter(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(),
            event.getMember().getEffectiveAvatarUrl());
        embed.setTitle(event.getAuthor().getName() + "'s Profile");
        embed.setThumbnail(event.getMember().getEffectiveAvatarUrl());
        embed.setDescription(
            "**__Level__**: " + found.getLevel() + "\n**__XP__**: " + found.getXp() + "\n**__Health__**: "
                + found.getHealth() + "/" + found.getMaxHealth() + "\n**__Money__**: $" + found.getMoney());
        
        reply(event, embed);
    }
}
