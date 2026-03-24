package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.ValorantNotifier;

public class ValorantNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public ValorantNotifierSubcommand() {
        super("valorant", "Listens for VALORANT patch notes.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a VALORANT notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a VALORANT notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the VALORANT notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a VALORANT notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().valorantNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().valorantNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().valorantNotifier.insertOne(new ValorantNotifier(guildId, channelId, mention));
    }
}
