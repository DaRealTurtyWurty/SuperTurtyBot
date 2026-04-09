package dev.darealturtywurty.superturtybot.commands.moderation.automod;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;
import java.util.Objects;

abstract class AutoModSubcommand extends SubcommandCommand {
    protected AutoModSubcommand(String name, String description) {
        super(name, description);
    }

    protected static GuildData requireConfig(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return null;
        }

        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "❌ You do not have permission to configure automod!", false, true);
            return null;
        }

        return GuildData.getOrCreateGuildData(event.getGuild());
    }

    protected static boolean saveConfig(Guild guild, GuildData config) {
        UpdateResult result = Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guild.getIdLong()), config);
        return result.getModifiedCount() > 0 || result.getMatchedCount() > 0;
    }

    protected static String bool(boolean enabled) {
        return enabled ? "`Enabled`" : "`Disabled`";
    }

    protected static String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).reduce((left, right) -> left + " " + right).orElse("");
    }

    protected static String formatChannelList(Guild guild, List<Long> channelIds) {
        if (channelIds.isEmpty())
            return "`None`";

        return channelIds.stream()
                .map(guild::getGuildChannelById)
                .filter(Objects::nonNull)
                .map(GuildChannel::getAsMention)
                .reduce((left, right) -> left + ", " + right)
                .orElse("`None`");
    }
}
