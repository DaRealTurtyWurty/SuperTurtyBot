package dev.darealturtywurty.superturtybot.commands.core.notifier;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public abstract class SingleGuildNotifierSubcommand extends BaseNotifierSubcommand {
    protected SingleGuildNotifierSubcommand(String name, String description) {
        super(name, description);
        addOption(discordChannelOption());
        addOption(mentionOption());
        addOption(unsubscribeOption());
    }

    protected abstract String existsMessage();

    protected abstract String subscribeMessage(long channelId);

    protected abstract String unsubscribeSuccessMessage();

    protected abstract String unsubscribeMissingMessage();

    protected abstract boolean exists(long guildId);

    protected abstract boolean delete(long guildId);

    protected abstract void insert(long guildId, long channelId, String mention);

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateManageServer(event))
            return;

        Guild guild = event.getGuild();
        if (guild == null)
            return;

        long guildId = guild.getIdLong();
        boolean unsubscribe = event.getOption("unsubscribe", false, OptionMapping::getAsBoolean);
        if (unsubscribe) {
            if (delete(guildId)) {
                reply(event, unsubscribeSuccessMessage());
            } else {
                reply(event, unsubscribeMissingMessage(), false, true);
            }

            return;
        }

        if (exists(guildId)) {
            reply(event, existsMessage(), false, true);
            return;
        }

        ChannelMentionContext context = requireChannelAndMention(event);
        if (context == null)
            return;

        insert(guildId, context.channelId(), context.mention());
        reply(event, subscribeMessage(context.channelId()));
    }
}
