package dev.darealturtywurty.superturtybot.modules.rpg;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Finding;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.FindingRegistry;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Outcome;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.response.ResponseBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RPGManager {
    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static RPGPlayer getPlayer(long guild, long user) {
        return Database.getDatabase().rpgPlayers.find(Filters.and(
                Filters.eq("guild", guild),
                Filters.eq("user", user))
        ).first();
    }

    public static void createPlayer(long guild, long user) {
        Database.getDatabase().rpgPlayers.insertOne(new RPGPlayer(guild, user));
    }

    public static void updatePlayer(RPGPlayer player) {
        Database.getDatabase().rpgPlayers.replaceOne(Filters.and(
                        Filters.eq("guild", player.getGuild()),
                        Filters.eq("user", player.getUser())),
                player);
    }

    public static void deletePlayer(long guild, long user) {
        Database.getDatabase().rpgPlayers.deleteOne(Filters.and(
                Filters.eq("guild", guild),
                Filters.eq("user", user))
        );
    }

    public static void deletePlayer(RPGPlayer player) {
        deletePlayer(player.getGuild(), player.getUser());
    }

    public static void deleteAllPlayers(long guild) {
        Database.getDatabase().rpgPlayers.deleteMany(Filters.eq("guild", guild));
    }

    private static void deleteUser(long idLong) {
        Database.getDatabase().rpgPlayers.deleteMany(Filters.eq("user", idLong));
    }

    public static void startExploring(RPGPlayer player, JDA jda, long guildId, long channelId, long messageId) {
        player.setExploring(true);
        player.setExploreTime(System.currentTimeMillis() + /*(RANDOM.nextLong(300L) + 300L) * 1000L*/ 1000L);
        updatePlayer(player);

        EXECUTOR.schedule(() -> {
            player.setExploring(false);
            player.setExploreTime(0L);
            updatePlayer(player);

            Finding result = generateExploreResult(player);
            result.start(player, jda, guildId, channelId);
        }, player.getExploreTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    // TODO: Have findings depend on a player's current situation
    private static Finding generateExploreResult(RPGPlayer player) {
        return FindingRegistry.FINDINGS.random().getValue();
    }

    private static void dmUser(JDA jda, long userId, String message) {
        jda.retrieveUserById(userId).queue(
                user -> user.openPrivateChannel().flatMap(dm -> dm.sendMessage(message)).queue(),
                throwable -> deleteUser(userId));
    }

    public static void startCaving(RPGPlayer player, JDA jda, long guild, long channel) {
        player.setCaving(true);
        player.setCaveTime(System.currentTimeMillis() + (RANDOM.nextLong(300L) + 300L) * 1000L);
        updatePlayer(player);

        EXECUTOR.schedule(() -> {
            player.setCaving(false);
            player.setCaveTime(0L);
            updatePlayer(player);

            dmUser(jda, player.getUser(), "You have finished exploring the cave!");
        }, player.getCaveTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public static void startDungeoning(RPGPlayer player, JDA jda, long guild, long channel) {
        player.setDungeoning(true);
        player.setDungeonTime(System.currentTimeMillis() + (RANDOM.nextLong(300L) + 300L) * 1000L);
        updatePlayer(player);

        EXECUTOR.schedule(() -> {
            player.setDungeoning(false);
            player.setDungeonTime(0L);
            updatePlayer(player);

            dmUser(jda, player.getUser(), "You have finished exploring the dungeon!");
        }, player.getDungeonTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
}
