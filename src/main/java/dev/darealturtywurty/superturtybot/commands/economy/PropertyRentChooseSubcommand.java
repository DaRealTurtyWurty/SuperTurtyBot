package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.RenterOffer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PropertyRentChooseSubcommand extends PropertySubcommand {
    public PropertyRentChooseSubcommand() {
        super("rent-choose", "Choose a renter offer for your property");
        addOption(OptionType.STRING, "property", "The property to rent out", true, true);
        addOption(OptionType.INTEGER, "offer", "The offer number to accept", true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            PropertyCommand.hookReply(event, "❌ You must specify a property!");
            return;
        }

        int offerIndex = event.getOption("offer", 0, OptionMapping::getAsInt);
        if (offerIndex <= 0) {
            PropertyCommand.hookReply(event, "❌ Offer must be 1 or higher.");
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

        List<RenterOffer> offers = property.getRenterOffers();
        if (offers == null || offers.isEmpty()) {
            PropertyCommand.hookReply(event, "❌ No renter offers available. Use `/property rent` to generate them.");
            return;
        }

        if (offerIndex > offers.size()) {
            PropertyCommand.hookReply(event, "❌ That offer does not exist. Choose 1-%d.".formatted(offers.size()));
            return;
        }

        RenterOffer offer = offers.get(offerIndex - 1);
        EconomyManager.addMoney(account, offer.getOffer(), true);
        account.addTransaction(offer.getOffer(), MoneyTransaction.RENT);

        property.setRenter(0L);
        property.setRenterName(offer.getName());
        property.setRentEndsAt(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
        property.setRenterOffers(new ArrayList<>());

        EconomyManager.updateAccount(account);

        PropertyCommand.hookReply(event, "✅ You rented out %s to %s for %s. Rental ends %s."
                .formatted(property.getName(),
                        offer.getName(),
                        StringUtils.numberFormat(offer.getOffer(), config),
                        TimeFormat.RELATIVE.format(property.getRentEndsAt())));
    }
}
