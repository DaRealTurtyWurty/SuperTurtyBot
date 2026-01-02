package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

abstract class PropertySubcommand extends SubcommandCommand {
    protected PropertySubcommand(String name, String description) {
        super(name, description);
    }

    @Override
    public final void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ This command can only be used in servers!", false, true);
            return;
        }

        event.deferReply().mentionRepliedUser(false).queue();

        GuildData config = GuildData.getOrCreateGuildData(guild);
        if (!config.isEconomyEnabled()) {
            event.getHook().sendMessage("❌ Economy is not enabled in this server!").queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot access your properties! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        execute(event, guild, account, config);
    }

    protected abstract void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config);
}
