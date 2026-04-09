package dev.darealturtywurty.superturtybot.commands.moderation.sticky;

import dev.darealturtywurty.superturtybot.modules.StickyMessageManager;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class StickyClearSubcommand extends StickySubcommand {
    public StickyClearSubcommand() {
        super("clear", "Removes the sticky configured for a channel");
        addOptions(channelOption());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageMessages(event) || event.getGuild() == null)
            return;

        GuildMessageChannel channel = requireChannel(event);
        if (channel == null)
            return;

        if (!StickyMessageManager.clearSticky(event.getGuild(), channel.getIdLong())) {
            reply(event, "❌ There is no sticky configured for " + channel.getAsMention() + ".", false, true);
            return;
        }

        reply(event, "✅ Sticky cleared for " + channel.getAsMention() + ".", false, true);
    }
}
