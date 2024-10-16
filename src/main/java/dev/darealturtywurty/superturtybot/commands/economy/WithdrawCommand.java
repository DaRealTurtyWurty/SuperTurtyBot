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
        return List.of(
                new OptionData(OptionType.NUMBER, "amount", "The amount of money to withdraw.", true).setMinValue(0));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        long amount;
        try {
            amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        } catch (IllegalStateException | NumberFormatException exception) {
            event.getHook().editOriginal("❌ You must provide a valid amount to withdraw!").queue();
            return;
        }

        if (amount <= 0) {
            event.getHook().editOriginalFormat("❌ You must withdraw at least %s1!", config.getEconomyCurrency()).queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (amount > account.getBank()) {
            event.getHook().editOriginal("❌ You do not have enough money in your bank to withdraw that much!").queue();
            return;
        }

        EconomyManager.withdraw(account, amount);
        EconomyManager.updateAccount(account);
        event.getHook().editOriginalFormat("✅ You have withdrawn %s%s from your bank!",
                config.getEconomyCurrency(), StringUtils.numberFormat(amount)).queue();
    }
}