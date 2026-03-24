package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bson.conversions.Bson;

public class YoutubeNotifierSubcommand extends BaseNotifierSubcommand {
    public YoutubeNotifierSubcommand() {
        super("youtube", "Listens for YouTube uploads on the given YouTube channel.");
        addOption(OptionType.STRING, "youtube_channel_id", "The ID of the YouTube channel (not the name)", true);
        addOption(discordChannelOption());
        addOption(mentionOption());
        addOption(unsubscribeOption());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageServer(event))
            return;

        String youtubeChannelId = event.getOption("youtube_channel_id", OptionMapping::getAsString);
        if (youtubeChannelId == null) {
            reply(event, "❌ You must provide a YouTube channel ID!", false, true);
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null)
            return;

        Bson findFilter = Filters.and(Filters.eq("guild", guild.getIdLong()),
                Filters.eq("youtubeChannel", youtubeChannelId));

        boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (Database.getDatabase().youtubeNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                reply(event, "✅ I have successfully unsubscribed the notifier for this YouTube channel!");
            } else {
                reply(event, "❌ You do not have any notifiers for this YouTube channel!", false, true);
            }

            return;
        }

        if (Database.getDatabase().youtubeNotifier.find(findFilter).first() != null) {
            reply(event, "❌ You already have a notifier for this YouTube channel!", false, true);
            return;
        }

        ChannelMentionContext context = requireChannelAndMention(event);
        if (context == null)
            return;

        Database.getDatabase().youtubeNotifier.insertOne(
                new YoutubeNotifier(guild.getIdLong(), context.channelId(), youtubeChannelId, context.mention()));

        reply(event, "✅ I have successfully set up a notifier for this YouTube channel in <#" + context.channelId() + ">!");
    }
}
