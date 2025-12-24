package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

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
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.BOOLEAN, "detailed", "Whether to show detailed information", false));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Member member = event.getMember();
        if (member == null) {
            event.getHook().sendMessage("❌ You must be in a server to use this command!").queue();
            return;
        }

        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());

        boolean detailed = event.getOption("detailed") != null && event.getOption("detailed").getAsBoolean();

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(EconomyManager.getBalance(account).signum() > 0 ? Color.GREEN : Color.RED);
        embed.setTitle("Economy Balance for: " + member.getEffectiveName());
        if (!detailed) {
            embed.setDescription("**Wallet:** %s%n**Bank:** %s%n**Total Balance:** %s%n".formatted(
                    StringUtils.numberFormat(account.getWallet(), config),
                    StringUtils.numberFormat(account.getBank(), config),
                    StringUtils.numberFormat(EconomyManager.getBalance(account), config)));
        } else {
            embed.setDescription("**Wallet:** %s%n**Bank:** %s%n**Total Balance:** %s%n".formatted(
                    StringUtils.numberFormatExact(account.getWallet(), config),
                    StringUtils.numberFormatExact(account.getBank(), config),
                    StringUtils.numberFormatExact(EconomyManager.getBalance(account), config)));
        }

        BigInteger betWins = account.getTotalBetWin();
        BigInteger betLosses = account.getTotalBetLoss();
        embed.addField("Bet Losses",
                !detailed ? StringUtils.numberFormat(betLosses.abs(), config) : StringUtils.numberFormatExact(betLosses.abs(), config),
                true);
        embed.addField("Bet Wins",
                !detailed ? StringUtils.numberFormat(betWins, config) : StringUtils.numberFormatExact(betWins, config),
                true);

        BigInteger betTotal = betWins.subtract(betLosses);
        embed.addField("Bet Total",
                !detailed? (betTotal.signum() < 0 ? "-" : "+") + StringUtils.numberFormat(betTotal.abs(), config) :
                        (betTotal.signum() < 0 ? "-" : "+") + StringUtils.numberFormatExact(betTotal.abs(), config),
                true);

        embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}