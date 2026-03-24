package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SteamNotifier;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bson.conversions.Bson;

public class SteamNotifierSubcommand extends BaseNotifierSubcommand {
    public SteamNotifierSubcommand() {
        super("steam", "Listens for when a Steam game is updated.");
        addOption(OptionType.INTEGER, "steam_app_id", "The app ID of the game on steam", true);
        addOption(discordChannelOption());
        addOption(mentionOption());
        addOption(unsubscribeOption());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageServer(event))
            return;

        int appId = event.getOption("steam_app_id", -1, OptionMapping::getAsLong).intValue();
        if (appId == -1) {
            reply(event, "❌ You must provide a Steam app ID!", false, true);
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null)
            return;

        Bson findFilter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("appId", appId));

        boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (Database.getDatabase().steamNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                reply(event, "✅ I have successfully unsubscribed the notifier for this Steam app!");
            } else {
                reply(event, "❌ You do not have any notifiers for this Steam app!", false, true);
            }

            return;
        }

        if (Database.getDatabase().steamNotifier.find(findFilter).first() != null) {
            reply(event, "❌ You already have a notifier for this Steam app!", false, true);
            return;
        }

        ChannelMentionContext context = requireChannelAndMention(event);
        if (context == null)
            return;

        Database.getDatabase().steamNotifier.insertOne(
                new SteamNotifier(guild.getIdLong(), context.channelId(), appId, context.mention()));

        reply(event, "✅ I have successfully set up a notifier for this Steam app in <#" + context.channelId() + ">!");
    }
}
