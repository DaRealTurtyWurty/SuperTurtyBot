package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.PropertyRegistry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.math.BigInteger;

public class PropertyBuySubcommand extends PropertySubcommand {
    public PropertyBuySubcommand() {
        super("buy", "Buy a property");
        addOption(OptionType.STRING, "property", "The property to buy", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            PropertyCommand.hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property template = PropertyRegistry.getByName(propertyName);
        if (template == null) {
            PropertyCommand.hookReply(event, "❌ That property does not exist!");
            return;
        }

        PropertyCommand.OwnedProperty existingOwner = PropertyCommand.findOwnedProperty(guild.getIdLong(), template.getName());
        if (existingOwner != null) {
            if (existingOwner.account().getUser() == account.getUser()) {
                PropertyCommand.hookReply(event, "❌ You already own this property!");
            } else {
                PropertyCommand.hookReply(event, "❌ This property is already owned by someone else!");
            }

            return;
        }

        BigInteger totalCost = template.getOriginalPrice().add(template.getEstateTax());
        if (!EconomyManager.removeBalance(account, totalCost)) {
            PropertyCommand.hookReply(event, "❌ You need another %s to buy this property!".formatted(
                    StringUtils.numberFormat(totalCost.subtract(EconomyManager.getBalance(account)), config)));
            return;
        }

        Property property = PropertyRegistry.createForOwner(template, account.getUser());
        account.getProperties().add(property);
        account.addTransaction(totalCost.negate(), MoneyTransaction.PROPERTY);
        EconomyManager.updateAccount(account);

        PropertyCommand.hookReply(event, "✅ You bought %s for %s (including %s estate tax)!"
                .formatted(property.getName(),
                        StringUtils.numberFormat(template.getOriginalPrice(), config),
                        StringUtils.numberFormat(template.getEstateTax(), config)));
    }
}
