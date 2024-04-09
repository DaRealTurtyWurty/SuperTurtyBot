package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class AnnounceCommand extends CoreCommand {
    public AnnounceCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Announces a message to all servers the bot is in!";
    }

    @Override
    public String getName() {
        return "announce";
    }

    @Override
    public String getRichName() {
        return "Announce";
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId().orElseThrow(() -> new IllegalStateException("Owner ID is not set!")))
            return;

        event.getMessage().delete().queue();
        int commandIndex = event.getMessage().getContentRaw().indexOf(getName());

        String content = "**[ANNOUNCEMENT]** " + event.getMessage().getContentRaw().substring(commandIndex + getName().length());
        event.getJDA().getGuilds().forEach(guild -> {
            GuildChannel defaultChannel = guild.getDefaultChannel();
            if (defaultChannel == null || !defaultChannel.getType().isMessage()) {
                List<TextChannel> textChannels = guild.getTextChannels();
                if (textChannels.isEmpty()) {
                    guild.createTextChannel("announcements").flatMap(channel -> channel.sendMessage(content)).queue();
                    return;
                }

                defaultChannel = textChannels.getFirst();
            }

            if(defaultChannel instanceof GuildMessageChannel messageChannel) {
                messageChannel.sendMessage(content).queue();
            }
        });
    }
}
