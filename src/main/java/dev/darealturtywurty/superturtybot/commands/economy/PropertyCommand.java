package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PropertyCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("buy", "Buy a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to buy", true, true)),
                new SubcommandData("sell", "Sell a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to sell", true, true)),
                new SubcommandData("list", "List all properties").addOptions(
                        new OptionData(OptionType.USER, "user", "The user to list properties for", false),
                        new OptionData(OptionType.BOOLEAN, "include-sold", "Include sold properties", false),
                        new OptionData(OptionType.BOOLEAN, "include-rented", "Include rented properties", false)),
                new SubcommandData("info", "Get info on a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to get info on", true, true)),
                new SubcommandData("upgrade", "Upgrade a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to upgrade", true, true)),
                new SubcommandData("trade", "Trade a property for another user's property").addOptions(
                        new OptionData(OptionType.USER, "user", "The user to trade with", true),
                        new OptionData(OptionType.STRING, "your-property", "The property you want to offer", true, true),
                        new OptionData(OptionType.STRING, "their-property", "The property you want from them", true, true)),
                new SubcommandData("rent", "Rent a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to rent", true, true)),
                new SubcommandData("stop-rent", "Stop renting a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to stop renting", true, true)),
                new SubcommandData("pause-rent", "Pause renting a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to pause renting", true, true)),
                new SubcommandData("resume-rent", "Resume renting a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to resume renting", true, true)),
                new SubcommandData("set-rent", "Set the rent price of a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to set the rent price of", true, true),
                        new OptionData(OptionType.INTEGER, "price", "The price to set the rent price to", true)));
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
                To rent a property:
                `/property rent <property>`
                To stop renting a property:
                `/property stop-rent <property>`
                To pause renting a property:
                `/property pause-rent <property>`
                To resume renting a property:
                `/property resume-rent <property>`
                To set the rent price of a property:
                `/property set-rent <property> <price>`
                """;
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(getName())) return;
        if (event.getFocusedOption().getName().equalsIgnoreCase("property")
                || event.getFocusedOption().getName().equalsIgnoreCase("your-property")
                || event.getFocusedOption().getName().equalsIgnoreCase("their-property")) {
            event.replyChoices(PropertyRegistry.PROPERTIES.getRegistry().entrySet().stream()
                    .map(entry -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(
                            entry.getValue().getName(), entry.getKey()))
                    .toList()).queue();
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());

        switch (subcommand) {
            case "buy" -> buyProperty(event, guild, account, config);
            case "sell" -> sellProperty(event, guild, account, config);
            case "list" -> listProperties(event, guild, account, config);
            case "info" -> getPropertyInfo(event, guild, account, config);
            case "upgrade" -> upgradeProperty(event, guild, account, config);
            case "trade" -> tradeProperty(event, guild, account, config);
            case "rent" -> rentProperty(event, guild, account, config);
            case "stop-rent" -> stopRentingProperty(event, guild, account, config);
            case "pause-rent" -> pauseRentingProperty(event, guild, account, config);
            case "resume-rent" -> resumeRentingProperty(event, guild, account, config);
            case "set-rent" -> setRentPrice(event, guild, account, config);
        }
    }

    private void buyProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property template = PropertyRegistry.getByName(propertyName);
        if (template == null) {
            hookReply(event, "❌ That property does not exist!");
            return;
        }

        OwnedProperty existingOwner = findOwnedProperty(guild.getIdLong(), template.getName());
        if (existingOwner != null) {
            if (existingOwner.account().getUser() == account.getUser()) {
                hookReply(event, "❌ You already own this property!");
            } else {
                hookReply(event, "❌ This property is already owned by someone else!");
            }

            return;
        }

        BigInteger totalCost = template.getOriginalPrice().add(template.getEstateTax());
        if (!EconomyManager.removeBalance(account, totalCost)) {
            hookReply(event, "❌ You need another %s to buy this property!".formatted(
                    StringUtils.numberFormat(totalCost.subtract(EconomyManager.getBalance(account)), config)));
            return;
        }

        Property property = PropertyRegistry.createForOwner(template, account.getUser());
        account.getProperties().add(property);
        account.addTransaction(totalCost.negate(), MoneyTransaction.PROPERTY);
        EconomyManager.updateAccount(account);

        hookReply(event, "✅ You bought %s for %s (including %s estate tax)!"
                .formatted(property.getName(),
                        StringUtils.numberFormat(template.getOriginalPrice(), config),
                        StringUtils.numberFormat(template.getEstateTax(), config)));
    }

    private void sellProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property property = getOwnedProperty(account, propertyName);
        if (property == null) {
            hookReply(event, "❌ You do not own that property!");
            return;
        }

        BigInteger worth = property.calculateCurrentWorth();
        BigInteger payout = worth.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100));
        EconomyManager.addMoney(account, payout, true);
        account.addTransaction(payout, MoneyTransaction.PROPERTY);
        account.getProperties().remove(property);
        EconomyManager.updateAccount(account);

        hookReply(event, "✅ You sold %s for %s.".formatted(property.getName(), StringUtils.numberFormat(payout, config)));
    }

    private void listProperties(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Member member = event.getOption("user", event.getMember(), OptionMapping::getAsMember);
        if (member == null) {
            hookReply(event, "❌ That user is not in this server!");
            return;
        }

        boolean includeSold = event.getOption("include-sold", false, OptionMapping::getAsBoolean);
        boolean includeRented = event.getOption("include-rented", false, OptionMapping::getAsBoolean);

        if (member.getUser().isBot()) {
            hookReply(event, "❌ You cannot list properties for a bot!");
            return;
        }

        if (member.getUser().isSystem()) {
            hookReply(event, "❌ You cannot list properties for a system user!");
            return;
        }

        boolean isSelf = member.getUser().getIdLong() == event.getUser().getIdLong();

        Economy userAccount = isSelf ? account : EconomyManager.getOrCreateAccount(guild, member.getUser());

        List<Property> properties = userAccount.getProperties();
        if (properties.isEmpty()) {
            hookReply(event, isSelf ? "❌ You do not have any properties!" : "❌ That user does not have any properties!");
            return;
        }

        properties.removeIf(property -> !includeSold && !property.hasOwner());
        properties.removeIf(property -> !includeRented && property.isRentActive());

        if (properties.isEmpty()) {
            hookReply(event, isSelf ? "❌ You do not have any properties!" : "❌ That user does not have any properties!");
            return;
        }

        var contents = new PaginatedEmbed.ContentsBuilder();
        var embed = new PaginatedEmbed.Builder(5, contents)
                .title(isSelf ? "Your Properties" : member.getEffectiveName() + "'s Properties")
                .description(isSelf ? "You have " + properties.size() + " properties!" : member.getEffectiveName() + " has " + properties.size() + " properties!")
                .footer("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                .color(member.getColorRaw())
                .timestamp(Instant.now())
                .authorOnly(event.getUser().getIdLong());

        for (Property property : properties) {
            Member owner = guild.getMemberById(property.getOwner());
            normalizeRent(property);
            contents.field(property.getName(), """
                            Value: %s
                            Rent Price: %s
                            Renting: %s
                            Sold: %s
                            Description: %s
                            Upgrade Level: %d
                            Mortgaged: %s
                            Mortgage Paid Off: %s
                            Previous Owners: %d
                            Estate Tax: %s
                            Buy Date: %s
                            Owner: %s
                            Renter: %s
                            Rent Ends: %s
                            Mortgage: %s"""
                            .formatted(
                                    StringUtils.numberFormat(property.calculateCurrentWorth(), config),
                                    property.getRent() == null ? "None" : StringUtils.numberFormat(property.getRent().getCurrentRent(), config),
                                    StringUtils.booleanToEmoji(property.isRentActive()),
                                    StringUtils.booleanToEmoji(!property.hasOwner()),
                                    property.getDescription(),
                                    property.getUpgradeLevel(),
                                    StringUtils.booleanToEmoji(property.isMortgaged()),
                                    StringUtils.booleanToEmoji(property.isPaidOff()),
                                    property.hasPreviousOwners() ? property.getPreviousOwners().size() : 0,
                                    StringUtils.numberFormat(property.getEstateTax(), config),
                                    TimeFormat.DATE_TIME_SHORT.format(property.getBuyDate()),
                                    property.hasOwner() ? owner == null ? "Unknown" : owner.getAsMention() : "None",
                                    property.isRentActive() ? guild.getMemberById(property.getRenter()) == null ? "Unknown" :
                                            guild.getMemberById(property.getRenter()).getAsMention() : "None",
                                    property.isRentActive() ? TimeFormat.RELATIVE.format(property.getRentEndsAt()) : "N/A",
                                    property.isMortgaged() ? StringUtils.numberFormat(property.getMortgage().getAmount(), config) : "None"),
                    false);
        }

        embed.build(event.getJDA()).send(event.getHook());
    }

    private void getPropertyInfo(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property property = getOwnedProperty(account, propertyName);
        if (property == null) {
            Property template = PropertyRegistry.getByName(propertyName);
            if (template == null) {
                hookReply(event, "❌ That property does not exist!");
                return;
            }

            hookReply(event, """
                    **%s**
                    Price: %s
                    Estate Tax: %s
                    Base Rent: %s
                    Description: %s
                    """.formatted(
                    template.getName(),
                    StringUtils.numberFormat(template.getOriginalPrice(), config),
                    StringUtils.numberFormat(template.getEstateTax(), config),
                    template.getRent() == null ? "None" : StringUtils.numberFormat(template.getRent().getBaseRent(), config),
                    template.getDescription()));
            return;
        }

        normalizeRent(property);
        hookReply(event, """
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
                property.getRent() == null ? "None" : StringUtils.numberFormat(property.getRent().getCurrentRent(), config),
                StringUtils.booleanToEmoji(property.isRentActive()),
                property.getUpgradeLevel(),
                StringUtils.numberFormat(property.getEstateTax(), config),
                TimeFormat.DATE_TIME_SHORT.format(property.getBuyDate()),
                property.isRentActive() ? guild.getMemberById(property.getRenter()) == null ? "Unknown" :
                        guild.getMemberById(property.getRenter()).getAsMention() : "None",
                property.isRentActive() ? TimeFormat.RELATIVE.format(property.getRentEndsAt()) : "N/A"));
    }

    private void upgradeProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property property = getOwnedProperty(account, propertyName);
        if (property == null) {
            hookReply(event, "❌ You do not own that property!");
            return;
        }

        if (property.getUpgradePrices() == null || property.getUpgradePrices().isEmpty()
                || property.getUpgradeLevel() >= property.getUpgradePrices().size()) {
            hookReply(event, "❌ This property cannot be upgraded further!");
            return;
        }

        BigInteger upgradeCost = property.getUpgradePrice();
        if (!EconomyManager.removeBalance(account, upgradeCost)) {
            hookReply(event, "❌ You need another %s to upgrade this property!"
                    .formatted(StringUtils.numberFormat(upgradeCost.subtract(EconomyManager.getBalance(account)), config)));
            return;
        }

        property.setUpgradeLevel(property.getUpgradeLevel() + 1);
        account.addTransaction(upgradeCost.negate(), MoneyTransaction.PROPERTY);
        EconomyManager.updateAccount(account);

        hookReply(event, "✅ Upgraded %s to level %d for %s."
                .formatted(property.getName(), property.getUpgradeLevel(), StringUtils.numberFormat(upgradeCost, config)));
    }

    private void tradeProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Member otherMember = event.getOption("user", OptionMapping::getAsMember);
        if (otherMember == null) {
            hookReply(event, "❌ That user is not in this server!");
            return;
        }

        String yourPropertyName = event.getOption("your-property", OptionMapping::getAsString);
        String theirPropertyName = event.getOption("their-property", OptionMapping::getAsString);
        if (yourPropertyName == null || theirPropertyName == null) {
            hookReply(event, "❌ You must specify both properties to trade!");
            return;
        }

        Property yourProperty = getOwnedProperty(account, yourPropertyName);
        if (yourProperty == null) {
            hookReply(event, "❌ You do not own the property you want to trade!");
            return;
        }

        if (otherMember.getIdLong() == event.getUser().getIdLong()) {
            hookReply(event, "❌ You cannot trade with yourself!");
            return;
        }

        Economy otherAccount = EconomyManager.getOrCreateAccount(guild, otherMember.getUser());
        Property otherProperty = getOwnedProperty(otherAccount, theirPropertyName);
        if (otherProperty == null) {
            hookReply(event, "❌ That user does not own the property you want!");
            return;
        }

        account.getProperties().remove(yourProperty);
        otherAccount.getProperties().remove(otherProperty);

        yourProperty.setOwner(otherAccount.getUser());
        otherProperty.setOwner(account.getUser());

        account.getProperties().add(otherProperty);
        otherAccount.getProperties().add(yourProperty);

        EconomyManager.updateAccount(account);
        EconomyManager.updateAccount(otherAccount);

        hookReply(event, "✅ Trade complete! You swapped %s with %s."
                .formatted(yourProperty.getName(), otherProperty.getName()));
    }

    private void rentProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            hookReply(event, "❌ You must specify a property!");
            return;
        }

        OwnedProperty ownedProperty = findOwnedProperty(guild.getIdLong(), propertyName);
        if (ownedProperty == null) {
            hookReply(event, "❌ That property is not owned by anyone!");
            return;
        }

        Property property = ownedProperty.property();
        normalizeRent(property);
        if (property.getOwner() == account.getUser()) {
            hookReply(event, "❌ You cannot rent your own property!");
            return;
        }

        if (property.getRent() == null || property.getRent().isPaused() || property.getRent().getCurrentRent().signum() <= 0) {
            hookReply(event, "❌ This property is not available to rent.");
            return;
        }

        if (property.isRentActive()) {
            hookReply(event, "❌ This property is already rented.");
            return;
        }

        BigInteger rentCost = property.getRent().getCurrentRent();
        if (!EconomyManager.removeBalance(account, rentCost)) {
            hookReply(event, "❌ You need another %s to rent this property!"
                    .formatted(StringUtils.numberFormat(rentCost.subtract(EconomyManager.getBalance(account)), config)));
            return;
        }

        Economy ownerAccount = ownedProperty.account();
        EconomyManager.addMoney(ownerAccount, rentCost, true);
        ownerAccount.addTransaction(rentCost, MoneyTransaction.RENT);
        account.addTransaction(rentCost.negate(), MoneyTransaction.RENT);

        property.setRenter(account.getUser());
        property.setRentEndsAt(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));

        EconomyManager.updateAccount(account);
        EconomyManager.updateAccount(ownerAccount);

        hookReply(event, "✅ You rented %s for %s. Rental ends %s."
                .formatted(property.getName(),
                        StringUtils.numberFormat(rentCost, config),
                        TimeFormat.RELATIVE.format(property.getRentEndsAt())));
    }

    private void stopRentingProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property property = getOwnedProperty(account, propertyName);
        if (property == null) {
            hookReply(event, "❌ You do not own that property!");
            return;
        }

        property.clearRenter();
        EconomyManager.updateAccount(account);
        hookReply(event, "✅ Rent has been stopped for %s.".formatted(property.getName()));
    }

    private void pauseRentingProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Property property = getOwnedProperty(account, event.getOption("property", OptionMapping::getAsString));
        if (property == null) {
            hookReply(event, "❌ You do not own that property!");
            return;
        }

        if (property.getRent() == null) {
            property.setRent(new Rent(BigInteger.ZERO));
        }

        property.getRent().pauseRent();
        EconomyManager.updateAccount(account);
        hookReply(event, "✅ Rent paused for %s.".formatted(property.getName()));
    }

    private void resumeRentingProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Property property = getOwnedProperty(account, event.getOption("property", OptionMapping::getAsString));
        if (property == null) {
            hookReply(event, "❌ You do not own that property!");
            return;
        }

        if (property.getRent() == null) {
            property.setRent(new Rent(BigInteger.ZERO));
        }

        property.getRent().resumeRent();
        EconomyManager.updateAccount(account);
        hookReply(event, "✅ Rent resumed for %s.".formatted(property.getName()));
    }

    private void setRentPrice(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Property property = getOwnedProperty(account, event.getOption("property", OptionMapping::getAsString));
        if (property == null) {
            hookReply(event, "❌ You do not own that property!");
            return;
        }

        int amount = event.getOption("price", 0, OptionMapping::getAsInt);
        if (amount < 0) {
            hookReply(event, "❌ Rent price must be zero or more.");
            return;
        }

        if (property.getRent() == null) {
            property.setRent(new Rent(BigInteger.valueOf(amount)));
        } else {
            property.getRent().setRent(BigInteger.valueOf(amount));
        }

        EconomyManager.updateAccount(account);
        hookReply(event, "✅ Rent for %s set to %s."
                .formatted(property.getName(), StringUtils.numberFormat(BigInteger.valueOf(amount), config)));
    }

    private static void normalizeRent(Property property) {
        if (property == null || !property.hasRenter())
            return;

        if (property.getRentEndsAt() <= System.currentTimeMillis()) {
            property.clearRenter();
        }
    }

    private static Property getOwnedProperty(Economy account, String propertyName) {
        if (propertyName == null)
            return null;

        return account.getProperties().stream()
                .filter(property -> property.getName().equalsIgnoreCase(propertyName))
                .findFirst()
                .orElse(null);
    }

    private static OwnedProperty findOwnedProperty(long guildId, String propertyName) {
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

    private record OwnedProperty(Economy account, Property property) {
    }

    private static void hookReply(SlashCommandInteractionEvent event, String message) {
        event.getHook().sendMessage(message).queue();
    }
}
