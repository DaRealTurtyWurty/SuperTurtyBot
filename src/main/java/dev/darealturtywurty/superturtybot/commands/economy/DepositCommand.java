package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildConfig config) {
        Economy account = EconomyManager.getAccount(guild, event.getUser());
        if (account.getWallet() <= 0) {
            event.getHook().editOriginal("❌ Your wallet is empty, you cannot deposit!").queue();
            return;
        }

        int amount = event.getOption("amount", account.getWallet(), OptionMapping::getAsInt);
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
        event.getHook().editOriginal("✅ You have deposited %s%d into your bank!"
                        .formatted(config.getEconomyCurrency(), amount))
                .queue();
    }
}