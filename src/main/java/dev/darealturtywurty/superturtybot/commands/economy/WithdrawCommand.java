package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

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
                new OptionData(OptionType.INTEGER, "amount", "The amount of money to withdraw.", true).setMinValue(0));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        int amount = Objects.requireNonNull(event.getOption("amount")).getAsInt();
        if (amount <= 0) {
            event.getHook().editOriginal("❌ You must withdraw at least %s1!"
                    .formatted(config.getEconomyCurrency())).queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (amount > account.getBank()) {
            event.getHook().editOriginal("❌ You do not have enough money in your bank to withdraw that much!")
                    .queue();
            return;
        }

        EconomyManager.withdraw(account, amount);
        EconomyManager.updateAccount(account);
        event.getHook().editOriginal("✅ You have withdrawn %s%d from your bank!"
                .formatted(config.getEconomyCurrency(), amount)).queue();
    }
}