package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MinecraftNotifier;

public class MinecraftNotifierSubcommand extends SingleGuildNotifierSubcommand {
    public MinecraftNotifierSubcommand() {
        super("minecraft", "Listens for Minecraft release and snapshot articles.");
    }

    @Override
    protected String existsMessage() {
        return "❌ You already have a Minecraft notifier configured!";
    }

    @Override
    protected String subscribeMessage(long channelId) {
        return "✅ I have successfully set up a Minecraft notifier in <#" + channelId + ">!";
    }

    @Override
    protected String unsubscribeSuccessMessage() {
        return "✅ I have successfully unsubscribed the Minecraft notifier!";
    }

    @Override
    protected String unsubscribeMissingMessage() {
        return "❌ You do not have a Minecraft notifier configured!";
    }

    @Override
    protected boolean exists(long guildId) {
        return Database.getDatabase().minecraftNotifier.find(Filters.eq("guild", guildId)).first() != null;
    }

    @Override
    protected boolean delete(long guildId) {
        return Database.getDatabase().minecraftNotifier.deleteOne(Filters.eq("guild", guildId)).getDeletedCount() != 0;
    }

    @Override
    protected void insert(long guildId, long channelId, String mention) {
        Database.getDatabase().minecraftNotifier.insertOne(new MinecraftNotifier(guildId, channelId, mention));
    }
}
