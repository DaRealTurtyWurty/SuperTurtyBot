package io.github.darealturtywurty.superturtybot.modules.counting;

import io.github.darealturtywurty.superturtybot.database.TurtyBotDatabase;
import io.github.darealturtywurty.superturtybot.database.impl.CountingDatabaseHandler;
import io.github.darealturtywurty.superturtybot.database.impl.CountingDatabaseHandler.ChannelData;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CountingHandler extends ListenerAdapter {
    public static final CountingHandler INSTANCE = new CountingHandler();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (shouldIgnore(event))
            return;

        final CountingDatabaseHandler database = TurtyBotDatabase.COUNTING;

        final TextChannel channel = event.getTextChannel();
        if (!database.isCountingChannel(event.getGuild(), channel.getIdLong()))
            return;

        final ChannelData data = database.getData(event.getGuild(), channel);
        final Message message = event.getMessage();

        final User author = message.getAuthor();
        final long lastCounter = data.lastCounter();
        if (author.getIdLong() == lastCounter) {
            database.resetChannel(event.getGuild(), channel.getIdLong(), data);

            // TODO: Configurable amount of times in a row
            message.addReaction("❌")
                .queue(success -> message.reply("💀 You cannot count multiple times in a row!").queue());
            return;
        }

        final String content = message.getContentRaw();
        final String beginning = content.split("\s*")[0];
        
        final Integer givenNumber = switch (data.mode()) {
            case NORMAL:
                try {
                    yield Integer.parseInt(beginning);
                } catch (final NumberFormatException exception) {
                    yield null;
                }
            default:
                message.reply("An invalid counting mode has been found! This has been reported to the bot owner.")
                    .mentionRepliedUser(true).queue();
                throw new UnsupportedOperationException(
                    "An invalid counting mode has been found!\nGuild: " + event.getGuild().toString() + "\n\nChannel: "
                        + channel.toString() + "\n\nMode: " + data.mode().toString());
        };
        
        if (givenNumber == null)
            return;

        if (givenNumber <= data.currentCount() || givenNumber > data.currentCount() + 1) {
            database.resetChannel(event.getGuild(), channel.getIdLong(), data);

            // TODO: Option to use a save
            message.addReaction("❌").queue(success -> message.reply("💀 " + givenNumber + " was incorrect. It was `"
                + (data.currentCount() + 1) + "`! The next number is: **" + 1 + "**!").queue());
            return;
        }

        message.addReaction("✅")
            .queue(success -> message.reply("👍 The next number is **" + (givenNumber + 1) + "**!").queue());
        database.setData(event.getGuild(), channel.getIdLong(), new ChannelData(data.mode(), givenNumber,
            givenNumber > data.highestCount() ? givenNumber + 1 : data.highestCount(), author.getIdLong()));
    }
    
    public static boolean shouldIgnore(MessageReceivedEvent event) {
        return !event.isFromGuild() || event.isFromThread() || event.isWebhookMessage() || event.getAuthor().isBot()
            || event.getAuthor().isSystem() || event.getChannelType() != ChannelType.TEXT;
    }
}
