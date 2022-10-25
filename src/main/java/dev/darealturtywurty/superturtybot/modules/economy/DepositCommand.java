package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class DepositCommand extends EconomyCommand {
    public DepositCommand() {
    }

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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        if (account.getWallet() <= 0) {
            reply(event, "❌ Your wallet is empty, you cannot deposit!", false, true);
            return;
        }

        int amount = event.getOption("amount", account.getWallet(), OptionMapping::getAsInt);
        if (amount <= 0) {
            reply(event, "❌ You must deposit at least <>1!".replace("<>", "$"), false, true);
            return;
        }

        if (amount > account.getWallet()) {
            reply(event, "❌ You cannot deposit more than you have in your wallet!", false, true);
            return;
        }

        EconomyManager.deposit(account, amount);
        EconomyManager.updateAccount(account);
        reply(event, "✅ You have deposited <>%d into your bank!".replace("<>", "$").formatted(amount));
    }
}