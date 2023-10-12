package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;

public class BalanceCommand extends EconomyCommand {
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildConfig config) {
        final Economy account = EconomyManager.getAccount(guild, event.getUser());

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(EconomyManager.getBalance(account) > 0 ? Color.GREEN : Color.RED);
        embed.setTitle("Economy Balance for: " + event.getUser().getName());
        embed.setDescription(
                "**Wallet:** <>%d%n**Bank:** <>%d%n**Total Balance:** <>%d%n".replace("<>", config.getEconomyCurrency())
                        .formatted(account.getWallet(), account.getBank(), EconomyManager.getBalance(account)));
        embed.addField("Bet Losses", "%s%d".formatted(config.getEconomyCurrency(), account.getTotalBetLoss()), true);
        embed.addField("Bet Wins", "%s%d".formatted(config.getEconomyCurrency(), account.getTotalBetWin()), true);
        embed.addField("Bet Total", "%s%d".formatted(config.getEconomyCurrency(), account.getTotalBetWin() + account.getTotalBetLoss()), true);
        embed.setFooter(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}