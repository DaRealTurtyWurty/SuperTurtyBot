package dev.darealturtywurty.superturtybot.commands.moderation.sticky;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.StickyMessage;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserEmbeds;
import dev.darealturtywurty.superturtybot.modules.StickyMessageManager;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class StickyEmbedSubcommand extends StickySubcommand {
    public StickyEmbedSubcommand() {
        super("embed", "Creates or updates a sticky embed using one of your saved embeds");
        addOptions(channelOption(), new OptionData(OptionType.STRING, "name", "The saved embed name", true, true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageMessages(event) || event.getGuild() == null)
            return;

        GuildMessageChannel channel = requireChannel(event);
        if (channel == null)
            return;

        String name = event.getOption("name", "", OptionMapping::getAsString);
        if (name.isBlank()) {
            reply(event, "❌ You must provide a saved embed name!", false, true);
            return;
        }

        UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", event.getUser().getIdLong())).first();
        if (userEmbeds == null || userEmbeds.getEmbed(name).isEmpty()) {
            reply(event, "❌ You do not have a saved embed with that name!", false, true);
            return;
        }

        String embedJson = userEmbeds.getEmbed(name).get().build().toData().toString();
        StickyMessage sticky = new StickyMessage(event.getGuild().getIdLong(), channel.getIdLong(), event.getUser().getIdLong(), null, embedJson);
        StickyMessageManager.saveSticky(sticky);
        StickyMessageManager.repostSticky(event.getGuild(), channel, sticky);
        reply(event, "✅ Sticky embed `" + name + "` configured for " + channel.getAsMention() + ".", false, true);
    }
}
