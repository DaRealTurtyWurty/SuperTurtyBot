package io.github.darealturtywurty.superturtybot.commands.util;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.TwitchNotifier;
import io.github.darealturtywurty.superturtybot.weblisteners.TwitchListener;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class TwitchNotificationsCommand extends CoreCommand {
    public TwitchNotificationsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "twitch_channel", "The name of the Twitch channel", true),
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
        return "Sets up a notification system for every time a Twitch channel goes live.";
    }

    @Override
    public String getHowToUse() {
        return "/twitch-notis [twitchChannel] [discordChannel] [whoToPing]";
    }

    @Override
    public String getName() {
        return "twitch-notis";
    }

    @Override
    public String getRichName() {
        return "Twitch Notifications";
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
        
        final String twitchChannel = event.getOption("twitch_channel").getAsString();

        final Bson findFilter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("channel", twitchChannel));
        
        final boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (Database.getDatabase().twitchNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                TwitchListener.unsubscribe(event.getGuild(), twitchChannel);
                reply(event, "✅ I have successfully unsubscribed the notifier for this Twitch channel!");
            } else {
                reply(event, "❌ You do not have any notifiers for this Twitch channel!", false, true);
            }
            
            return;
        }

        final boolean exists = Database.getDatabase().twitchNotifier.find(findFilter).first() != null;
        if (exists) {
            reply(event, "❌ You already have a notifier for this Twitch channel!", false, true);
            return;
        }
        
        final long discordChannel = event.getOption("discord_channel").getAsChannel().getIdLong();
        final String mention = event.getOption("who_to_ping").getAsMentionable().getAsMention();

        Database.getDatabase().twitchNotifier
            .insertOne(new TwitchNotifier(event.getGuild().getIdLong(), twitchChannel, discordChannel, mention));
        if (TwitchListener.subscribeChannel(event.getGuild(), twitchChannel)) {
            reply(event,
                "✅ I have successfully setup a notifier for this Twitch channel in <#" + discordChannel + ">!");
        } else {
            reply(event,
                "❌ I have failed to setup a notifier for this channel. Check that the channel name is correct!", false,
                true);
        }
    }
}
