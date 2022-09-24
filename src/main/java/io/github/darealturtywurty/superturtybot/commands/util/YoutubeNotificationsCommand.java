package io.github.darealturtywurty.superturtybot.commands.util;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class YoutubeNotificationsCommand extends CoreCommand {

    public YoutubeNotificationsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "youtube_channel_id", "The ID of the YouTube channel (not the name)",
                true),
            new OptionData(OptionType.CHANNEL, "discord_channel", "The channel to send notifications to", true),
            new OptionData(OptionType.MENTIONABLE, "who_to_ping", "Who should be pinged when a notification happens",
                true),
            new OptionData(OptionType.BOOLEAN, "unsubscribe", "Whether or not to unsubscribe this notifier", false));
    }

    @Override
    public String getAccess() {
        return "Moderators Only (Manage Server Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Sets up a notification system for every time a YouTube channel uploads.";
    }

    @Override
    public String getHowToUse() {
        return "/youtube-notis [youtubeChannelId] [discordChannel] [whoToPing]";
    }

    @Override
    public String getName() {
        return "youtube-notis";
    }

    @Override
    public String getRichName() {
        return "YouTube Notifications";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }
        
        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return;
        }
        
        final String youtubeChannelId = event.getOption("youtube_channel_id").getAsString();

        final Bson findFilter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("youtubeChannel", youtubeChannelId));
        
        final boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (Database.getDatabase().youtubeNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                reply(event, "✅ I have successfully unsubscribed the notifier for this YouTube channel!");
            } else {
                reply(event, "❌ You do not have any notifiers for this YouTube channel!", false, true);
            }
            
            return;
        }

        final boolean exists = Database.getDatabase().youtubeNotifier.find(findFilter).first() != null;
        if (exists) {
            reply(event, "❌ You already have a notifier for this YouTube channel!", false, true);
            return;
        }
        
        final long discordChannel = event.getOption("discord_channel").getAsChannel().getIdLong();
        final String mention = event.getOption("who_to_ping").getAsMentionable().getAsMention();

        Database.getDatabase().youtubeNotifier
            .insertOne(new YoutubeNotifier(event.getGuild().getIdLong(), discordChannel, youtubeChannelId, mention));
        // TurtyBot.getYoutube().subscribe(event.getGuild().getIdLong(), youtubeChannelId);
        
        reply(event, "✅ I have successfully setup a notifier for this YouTube channel in <#" + discordChannel + ">!");
    }
}
