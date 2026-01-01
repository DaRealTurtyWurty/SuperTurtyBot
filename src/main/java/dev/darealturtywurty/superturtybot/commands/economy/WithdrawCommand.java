package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.math.BigInteger;
import java.util.List;

public class WithdrawCommand extends EconomyCommand {
    @Override
    public String getDescription() {
        return "Allows you to withdraw money from your bank into your wallet.";
    }

    @Override
    public String getName() {
        return "withdraw";
    }

    @Override
    public String getRichName() {
        return "Withdraw";
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "amount", "The amount of money to withdraw.", false));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot withdraw money! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        BigInteger amount = event.getOption("amount", BigInteger.ZERO.max(account.getWallet().negate()), StringUtils.getAsBigInteger(event));
        if (amount == null) {
            event.getHook().editOriginalFormat("❌ The amount provided is not a valid number!").queue();
            return;
        }

        if (amount.signum() <= 0) {
            event.getHook().editOriginalFormat("❌ You must withdraw at least %s1!", config.getEconomyCurrency()).queue();
            return;
        }

        if (amount.compareTo(account.getBank()) > 0) {
            event.getHook().editOriginal("❌ You do not have enough money in your bank to withdraw that much!").queue();
            return;
        }

        EconomyManager.withdraw(account, amount);
        EconomyManager.updateAccount(account);
        event.getHook().editOriginalFormat("✅ You have withdrawn %s from your bank!",
                StringUtils.numberFormat(amount, config)).queue();
    }
}