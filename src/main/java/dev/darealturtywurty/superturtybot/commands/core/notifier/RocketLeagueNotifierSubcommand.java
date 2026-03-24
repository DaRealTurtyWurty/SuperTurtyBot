package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RocketLeagueNotifier;

public class RocketLeagueNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public RocketLeagueNotifierSubcommand() {
        super("rocketleague", "Listens for Rocket League patch notes.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a Rocket League notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a Rocket League notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the Rocket League notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a Rocket League notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().rocketLeagueNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().rocketLeagueNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().rocketLeagueNotifier.insertOne(new RocketLeagueNotifier(guildId, channelId, mention));
    }
}
