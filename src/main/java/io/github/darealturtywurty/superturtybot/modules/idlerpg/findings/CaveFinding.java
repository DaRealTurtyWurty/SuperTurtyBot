package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import java.time.Instant;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class CaveFinding extends Finding {
    public CaveFinding() {
        super(Outcome.UNKNOWN, "Woah, you found a massive cave. I wonder what could be inside?");
    }
    
    @Override
    protected ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return ResponseBuilder.start(jda, player, channel).first(() -> {
            final User user = jda.getUserById(player.getUser());
            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setFooter(user.getName() + "#" + user.getDiscriminator(), user.getEffectiveAvatarUrl());
            embed.setColor(getOutcome().getColor());
            embed.setDescription(randomFoundMessage());
            
            final TextChannel textChannel = jda.getTextChannelById(channel);
            textChannel.sendMessage(user.getAsMention()).setEmbeds(embed.build()).queue();
            textChannel.sendMessage("Would you like to explore the cave?").queue();
        }).condition(event -> "yes".equalsIgnoreCase(event.getMessage().getContentDisplay().trim())).then(event -> {

        }).end();
    }
}
