package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SiegeNotifier;

public class SiegeNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public SiegeNotifierSubcommand() {
        super("siege", "Listens for Rainbow Six Siege patch notes and designer's notes.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a Rainbow Six Siege notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a Rainbow Six Siege notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the Rainbow Six Siege notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a Rainbow Six Siege notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().siegeNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().siegeNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().siegeNotifier.insertOne(new SiegeNotifier(guildId, channelId, mention));
    }
}
