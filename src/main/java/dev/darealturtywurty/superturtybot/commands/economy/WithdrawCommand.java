package dev.darealturtywurty.superturtybot.commands.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", event.getGuild().getId()))
                .first();
        if (config == null) {
            config = new GuildConfig(event.getGuild().getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        event.deferReply().queue();

        int amount = Objects.requireNonNull(event.getOption("amount")).getAsInt();
        if (amount <= 0) {
            event.getHook().editOriginal("❌ You must withdraw at least %s1!"
                    .formatted(config.getEconomyCurrency())).queue();
            return;
        }

        Economy account = EconomyManager.getAccount(event.getGuild(), event.getUser());
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