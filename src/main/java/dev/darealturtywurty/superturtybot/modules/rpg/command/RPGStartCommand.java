package dev.darealturtywurty.superturtybot.modules.rpg.command;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.RPGManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class RPGStartCommand extends RPGCommand {
    @Override
    public String getDescription() {
        return "Starts your RPG adventure!";
    }

    @Override
    public String getName() {
        return "rpgstart";
    }

    @Override
    public String getRichName() {
        return "Start RPG";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 5L);
    }

    @Override
    protected void handleSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();

        RPGPlayer player = RPGManager.getPlayer(guild.getIdLong(), user.getIdLong());
        if (player == null) {
            RPGManager.createPlayer(guild.getIdLong(), user.getIdLong());
            reply(event, "✅ You have started your RPG adventure!");
        } else {
            reply(event, "❌ You have already started your RPG adventure!", false, true);
        }
    }
}
