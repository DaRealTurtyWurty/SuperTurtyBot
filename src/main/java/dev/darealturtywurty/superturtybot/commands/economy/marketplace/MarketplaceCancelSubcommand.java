package dev.darealturtywurty.superturtybot.commands.economy.marketplace;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MarketplaceListing;
import dev.darealturtywurty.superturtybot.modules.economy.MarketplaceService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class MarketplaceCancelSubcommand extends MarketplaceSubcommand {
    public MarketplaceCancelSubcommand() {
        super("cancel", "Cancel your listing");
        addOption(OptionType.STRING, "id", "Choose one of your listings", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String id = event.getOption("id", OptionMapping::getAsString);
        MarketplaceListing listing = MarketplaceService.cancel(guild.getIdLong(), event.getUser().getIdLong(), id);
        event.getHook().editOriginalEmbeds(MarketplaceCommand.result("Listing cancelled", listing, config)
            .setFooter(MarketplaceService.RENTAL.equals(listing.getType())
                ? "The rental offer is no longer available."
                : "The item has been returned to your inventory.")
            .build()).queue();
    }
}
