package io.github.darealturtywurty.superturtybot.commands.util;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.SteamNotifier;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.TwitchNotifier;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;
import io.github.darealturtywurty.superturtybot.weblisteners.social.TwitchListener;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class NotifierCommand extends CoreCommand {
    public NotifierCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("youtube", "Listens for YouTube uploads on the given YouTube channel.")
                .addOptions(List.of(
                    new OptionData(OptionType.STRING, "youtube_channel_id",
                        "The ID of the YouTube channel (not the name)", true),
                    new OptionData(OptionType.CHANNEL, "discord_channel", "The channel to send notifications to", true),
                    new OptionData(OptionType.MENTIONABLE, "who_to_ping",
                        "Who should be pinged when a notification happens", true),
                    new OptionData(OptionType.BOOLEAN, "unsubscribe", "Whether or not to unsubscribe this notifier",
                        false))),
            new SubcommandData("twitch", "Listens for when a Twitch channel goes live.").addOptions(
                List.of(new OptionData(OptionType.STRING, "twitch_channel", "The name of the Twitch channel", true),
                    new OptionData(OptionType.CHANNEL, "discord_channel", "The channel to send notifications to", true),
                    new OptionData(OptionType.MENTIONABLE, "who_to_ping",
                        "Who should be pinged when a notification happens", true),
                    new OptionData(OptionType.BOOLEAN, "unsubscribe", "Whether or not to unsubscribe this notifier",
                        false))),
            new SubcommandData("steam", "Listens for when a Steam game is updated.").addOptions(
                List.of(new OptionData(OptionType.INTEGER, "steam_app_id", "The app ID of the game on steam", true),
                    new OptionData(OptionType.CHANNEL, "discord_channel", "The channel to send notifications to", true),
                    new OptionData(OptionType.MENTIONABLE, "who_to_ping",
                        "Who should be pinged when a notification happens", true),
                    new OptionData(OptionType.BOOLEAN, "unsubscribe", "Whether or not to unsubscribe this notifier",
                        false))));
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
        return "Sets up a notification system for the given social media channel/game/etc";
    }

    @Override
    public String getHowToUse() {
        return "/notifier [social] [whatToListenTo] [discordChannel] [whoToPing]";
    }

    @Override
    public String getName() {
        return "notifier";
    }

    @Override
    public String getRichName() {
        return "Notifier";
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

        switch (event.getSubcommandName()) {
            case "youtube": {
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

                Database.getDatabase().youtubeNotifier.insertOne(
                    new YoutubeNotifier(event.getGuild().getIdLong(), discordChannel, youtubeChannelId, mention));

                reply(event,
                    "✅ I have successfully setup a notifier for this YouTube channel in <#" + discordChannel + ">!");
                break;
            }
            case "twitch": {
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

                Database.getDatabase().twitchNotifier.insertOne(
                    new TwitchNotifier(event.getGuild().getIdLong(), twitchChannel, discordChannel, mention));
                if (TwitchListener.subscribeChannel(event.getGuild(), twitchChannel)) {
                    reply(event,
                        "✅ I have successfully setup a notifier for this Twitch channel in <#" + discordChannel + ">!");
                } else {
                    reply(event,
                        "❌ I have failed to setup a notifier for this channel. Check that the channel name is correct!",
                        false, true);
                }
                break;
            }
            case "steam": {
                final int appId = event.getOption("steam_app_id").getAsInt();

                final Bson findFilter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                    Filters.eq("appId", appId));

                final boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
                if (unsubscribe) {
                    if (Database.getDatabase().steamNotifier.deleteOne(findFilter).getDeletedCount() != 0) {
                        reply(event, "✅ I have successfully unsubscribed the notifier for this Steam app!");
                    } else {
                        reply(event, "❌ You do not have any notifiers for this Steam app!", false, true);
                    }

                    return;
                }

                final boolean exists = Database.getDatabase().steamNotifier.find(findFilter).first() != null;
                if (exists) {
                    reply(event, "❌ You already have a notifier for this Steam app!", false, true);
                    return;
                }

                final long discordChannel = event.getOption("discord_channel").getAsChannel().getIdLong();
                final String mention = event.getOption("who_to_ping").getAsMentionable().getAsMention();

                Database.getDatabase().steamNotifier
                    .insertOne(new SteamNotifier(event.getGuild().getIdLong(), discordChannel, appId, mention));

                reply(event, "✅ I have successfully setup a notifier for this Steam app in <#" + discordChannel + ">!");
                break;
            }
            default:
                throw new IllegalStateException(
                    "Unexpected subcommand for notifier command: " + event.getSubcommandName());
        }
    }
}
