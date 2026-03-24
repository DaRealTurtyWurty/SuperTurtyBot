package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.LeagueNotifier;

public class LeagueNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public LeagueNotifierSubcommand() {
        super("league", "Listens for League of Legends patch notes.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a League of Legends notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a League of Legends notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the League of Legends notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a League of Legends notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().leagueNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().leagueNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().leagueNotifier.insertOne(new LeagueNotifier(guildId, channelId, mention));
    }
}
