package dev.darealturtywurty.superturtybot.commands.economy.marketplace;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.MarketplaceService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class MarketplaceBrowseSubcommand extends MarketplaceSubcommand {
    public MarketplaceBrowseSubcommand() {
        super("browse", "Browse current listings");
        addOption(new OptionData(OptionType.STRING, "type", "Filter by asset type", false)
                .addChoice("Collectable", MarketplaceService.COLLECTABLE)
                .addChoice("Rank card item", MarketplaceService.RANK_CARD)
                .addChoice("Economy item", MarketplaceService.ECONOMY_ITEM)
                .addChoice("Property rental", MarketplaceService.RENTAL));
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String type = event.getOption("type", OptionMapping::getAsString);
        var listings = MarketplaceService.browse(guild.getIdLong(), type, 15);
        event.getHook().editOriginalEmbeds(MarketplaceCommand.browse(listings, config).build()).queue();
    }
}
