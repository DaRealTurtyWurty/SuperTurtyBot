package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Member member = event.getMember();
        if (member == null) {
            event.getHook().sendMessage("❌ You must be in a server to use this command!").queue();
            return;
        }

        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        String currency = config.getEconomyCurrency();

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(EconomyManager.getBalance(account) > 0 ? Color.GREEN : Color.RED);
        embed.setTitle("Economy Balance for: " + member.getEffectiveName());
        embed.setDescription("**Wallet:** %s%s%n**Bank:** %s%s%n**Total Balance:** %s%s%n"
                .formatted((account.getWallet() < 0 ? "-" : "") + currency,
                        StringUtils.numberFormat(Math.abs(account.getWallet())),
                        (account.getBank() < 0 ? "-" : "") + currency,
                        StringUtils.numberFormat(Math.abs(account.getBank())),
                        (EconomyManager.getBalance(account) < 0 ? "-" : "") + currency,
                        StringUtils.numberFormat(Math.abs(EconomyManager.getBalance(account)))));

        long betWins = account.getTotalBetWin();
        long betLosses = account.getTotalBetLoss();
        embed.addField("Bet Losses",
                currency + StringUtils.numberFormat(Math.abs(betLosses)),
                true);
        embed.addField("Bet Wins",
                currency + StringUtils.numberFormat(betWins),
                true);

        long betTotal = betWins - betLosses;
        embed.addField("Bet Total",
                (betTotal < 0 ? "-" : "+") + (currency + StringUtils.numberFormat(Math.abs(betTotal))),
                true);

        embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}