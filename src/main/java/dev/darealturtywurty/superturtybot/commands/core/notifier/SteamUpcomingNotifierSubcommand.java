package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SteamUpcomingNotifier;

public class SteamUpcomingNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public SteamUpcomingNotifierSubcommand() {
        super("steamupcoming", "Listens for popular upcoming Steam releases on Windows.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a Steam upcoming notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a Steam upcoming notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the Steam upcoming notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a Steam upcoming notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().steamUpcomingNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().steamUpcomingNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().steamUpcomingNotifier.insertOne(new SteamUpcomingNotifier(guildId, channelId, mention));
    }
}
