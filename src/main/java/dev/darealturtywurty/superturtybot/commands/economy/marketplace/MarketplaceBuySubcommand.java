package dev.darealturtywurty.superturtybot.commands.economy.marketplace;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MarketplaceListing;
import dev.darealturtywurty.superturtybot.modules.economy.MarketplaceService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class MarketplaceBuySubcommand extends MarketplaceSubcommand {
    public MarketplaceBuySubcommand() {
        super("buy", "Buy a listing");
        addOption(OptionType.STRING, "id", "Choose a current listing", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String id = event.getOption("id", OptionMapping::getAsString);
        MarketplaceListing listing = MarketplaceService.buy(guild.getIdLong(), event.getUser().getIdLong(), id);
        event.getHook().editOriginalEmbeds(MarketplaceCommand.result("Purchase complete", listing, config)
                .setFooter("Seller paid and item transferred.").build()).queue();
    }
}
