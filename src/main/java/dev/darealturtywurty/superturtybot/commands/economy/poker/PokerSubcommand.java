package dev.darealturtywurty.superturtybot.commands.economy.poker;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.math.BigInteger;

abstract class PokerSubcommand extends SubcommandCommand {
    protected PokerSubcommand(String name, String description) {
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
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot gamble! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        execute(event, guild, account, config);
    }

    protected abstract void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config);

    protected static void applySettlement(Economy account, PokerCommand.Settlement settlement) {
        BigInteger payout = settlement.payout();
        BigInteger bet = settlement.bet();
        if (payout.signum() > 0) {
            EconomyManager.addMoney(account, payout, false);
        }

        BigInteger net = payout.subtract(bet);
        account.addTransaction(net, MoneyTransaction.POKER);
        if (net.signum() > 0) {
            EconomyManager.betWin(account, net);
        } else if (net.signum() < 0) {
            EconomyManager.betLoss(account, net.negate());
        }

        EconomyManager.updateAccount(account);
    }
}
