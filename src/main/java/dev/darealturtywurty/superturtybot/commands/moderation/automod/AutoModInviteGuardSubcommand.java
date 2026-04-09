package dev.darealturtywurty.superturtybot.commands.moderation.automod;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class AutoModInviteGuardSubcommand extends AutoModSubcommand {
    private static final int MAX_INVITE_WHITELIST_CHANNELS = 5;

    public AutoModInviteGuardSubcommand() {
        super("invite_guard", "Configures Discord invite filtering");
        addOption(new OptionData(OptionType.BOOLEAN, "enabled", "Whether invite guard should be enabled", true));
        addOption(new OptionData(OptionType.BOOLEAN, "clear_whitelist", "Clears the whitelist instead of replacing it", false));
        for (int index = 1; index <= MAX_INVITE_WHITELIST_CHANNELS; index++) {
            addOption(new OptionData(OptionType.CHANNEL, "channel_" + index,
                    "A channel where invite links are blocked", false));
        }
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildData config = requireConfig(event);
        if (config == null || event.getGuild() == null)
            return;

        boolean enabled = event.getOption("enabled", false, OptionMapping::getAsBoolean);
        boolean clearWhitelist = event.getOption("clear_whitelist", false, OptionMapping::getAsBoolean);

        List<Long> whitelist = new ArrayList<>();
        for (int index = 1; index <= MAX_INVITE_WHITELIST_CHANNELS; index++) {
            GuildChannel channel = event.getOption("channel_" + index, OptionMapping::getAsChannel);
            if (channel == null)
                continue;

            if (channel.getGuild().getIdLong() != event.getGuild().getIdLong()) {
                reply(event, "❌ All whitelist channels must belong to this server!", false, true);
                return;
            }

            whitelist.add(channel.getIdLong());
        }

        if (enabled && !clearWhitelist && whitelist.isEmpty()
                && GuildData.getLongs(config.getDiscordInviteWhitelistChannels()).isEmpty()) {
            reply(event, "❌ Invite guard needs at least one whitelist channel before it can be enabled.", false, true);
            return;
        }

        config.setDiscordInviteGuardEnabled(enabled);
        if (clearWhitelist) {
            config.setDiscordInviteWhitelistChannels("");
        } else if (!whitelist.isEmpty()) {
            config.setDiscordInviteWhitelistChannels(joinIds(whitelist));
        }

        if (!saveConfig(event.getGuild(), config)) {
            reply(event, "❌ Failed to update invite guard settings!", false, true);
            return;
        }

        reply(event, """
                ✅ Updated invite guard.
                Enabled: `%s`
                Whitelist: %s""".formatted(
                enabled,
                formatChannelList(event.getGuild(), GuildData.getLongs(config.getDiscordInviteWhitelistChannels()))), false, true);
    }
}
