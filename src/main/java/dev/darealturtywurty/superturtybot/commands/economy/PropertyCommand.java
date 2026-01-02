package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.PropertyRegistry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class PropertyCommand extends EconomyCommand {
    public PropertyCommand() {
        addSubcommands(
                new PropertyBuySubcommand(),
                new PropertySellSubcommand(),
                new PropertyListSubcommand(),
                new PropertyInfoSubcommand(),
                new PropertyUpgradeSubcommand(),
                new PropertyTradeSubcommand(),
                new PropertyRentSubcommand(),
                new PropertyRentChooseSubcommand(),
                new PropertyRentRerollSubcommand(),
                new PropertyStopRentSubcommand(),
                new PropertyPauseRentSubcommand(),
                new PropertyResumeRentSubcommand()
        );
    }

    @Override
    public String getDescription() {
        return "Manage your properties!";
    }

    @Override
    public String getName() {
        return "property";
    }

    @Override
    public String getRichName() {
        return "Property";
    }

    @Override
    public String getHowToUse() {
        return """
                To buy a property:
                `/property buy <property>`
                To sell a property:
                `/property sell <property>`
                To list properties:
                `/property list [user] [include-sold] [include-rented]`
                To get info on a property:
                `/property info <property>`
                To upgrade a property:
                `/property upgrade <property>`
                To trade a property:
                `/property trade <user> <your-property> <their-property>`
                To view renter offers for your property:
                `/property rent <property>`
                To choose a renter offer:
                `/property rent-choose <property> <offer>`
                To re-roll renter offers for a fee:
                `/property rent-reroll <property>`
                Offers refresh automatically after 24h if unchosen.
                To stop renting out a property:
                `/property stop-rent <property>`
                To pause renting a property:
                `/property pause-rent <property>`
                To resume renting a property:
                `/property resume-rent <property>`
                """;
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(getName()))
            return;

        if (event.getFocusedOption().getName().equalsIgnoreCase("property")
                || event.getFocusedOption().getName().equalsIgnoreCase("your-property")
                || event.getFocusedOption().getName().equalsIgnoreCase("their-property")) {
            event.replyChoices(PropertyRegistry.PROPERTIES.getRegistry().entrySet().stream()
                    .map(entry -> new Command.Choice(
                            entry.getValue().getName(), entry.getKey()))
                    .toList()).queue();
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
        }
    }

    static void normalizeRent(Property property) {
        if (property == null || !property.hasRenter())
            return;

        if (property.getRentEndsAt() <= System.currentTimeMillis()) {
            property.clearRenter();
        }
    }

    static Property getOwnedProperty(Economy account, String propertyName) {
        if (propertyName == null)
            return null;

        return account.getProperties().stream()
                .filter(property -> property.getName().equalsIgnoreCase(propertyName))
                .findFirst()
                .orElse(null);
    }

    static OwnedProperty findOwnedProperty(long guildId, String propertyName) {
        if (propertyName == null)
            return null;

        List<Economy> accounts = Database.getDatabase().economy
                .find(com.mongodb.client.model.Filters.eq("guild", guildId))
                .into(new ArrayList<>());
        for (Economy economy : accounts) {
            for (Property property : economy.getProperties()) {
                if (!property.getName().equalsIgnoreCase(propertyName))
                    continue;

                normalizeRent(property);
                if (property.hasOwner())
                    return new OwnedProperty(economy, property);
            }
        }

        return null;
    }

    record OwnedProperty(Economy account, Property property) {
    }

    static String formatRenter(Property property, Guild guild) {
        if (!property.isRentActive())
            return "None";

        if (property.getRenterName() != null && !property.getRenterName().isBlank())
            return property.getRenterName();

        Member renterMember = guild.getMemberById(property.getRenter());
        return renterMember == null ? "Unknown" : renterMember.getAsMention();
    }

    static BigInteger calculateRerollCost(Property property) {
        BigInteger rent = calculateRent(property);
        if (rent.signum() <= 0)
            return BigInteger.ZERO;

        BigInteger cost = rent.multiply(BigInteger.valueOf(10)).divide(BigInteger.valueOf(100));
        return cost.signum() == 0 ? BigInteger.ONE : cost;
    }

    static BigInteger calculateRent(Property property) {
        if (property == null || property.getRent() == null)
            return BigInteger.ZERO;

        BigInteger originalPrice = property.getOriginalPrice();
        BigInteger baseRent = property.getRent().getBaseRent();
        if (originalPrice == null || baseRent == null || originalPrice.signum() <= 0)
            return BigInteger.ZERO;

        BigInteger upgradeValue = BigInteger.ZERO;
        if (property.getUpgradePrices() != null && !property.getUpgradePrices().isEmpty()) {
            int level = Math.min(property.getUpgradeLevel(), property.getUpgradePrices().size());
            for (int i = 0; i < level; i++) {
                upgradeValue = upgradeValue.add(property.getUpgradePrices().get(i));
            }
        }

        BigInteger adjustedValue = originalPrice.add(upgradeValue);
        BigDecimal ratio = new BigDecimal(baseRent).divide(new BigDecimal(originalPrice), 8, RoundingMode.HALF_UP);
        BigInteger rent = new BigDecimal(adjustedValue).multiply(ratio).toBigInteger();
        if (rent.signum() == 0 && baseRent.signum() > 0)
            return BigInteger.ONE;

        return rent;
    }

    static void hookReply(SlashCommandInteractionEvent event, String message) {
        event.getHook().sendMessage(message).queue();
    }
}
