package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.math.BigInteger;

public class PropertySellSubcommand extends PropertySubcommand {
    public PropertySellSubcommand() {
        super("sell", "Sell a property");
        addOption(OptionType.STRING, "property", "The property to sell", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            PropertyCommand.hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property property = PropertyCommand.getOwnedProperty(account, propertyName);
        if (property == null) {
            PropertyCommand.hookReply(event, "❌ You do not own that property!");
            return;
        }

        BigInteger worth = property.calculateCurrentWorth();
        BigInteger payout = worth.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100));
        EconomyManager.addMoney(account, payout, true);
        account.addTransaction(payout, MoneyTransaction.PROPERTY);
        account.getProperties().remove(property);
        EconomyManager.updateAccount(account);

        PropertyCommand.hookReply(event, "✅ You sold %s for %s."
                .formatted(property.getName(), StringUtils.numberFormat(payout, config)));
    }
}
