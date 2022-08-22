package io.github.darealturtywurty.superturtybot.modules.idlerpg.commands;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

// TODO: Use config for prefix
public class StartCommand extends RPGCommand {
    @Override
    public String getDescription() {
        return "Starts your RPG journey!";
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getRichName() {
        return "RPG Start";
    }

    @Override
    protected boolean requiresProfile() {
        return false;
    }
    
    @Override
    protected void run(MessageReceivedEvent event) {
        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", event.getAuthor().getIdLong()));

        RPGPlayer found = Database.getDatabase().rpgStats.find(filter).first();
        if (found != null) {
            reply(event,
                "❌ You already have a RPG profile in this server! You can use `.profile` to view your profile!");
            return;
        }

        found = new RPGPlayer(event.getGuild().getIdLong(), event.getAuthor().getIdLong());
        Database.getDatabase().rpgStats.insertOne(found);

        reply(event,
            "✅ Your profile has been created! To view your profile, use `.profile`. For a guide on how to get started, use `.guide`.");
    }
}
