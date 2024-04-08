package dev.darealturtywurty.superturtybot.modules.rpg.command;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.RPGManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class RPGExploreCommand extends RPGCommand {
    @Override
    public String getDescription() {
        return "Explore the world!";
    }

    @Override
    public String getName() {
        return "rpgexplore";
    }

    @Override
    public String getRichName() {
        return "Explore";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void handleSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();

        RPGPlayer player = RPGManager.getPlayer(guild.getIdLong(), user.getIdLong());
        if (player == null) {
            reply(event, "❌ You have not started your RPG adventure!", false, true);
            return;
        }

        if(player.isDead()) {
            reply(event, "❌ You are dead! You can explore again %s!".formatted(TimeFormat.RELATIVE.format(player.getRespawnTime())), false, true);
            return;
        }

        if(player.isOccupied()) {
            reply(event, "❌ You are currently occupied with something else! You can explore again %s!".formatted(TimeFormat.RELATIVE.format(player.getOccupiedTime())), false, true);
            return;
        }


        event.reply("😲 You start exploring the world. What will you find?")
                .mentionRepliedUser(false)
                .flatMap(InteractionHook::retrieveOriginal)
                .queue(message -> RPGManager.startExploring(player, event.getJDA(), guild.getIdLong(), event.getChannel().getIdLong(), message.getIdLong()));
    }
}
