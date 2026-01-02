package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.PropertyRegistry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

public class PropertyInfoSubcommand extends PropertySubcommand {
    public PropertyInfoSubcommand() {
        super("info", "Get info on a property");
        addOption(OptionType.STRING, "property", "The property to get info on", true, true);
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
            Property template = PropertyRegistry.getByName(propertyName);
            if (template == null) {
                PropertyCommand.hookReply(event, "❌ That property does not exist!");
                return;
            }

            PropertyCommand.hookReply(event, """
                    **%s**
                    Price: %s
                    Estate Tax: %s
                    Base Rent: %s
                    Description: %s
                    """.formatted(
                    template.getName(),
                    StringUtils.numberFormat(template.getOriginalPrice(), config),
                    StringUtils.numberFormat(template.getEstateTax(), config),
                    template.getRent() == null ? "None" : StringUtils.numberFormat(PropertyCommand.calculateRent(template), config),
                    template.getDescription()));
            return;
        }

        PropertyCommand.normalizeRent(property);
        PropertyCommand.hookReply(event, """
                **%s**
                Value: %s
                Rent Price: %s
                Renting: %s
                Upgrade Level: %d
                Estate Tax: %s
                Buy Date: %s
                Renter: %s
                Rent Ends: %s
                """.formatted(
                property.getName(),
                StringUtils.numberFormat(property.calculateCurrentWorth(), config),
                property.getRent() == null ? "None" : StringUtils.numberFormat(PropertyCommand.calculateRent(property), config),
                StringUtils.booleanToEmoji(property.isRentActive()),
                property.getUpgradeLevel(),
                StringUtils.numberFormat(property.getEstateTax(), config),
                TimeFormat.DATE_TIME_SHORT.format(property.getBuyDate()),
                PropertyCommand.formatRenter(property, guild),
                property.isRentActive() ? TimeFormat.RELATIVE.format(property.getRentEndsAt()) : "N/A"));
    }
}
