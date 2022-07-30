package io.github.darealturtywurty.superturtybot.modules.idlerpg;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RPGManager extends ListenerAdapter {
    public static final RPGManager INSTANCE = new RPGManager();
    private static final ScheduledExecutorService EXCECUTOR = Executors.newScheduledThreadPool(100);

    public void explore(Member member, TextChannel channel) {
        final JDA jda = member.getJDA();
        final long userid = member.getIdLong();
        final long guildid = member.getGuild().getIdLong();
        final long channelid = channel.getIdLong();

        EXCECUTOR.schedule(() -> {
            final Guild guild = jda.getGuildById(guildid);
            if (guild == null)
                return;

            final Member explorer = guild.getMemberById(userid);
            if (explorer == null)
                return;

            MessageChannel responseChannel = guild.getTextChannelById(channelid);
            if (responseChannel == null) {
                responseChannel = explorer.getUser().openPrivateChannel().complete();
                if (responseChannel == null)
                    return;
            }
            
            final ExploreResult result = explore();

            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setFooter(explorer.getUser().getName() + "#" + explorer.getUser().getDiscriminator(),
                explorer.getEffectiveAvatarUrl());
            embed.setColor(result.finding().getBruhnuments().getColor());

            responseChannel.sendMessage(explorer.getAsMention()).setEmbeds(embed.build()).queue();
        }, ThreadLocalRandom.current().nextInt(3, 7), TimeUnit.MINUTES);
    }

    private static ExploreResult explore() {
        return new ExploreResult(Finding.values()[ThreadLocalRandom.current().nextInt(Finding.values().length)]);
    }
}
