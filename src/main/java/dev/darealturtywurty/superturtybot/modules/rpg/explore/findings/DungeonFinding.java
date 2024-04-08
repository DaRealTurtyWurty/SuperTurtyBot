package dev.darealturtywurty.superturtybot.modules.rpg.explore.findings;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.RPGManager;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Finding;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Outcome;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.response.ResponseBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.time.Instant;

public class DungeonFinding extends Finding {
    public DungeonFinding() {
        super(Outcome.UNKNOWN, "Hey! Look! Is that a dungeon? Is it worth the risk though?");
    }

    @Override
    public ResponseBuilder getResponse(RPGPlayer player, JDA jda, long guild, long channel) {
        return ResponseBuilder.start(player, jda, guild, channel).first(() -> {
            final var user = jda.getUserById(player.getUser());
            if (user == null)
                return;

            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setFooter(user.getEffectiveName(), user.getEffectiveAvatarUrl());
            embed.setColor(getOutcome().getColor());
            embed.setDescription(randomFoundMessage());

            final var guildObj = jda.getGuildById(guild);
            if (guildObj == null)
                return;

            final var textChannel = guildObj.getTextChannelById(channel);
            if (textChannel == null)
                return;

            textChannel.sendMessage(user.getAsMention()).setEmbeds(embed.build()).queue();
            textChannel.sendMessage("Would you like to explore the dungeon?").queue();
        }).condition(event -> "yes".equalsIgnoreCase(event.getMessage().getContentDisplay().trim()))
                .ifTrue(event -> RPGManager.startDungeoning(player, jda, guild, channel)).startFinding(this)
                .ifFalse(event -> event.getMessage().reply("Ok, dungeon avoided!").mentionRepliedUser(false).queue());
    }
}
