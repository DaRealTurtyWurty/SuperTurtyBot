package io.github.darealturtywurty.superturtybot.modules.idlerpg;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.Finding;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.FindingRegistry;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.ExploreResult;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RPGManager extends ListenerAdapter {
    public static final RPGManager INSTANCE = new RPGManager();
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(100);

    public void explore(Member member, TextChannel channel, Bson userFilter, RPGPlayer stats) {
        final JDA jda = member.getJDA();
        final long userid = member.getIdLong();
        final long guildid = member.getGuild().getIdLong();
        final long channelid = channel.getIdLong();

        EXECUTOR.schedule(() -> {
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
            final Finding finding = result.finding();

            finding.start(jda, stats, channelid);

            stats.setExploring(false);
            Database.getDatabase().rpgStats.updateOne(userFilter, Updates.set("exploring", false));
        }, ThreadLocalRandom.current().nextInt(2), TimeUnit.MINUTES);
    }

    private static ExploreResult explore() {
        return new ExploreResult(FindingRegistry.FINDINGS.random().getValue());
    }
}
