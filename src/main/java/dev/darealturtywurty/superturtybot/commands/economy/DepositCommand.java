package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.math.BigInteger;
import java.util.List;

public class DepositCommand extends EconomyCommand {
    @Override
    public String getDescription() {
        return "Allows you to deposit money into your bank account from your wallet.";
    }

    @Override
    public String getName() {
        return "deposit";
    }

    @Override
    public String getRichName() {
        return "Deposit";
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "amount", "The amount of money to deposit.", false));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.getWallet().signum() <= 0) {
            event.getHook().editOriginal("❌ Your wallet is empty, you cannot deposit!").queue();
            return;
        }

        BigInteger amount = event.getOption("amount", account.getWallet(), StringUtils.getAsBigInteger(event));
        if (amount.signum() <= 0) {
            event.getHook().editOriginal("❌ You must deposit at least %s1!".formatted(config.getEconomyCurrency())).queue();
            return;
        }

        if (amount.compareTo(account.getWallet()) > 0) {
            event.getHook().editOriginal("❌ You cannot deposit more than you have in your wallet!").queue();
            return;
        }

        EconomyManager.deposit(account, amount);
        EconomyManager.updateAccount(account);
        event.getHook().editOriginal("✅ You have deposited %s into your bank!\nYou now have %s in your bank.".formatted(
                StringUtils.numberFormat(amount, config),
                StringUtils.numberFormat(account.getBank(), config)
        )).queue();
    }
}