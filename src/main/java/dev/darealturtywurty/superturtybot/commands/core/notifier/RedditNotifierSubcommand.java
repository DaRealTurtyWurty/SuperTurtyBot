package dev.darealturtywurty.superturtybot.commands.core.notifier;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RedditNotifier;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bson.conversions.Bson;

public class RedditNotifierSubcommand extends BaseNotifierSubcommand {
    public RedditNotifierSubcommand() {
        super("reddit", "Listens for new posts on a subreddit.");
        addOption(OptionType.STRING, "subreddit", "The subreddit to listen to", true);
        addOption(discordChannelOption());
        addOption(mentionOption());
        addOption(unsubscribeOption());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageServer(event))
            return;

        String subreddit = event.getOption("subreddit", OptionMapping::getAsString);
        if (subreddit == null) {
            reply(event, "❌ You must provide a subreddit!", false, true);
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null)
            return;

        Bson findFilter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("subreddit", subreddit));

        boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (Database.getDatabase().redditNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                reply(event, "✅ I have successfully unsubscribed the notifier for this subreddit!");
            } else {
                reply(event, "❌ You do not have any notifiers for this subreddit!", false, true);
            }

            return;
        }

        if (Database.getDatabase().redditNotifier.find(findFilter).first() != null) {
            reply(event, "❌ You already have a notifier for this subreddit!", false, true);
            return;
        }

        ChannelMentionContext context = requireChannelAndMention(event);
        if (context == null)
            return;

        Database.getDatabase().redditNotifier.insertOne(
                new RedditNotifier(guild.getIdLong(), subreddit, context.channelId(), context.mention()));

        reply(event, "✅ I have successfully set up a notifier for this subreddit in <#" + context.channelId() + ">!");
    }
}
