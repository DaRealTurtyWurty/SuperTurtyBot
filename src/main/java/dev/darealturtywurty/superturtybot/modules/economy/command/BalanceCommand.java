package dev.darealturtywurty.superturtybot.modules.economy.command;

import java.awt.Color;
import java.time.Instant;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class BalanceCommand extends EconomyCommand {
    public BalanceCommand() {
    }

    @Override
    public String getDescription() {
        return "Gets your economy balance for this server (both wallet and bank).";
    }

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getRichName() {
        return "Balance";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(EconomyManager.getBalance(account) > 0 ? Color.GREEN : Color.RED);
        embed.setTitle("Economy Balance for: " + event.getUser().getName());
        
        // TODO: Get currency symbol from guild config
        embed.setDescription("**Wallet:** <>%d%n**Bank:** <>%d%n**Total Balance:** <>%d%n".replace("<>", "$")
            .formatted(account.getWallet(), account.getBank(), EconomyManager.getBalance(account)));
        
        embed.setFooter(event.getUser().getName() + event.getUser().getDiscriminator(),
            event.getUser().getEffectiveAvatarUrl());
        
        reply(event, embed);
    }
}
