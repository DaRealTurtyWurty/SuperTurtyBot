package dev.darealturtywurty.superturtybot.modules.rpg.explore.findings;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.RPGManager;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Finding;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.FindingRegistry;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Outcome;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.response.ResponseBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Instant;

public class CaveFinding extends Finding {
    public CaveFinding() {
        super(Outcome.UNKNOWN, "Woah, you found a massive cave. I wonder what could be inside?");
    }

    @Override
    public ResponseBuilder getResponse(RPGPlayer player, JDA jda, long guild, long channel) {
        return ResponseBuilder.start(player, jda, guild, channel).first(() -> {
                    final User user = jda.getUserById(player.getUser());
                    if (user == null)
                        return;

                    final var embed = new EmbedBuilder();
                    embed.setTimestamp(Instant.now());
                    embed.setFooter(user.getEffectiveName(), user.getEffectiveAvatarUrl());
                    embed.setColor(getOutcome().getColor());
                    embed.setDescription(randomFoundMessage());

                    Guild guildObj = jda.getGuildById(guild);
                    if (guildObj == null)
                        return;

                    final TextChannel textChannel = guildObj.getTextChannelById(channel);
                    if (textChannel == null)
                        return;

                    textChannel.sendMessage(user.getAsMention()).setEmbeds(embed.build()).queue();
                    textChannel.sendMessage("Would you like to explore the cave?").queue();
                }).condition(event -> "yes".equalsIgnoreCase(event.getMessage().getContentDisplay().trim()))
                .ifTrue(event -> RPGManager.startCaving(player, jda, guild, channel)) /*.startFinding(FindingRegistry.CAVE_OUTCOME)*/
                .ifFalse(event -> event.getMessage().reply("Ok, cave avoided!").mentionRepliedUser(false).queue());
    }
}
