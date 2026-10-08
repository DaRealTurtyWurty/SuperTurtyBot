package dev.darealturtywurty.superturtybot.commands.economy.marketplace;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.economy.EconomyCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.MarketplaceListing;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.modules.economy.MarketplaceService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarketplaceCommand extends EconomyCommand {
    private static final Color ACCENT = new Color(74, 144, 226);

    public MarketplaceCommand() {
        addSubcommands(
            new MarketplaceBrowseSubcommand(),
            new MarketplaceListSubcommand(),
            new MarketplaceBuySubcommand(),
            new MarketplaceCancelSubcommand());
    }

    public static EmbedBuilder notice(String title, String description, boolean error) {
        return new EmbedBuilder().setColor(error ? new Color(219, 78, 78) : ACCENT)
            .setTitle(title).setDescription(description);
    }

    public static EmbedBuilder result(String title, MarketplaceListing listing, GuildData config) {
        var embed = new EmbedBuilder().setColor(ACCENT).setTitle(title)
            .addField("Item", displayItem(listing), true)
            .addField("Price", StringUtils.numberFormat(listing.getPrice(), config), true)
            .addField("Type", typeName(listing.getType()), true)
            .addField("Listing ID", "`" + listing.getId() + "`", false);
        if (MarketplaceService.RENTAL.equals(listing.getType())) {
            embed.addField("Rental period", listing.getDays() + " days", true);
        }

        return embed;
    }

    public static EmbedBuilder browse(List<MarketplaceListing> listings, GuildData config) {
        var embed = new EmbedBuilder().setColor(ACCENT).setTitle("Marketplace listings");
        if (listings.isEmpty())
            return embed.setDescription("No listings are available yet. Use `/marketplace list` to create one.");

        for (MarketplaceListing listing : listings) {
            String details = "**" + StringUtils.numberFormat(listing.getPrice(), config) + "** · "
                + typeName(listing.getType()) + " · <@" + listing.getSeller() + ">\n"
                + "ID: `" + listing.getId() + "`";
            if (MarketplaceService.RENTAL.equals(listing.getType())) {
                details += " · " + listing.getDays() + " days";
            }

            String item = displayItem(listing);
            embed.addField(item.length() > 256 ? item.substring(0, 253) + "..." : item, details, false);
        }

        return embed.setFooter("Use /marketplace buy and select a listing to purchase it.");
    }

    public static String displayItem(MarketplaceListing listing) {
        if (MarketplaceService.COLLECTABLE.equals(listing.getType()))
            return listing.getItem() + " · " + listing.getCollection().replace('_', ' ');

        if (MarketplaceService.ECONOMY_ITEM.equals(listing.getType()) && listing.getShopItem() != null)
            return listing.getShopItem().getName();

        return listing.getItem();
    }

    public static String typeName(String type) {
        return switch (type) {
            case MarketplaceService.COLLECTABLE -> "Collectable";
            case MarketplaceService.RANK_CARD -> "Rank card item";
            case MarketplaceService.ECONOMY_ITEM -> "Economy item";
            case MarketplaceService.RENTAL -> "Property rental";
            default -> "Item";
        };
    }

    @Override
    public String getName() {
        return "marketplace";
    }

    @Override
    public String getRichName() {
        return "Marketplace";
    }

    @Override
    public String getDescription() {
        return "Trade collectables, rank-card and economy items, and property rentals.";
    }

    @Override
    public String getHowToUse() {
        return """
            /marketplace browse
            /marketplace list <type> <item> <price> [collection] [days]
            /marketplace buy <id>
            /marketplace cancel <id>""";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        event.getHook().editOriginalEmbeds(notice("Marketplace",
            "Use `/marketplace browse` to view listings, `/marketplace list` to sell or rent, "
                + "`/marketplace buy` to purchase, or `/marketplace cancel` to withdraw your listing.",
            false).build()).queue();
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!getName().equals(event.getName()))
            return;

        Guild guild = event.getGuild();
        if (guild == null) {
            event.replyChoices().queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        String option = event.getFocusedOption().getName();
        String query = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
        try {
            List<Command.Choice> choices = switch (subcommand == null ? "" : subcommand) {
                case "list" -> listChoices(event, option);
                case "buy", "cancel" -> option.equals("id")
                    ? listingChoices(guild.getIdLong(),
                        subcommand.equals("cancel") ? event.getUser().getIdLong() : -1)
                    : List.of();
                default -> List.of();
            };
            event.replyChoices(choices.stream()
                .filter(choice -> query.isBlank() || choice.getName().toLowerCase(Locale.ROOT).contains(query)
                    || choice.getAsString().toLowerCase(Locale.ROOT).contains(query))
                .limit(25).toList()).queue();
        } catch (RuntimeException exception) {
            event.replyChoices().queue();
        }
    }

    private static List<Command.Choice> listChoices(CommandAutoCompleteInteractionEvent event, String option) {
        Database db = Database.getDatabase();
        long user = event.getUser().getIdLong();
        if (option.equals("collection")) {
            UserCollectables owned = db.userCollectables.find(Filters.eq("user", user)).first();
            if (owned == null || owned.getCollectables() == null)
                return List.of();
            return owned.getCollectables().stream()
                .filter(group -> group.getCollectables() != null && !group.getCollectables().isEmpty())
                .map(group -> new Command.Choice(group.getType().replace('_', ' '), group.getType()))
                .toList();
        }
        if (!option.equals("item"))
            return List.of();

        String type = event.getOption("type", OptionMapping::getAsString);
        if (type == null)
            return List.of();
        return switch (type) {
            case MarketplaceService.COLLECTABLE -> {
                UserCollectables owned = db.userCollectables.find(Filters.eq("user", user)).first();
                if (owned == null || owned.getCollectables() == null)
                    yield List.of();
                String selectedCollection = event.getOption("collection", OptionMapping::getAsString);
                var choices = new ArrayList<Command.Choice>();
                for (UserCollectables.Collectables group : owned.getCollectables()) {
                    if (selectedCollection != null && !selectedCollection.equals(group.getType()))
                        continue;
                    if (group.getCollectables() == null)
                        continue;
                    for (String item : group.getCollectables()) {
                        choices
                            .add(new Command.Choice(shorten(item + " · " + group.getType().replace('_', ' ')), item));
                    }
                }
                yield choices;
            }
            case MarketplaceService.RANK_CARD -> {
                var level = db.levelling.find(Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                    Filters.eq("user", user))).first();
                yield level == null || level.getInventory() == null
                    ? List.of()
                    : level.getInventory().stream().map(item -> new Command.Choice(shorten(item), item)).toList();
            }
            case MarketplaceService.ECONOMY_ITEM, MarketplaceService.RENTAL -> {
                var account = db.economy.find(Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                    Filters.eq("user", user))).first();
                if (account == null)
                    yield List.of();
                if (MarketplaceService.ECONOMY_ITEM.equals(type))
                    yield account.getShopItems() == null
                        ? List.of()
                        : account.getShopItems().stream()
                            .map(item -> new Command.Choice(shorten(item.getName() + " (#" + item.getId() + ")"),
                                String.valueOf(item.getId())))
                            .toList();
                yield account.getProperties() == null
                    ? List.of()
                    : account.getProperties().stream()
                        .filter(property -> property.getOwner() == user && !property.isRentActive()
                            && property.getRent() != null && !property.getRent().isPaused())
                        .map(property -> new Command.Choice(shorten(property.getName()), property.getName())).toList();
            }
            default -> List.of();
        };
    }

    private static List<Command.Choice> listingChoices(long guild, long seller) {
        var listings = seller == -1
            ? MarketplaceService.browse(guild, null, 25)
            : MarketplaceService.browseSeller(guild, seller, 25);
        return listings.stream()
            .map(listing -> new Command.Choice(shorten(displayItem(listing) + " · "
                + typeName(listing.getType())), listing.getId()))
            .toList();
    }

    private static String shorten(String value) {
        return value.length() <= 100 ? value : value.substring(0, 97) + "...";
    }
}
