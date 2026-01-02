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
import java.util.concurrent.TimeUnit;

public class PropertyRentRerollSubcommand extends PropertySubcommand {
    public PropertyRentRerollSubcommand() {
        super("rent-reroll", "Re-roll renter offers for a fee");
        addOption(OptionType.STRING, "property", "The property to rent out", true, true);
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

        PropertyCommand.normalizeRent(property);
        if (property.isRentActive()) {
            PropertyCommand.hookReply(event, "❌ This property is already rented.");
            return;
        }

        if (property.getRent() == null || property.getRent().isPaused() || PropertyCommand.calculateRent(property).signum() <= 0) {
            PropertyCommand.hookReply(event, "❌ This property is not available to rent out.");
            return;
        }

        BigInteger rerollCost = PropertyCommand.calculateRerollCost(property);
        if (!EconomyManager.removeBalance(account, rerollCost)) {
            PropertyCommand.hookReply(event, "❌ You need another %s to re-roll offers!"
                    .formatted(StringUtils.numberFormat(rerollCost.subtract(EconomyManager.getBalance(account)), config)));
            return;
        }

        property.setRenterOffers(PropertyRentSubcommand.generateRenterOffers(property));
        property.setNextBestRenterAt(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24));
        account.addTransaction(rerollCost.negate(), MoneyTransaction.RENT);
        EconomyManager.updateAccount(account);

        PropertyCommand.hookReply(event, "✅ Re-rolled renter offers for %s."
                .formatted(property.getName()));
    }
}
