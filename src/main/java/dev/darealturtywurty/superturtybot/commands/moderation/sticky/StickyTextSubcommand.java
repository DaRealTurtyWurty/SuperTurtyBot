package dev.darealturtywurty.superturtybot.commands.moderation.sticky;

import dev.darealturtywurty.superturtybot.database.pojos.collections.StickyMessage;
import dev.darealturtywurty.superturtybot.modules.StickyMessageManager;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class StickyTextSubcommand extends StickySubcommand {
    public StickyTextSubcommand() {
        super("text", "Creates or updates a sticky plain-text message");
        addOptions(channelOption(), new OptionData(OptionType.STRING, "content", "The sticky text", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageMessages(event) || event.getGuild() == null)
            return;

        GuildMessageChannel channel = requireChannel(event);
        if (channel == null)
            return;

        String content = event.getOption("content", "", OptionMapping::getAsString);
        if (content.isBlank()) {
            reply(event, "❌ Sticky content cannot be blank!", false, true);
            return;
        }

        if (content.length() > MAX_STICKY_MESSAGE_LENGTH) {
            reply(event, "❌ Sticky content is too long!", false, true);
            return;
        }

        StickyMessage sticky = new StickyMessage(event.getGuild().getIdLong(), channel.getIdLong(), event.getUser().getIdLong(), content, null);
        StickyMessageManager.saveSticky(sticky);
        StickyMessageManager.repostSticky(event.getGuild(), channel, sticky);
        reply(event, "✅ Sticky text configured for " + channel.getAsMention() + ".", false, true);
    }
}
