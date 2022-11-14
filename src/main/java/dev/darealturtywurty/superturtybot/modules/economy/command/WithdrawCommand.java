package dev.darealturtywurty.superturtybot.modules.economy.command;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.command.EconomyCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

public class WithdrawCommand extends EconomyCommand {
    public WithdrawCommand() {
    }

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
        return List.of(
                new OptionData(OptionType.INTEGER, "amount", "The amount of money to withdraw.", true).setMinValue(0));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        int amount = Objects.requireNonNull(event.getOption("amount")).getAsInt();
        if (amount <= 0) {
            reply(event, "❌ You must withdraw at least <>1!".replace("<>", "$"), false, true);
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        if (amount > account.getBank()) {
            reply(event, "❌ You don't have enough money in your bank to withdraw that much!", false, true);
            return;
        }

        EconomyManager.withdraw(account, amount);
        EconomyManager.updateAccount(account);
        reply(event, "✅ You have withdrawn <>%d from your bank!".replace("<>", "$").formatted(amount));
    }
}
