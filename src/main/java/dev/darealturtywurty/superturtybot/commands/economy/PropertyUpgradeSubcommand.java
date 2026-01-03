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
import java.util.List;

public class PropertyUpgradeSubcommand extends PropertySubcommand {
    public PropertyUpgradeSubcommand() {
        super("upgrade", "Upgrade a property");
        addOption(OptionType.STRING, "property", "The property to upgrade", true, true);
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

        List<BigInteger> upgradePrices = property.getUpgradePrices();
        int upgradeLevel = property.getUpgradeLevel();
        if (upgradePrices == null || upgradePrices.isEmpty() || upgradeLevel < 0 || upgradeLevel >= upgradePrices.size()) {
            PropertyCommand.hookReply(event, "❌ This property cannot be upgraded further!");
            return;
        }

        BigInteger upgradeCost = upgradePrices.get(upgradeLevel);
        if (!EconomyManager.removeBalance(account, upgradeCost)) {
            PropertyCommand.hookReply(event, "❌ You need another %s to upgrade this property!"
                    .formatted(StringUtils.numberFormat(upgradeCost.subtract(EconomyManager.getBalance(account)), config)));
            return;
        }

        property.setUpgradeLevel(property.getUpgradeLevel() + 1);
        account.addTransaction(upgradeCost.negate(), MoneyTransaction.PROPERTY);
        EconomyManager.updateAccount(account);

        PropertyCommand.hookReply(event, "✅ Upgraded %s to level %d for %s."
                .formatted(property.getName(), property.getUpgradeLevel(), StringUtils.numberFormat(upgradeCost, config)));
    }
}
