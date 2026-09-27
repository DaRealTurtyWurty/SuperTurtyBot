package dev.darealturtywurty.superturtybot.commands.economy.marketplace;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MarketplaceListing;
import dev.darealturtywurty.superturtybot.modules.economy.MarketplaceService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.math.BigInteger;

public class MarketplaceListSubcommand extends MarketplaceSubcommand {
    public MarketplaceListSubcommand() {
        super("list", "List an asset for sale or rent");
        addOption(new OptionData(OptionType.STRING, "type", "Kind of asset", true)
                .addChoice("Collectable", MarketplaceService.COLLECTABLE)
                .addChoice("Rank-card item", MarketplaceService.RANK_CARD)
                .addChoice("Economy item", MarketplaceService.ECONOMY_ITEM)
                .addChoice("Property rental", MarketplaceService.RENTAL));
        addOption(OptionType.STRING, "item", "Choose an item you own or a rentable property", true, true);
        addOption(OptionType.STRING, "price", "Positive price in this server's currency", true);
        addOption(OptionType.STRING, "collection", "Required for collectables; choose one you own", false, true);
        addOption(new OptionData(OptionType.INTEGER, "days", "Rental duration in days; defaults to 7", false)
                .setRequiredRange(1, 30));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String type = event.getOption("type", OptionMapping::getAsString);
        String item = event.getOption("item", OptionMapping::getAsString);
        String priceText = event.getOption("price", "0", OptionMapping::getAsString);
        String collection = event.getOption("collection", OptionMapping::getAsString);
        long requestedDays = event.getOption("days", 7L, OptionMapping::getAsLong);
        if (MarketplaceService.RENTAL.equals(type) && (requestedDays < 1 || requestedDays > 30))
            throw new IllegalArgumentException("Rental length must be between 1 and 30 days.");

        int days = MarketplaceService.RENTAL.equals(type) ? (int) requestedDays : 0;
        BigInteger price;
        try {
            price = new BigInteger(priceText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Price must be a whole number.");
        }
        MarketplaceListing listing = MarketplaceService.list(guild.getIdLong(), event.getUser().getIdLong(),
                type, collection, item, price, days);
        event.getHook().editOriginalEmbeds(MarketplaceCommand.result("Listing created", listing, config)
                .setFooter("Buyers can find this listing with /marketplace browse.").build()).queue();
    }
}
