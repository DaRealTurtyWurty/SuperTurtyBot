package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.TwitchNotifier;
import dev.darealturtywurty.superturtybot.weblisteners.social.TwitchListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bson.conversions.Bson;

public class TwitchNotifierSubcommand extends BaseNotifierSubcommand {
    public TwitchNotifierSubcommand() {
        super("twitch", "Listens for when a Twitch channel goes live.");
        addOption(OptionType.STRING, "twitch_channel", "The name of the Twitch channel", true);
        addOption(discordChannelOption());
        addOption(mentionOption());
        addOption(unsubscribeOption());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageServer(event))
            return;

        String twitchChannel = event.getOption("twitch_channel", OptionMapping::getAsString);
        if (twitchChannel == null) {
            reply(event, "❌ You must provide a Twitch channel!", false, true);
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null)
            return;

        Bson findFilter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("channel", twitchChannel));

        boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (Database.getDatabase().twitchNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                TwitchListener.unsubscribe(twitchChannel);
                reply(event, "✅ I have successfully unsubscribed the notifier for this Twitch channel!");
            } else {
                reply(event, "❌ You do not have any notifiers for this Twitch channel!", false, true);
            }

            return;
        }

        if (Database.getDatabase().twitchNotifier.find(findFilter).first() != null) {
            reply(event, "❌ You already have a notifier for this Twitch channel!", false, true);
            return;
        }

        ChannelMentionContext context = requireChannelAndMention(event);
        if (context == null)
            return;

        Database.getDatabase().twitchNotifier.insertOne(
                new TwitchNotifier(guild.getIdLong(), twitchChannel, context.channelId(), context.mention()));
        if (TwitchListener.subscribeChannel(twitchChannel)) {
            reply(event, "✅ I have successfully set up a notifier for this Twitch channel in <#" + context.channelId() + ">!");
        } else {
            reply(event, "❌ I have failed to set up a notifier for this channel. Check that the channel name is correct!",
                    false, true);
        }
    }
}
