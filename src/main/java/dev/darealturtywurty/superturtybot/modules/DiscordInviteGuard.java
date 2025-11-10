package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Handles invite filtering for whitelisted channels only.
 */
public class DiscordInviteGuard {
    private static final Pattern INVITE_REGEX = Pattern.compile("(https?://)?(www\\.)?(discord\\.(gg|io|me|li)|discordapp\\.com/invite)/[^ /]+?(?=\\b)");

    public void handleMessage(Message message) {
        if (!message.isFromGuild())
            return;

        final Guild guild = message.getGuild();
        final GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (data == null || !data.isDiscordInviteGuardEnabled())
            return;

        final List<Long> whitelistedChannels = GuildData.getLongs(data.getDiscordInviteWhitelistChannels());
        if (whitelistedChannels.isEmpty() || !whitelistedChannels.contains(message.getChannel().getIdLong()))
            return;

        if (INVITE_REGEX.matcher(message.getContentRaw()).find()) {
            message.delete()
                    .queue(success -> message.getChannel()
                            .sendMessage(message.getAuthor().getAsMention() + " No invite links allowed!")
                            .queue(msg -> msg.delete().queueAfter(15, TimeUnit.SECONDS)));
        }
    }
}
