package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
        return List.of(
                new OptionData(OptionType.INTEGER, "amount", "The amount of money to deposit.", false).setMinValue(0));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.getWallet() <= 0) {
            event.getHook().editOriginal("❌ Your wallet is empty, you cannot deposit!").queue();
            return;
        }

        long amount = event.getOption("amount", account.getWallet(), OptionMapping::getAsLong);
        if (amount <= 0) {
            event.getHook().editOriginal("❌ You must deposit at least %s1!".formatted(config.getEconomyCurrency())).queue();
            return;
        }

        if (amount > account.getWallet()) {
            event.getHook().editOriginal("❌ You cannot deposit more than you have in your wallet!").queue();
            return;
        }

        EconomyManager.deposit(account, amount);
        EconomyManager.updateAccount(account);
        event.getHook().editOriginal("✅ You have deposited %s%s into your bank!"
                        .formatted(config.getEconomyCurrency(), StringUtils.numberFormat(amount))).queue();
    }
}