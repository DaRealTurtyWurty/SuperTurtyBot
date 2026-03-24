package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SteamStoreNotifier;

public class SteamStoreNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public SteamStoreNotifierSubcommand() {
        super("steamsales", "Listens for Steam store sales and fests.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a Steam sales/fests notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a Steam sales/fests notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the Steam sales/fests notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a Steam sales/fests notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().steamStoreNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().steamStoreNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().steamStoreNotifier.insertOne(new SteamStoreNotifier(guildId, channelId, mention));
    }
}
