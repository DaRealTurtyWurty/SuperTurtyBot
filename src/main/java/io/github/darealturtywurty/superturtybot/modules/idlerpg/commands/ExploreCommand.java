package io.github.darealturtywurty.superturtybot.modules.idlerpg.commands;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.RPGManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ExploreCommand extends RPGCommand {
    @Override
    public String getDescription() {
        return "Explores the area to see whats around";
    }

    @Override
    public String getName() {
        return "explore";
    }

    @Override
    public String getRichName() {
        return "RPG Explore";
    }

    @Override
    protected void run(MessageReceivedEvent event) {
        final RPGPlayer found = getStats(event);
        if (found == null)
            return;

        if (found.isExploring()) {
            reply(event, "❌ You are already exploring!");
            return;
        }

        if (found.isFighting()) {
            reply(event, "❌ You are currently fighting, you cannot explore!");
            return;
        }

        if (found.isMining()) {
            reply(event, "❌ You are currently mining, you cannot explore!");
            return;
        }

        found.setExploring(true);
        final Bson update = Updates.set("exploring", found.isExploring());
        Database.getDatabase().rpgStats.updateOne(getFilter(event), update);
        RPGManager.INSTANCE.explore(event.getMember(), event.getChannel().asTextChannel(), getFilter(event), found);
        
        reply(event, "You start exploring into the wilderness. What will you find?", true);
    }
}
