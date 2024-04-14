package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Instant;
import java.util.List;

// TODO: Finish this command
public class PropertyCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("buy", "Buy a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to buy", true, true)),
                new SubcommandData("sell", "Sell a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to sell", true, true),
                        new OptionData(OptionType.INTEGER, "price", "The price to sell the property for", true)),
                new SubcommandData("list", "List all properties").addOptions(
                        new OptionData(OptionType.STRING, "user", "The user to list properties for", false),
                        new OptionData(OptionType.BOOLEAN, "include-sold", "Include sold properties", false),
                        new OptionData(OptionType.BOOLEAN, "include-rented", "Include rented properties", false)),
                new SubcommandData("info", "Get info on a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to get info on", true, true)),
                new SubcommandData("upgrade", "Upgrade a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to upgrade", true, true)),
                new SubcommandData("trade", "Trade a property for another user's property").addOptions(
                        new OptionData(OptionType.USER, "user", "The user to trade with", true),
                        new OptionData(OptionType.STRING, "property-to-trade", "The property to trade for", true, true)),
                new SubcommandData("rent", "Rent a property").addOptions(
                        new OptionData(OptionType.STRING, "property", "The property to rent", true, true),
                        new OptionData(OptionType.INTEGER, "price", "The price to rent the property for", true)),
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
                `/property sell <property> <price>`
                To list properties:
                `/property list [user] [include-sold] [include-rented]`
                To get info on a property:
                `/property info <property>`
                To upgrade a property:
                `/property upgrade <property>`
                To trade a property:
                `/property trade <user> <property-to-trade>`
                To rent a property:
                `/property rent <property> <price>`
                To stop renting a property:
                `/property stop-rent <property>`
                To pause renting a property:
                `/property pause-rent <property>`
                To resume renting a property:
                `/property resume-rent <property>`
                To set the rent price of a property:
                `/property set-rent <property> <price>
                """;
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

    }

    private void sellProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void listProperties(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        boolean includeSold = event.getOption("include-sold", false, OptionMapping::getAsBoolean);
        boolean includeRented = event.getOption("include-rented", false, OptionMapping::getAsBoolean);

        if (user == null) {
            user = event.getUser();
        }

        if (user.isBot()) {
            hookReply(event, "❌ You cannot list properties for a bot!");
            return;
        }

        if (user.isSystem()) {
            hookReply(event, "❌ You cannot list properties for a system user!");
            return;
        }

        guild.retrieveMember(user).queue(member -> {
            if (member == null) {
                hookReply(event, "❌ That user is not in this server!");
                return;
            }

            boolean isSelf = member.getIdLong() == event.getUser().getIdLong();

            Economy userAccount = account;
            if (!isSelf) {
                userAccount = EconomyManager.getOrCreateAccount(guild, member.getUser());
            }

            List<Property> properties = userAccount.getProperties();
            if (properties.isEmpty()) {
                hookReply(event, isSelf ? "❌ You do not have any properties!" : "❌ That user does not have any properties!");
                return;
            }

            properties.removeIf(property -> !includeSold && !property.hasOwner());
            properties.removeIf(property -> !includeRented && property.hasRent());

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
                contents.field(property.getName(), "Value: " + property.calculateCurrentWorth() + "\n" +
                        "Rent Price: " + property.getRent().getCurrentRent() + "\n" +
                        "Renting: " + property.hasRent() + "\n" +
                        "Sold: " + !property.hasOwner() + "\n" +
                        "Description: " + property.getDescription() + "\n" +
                        "Upgrade Level: " + property.getUpgradeLevel() + "\n" +
                        "Mortgaged: " + property.isMortgaged() + "\n" +
                        "Mortgage Paid Off: " + property.isPaidOff() + "\n" +
                        "Previous Owners: " + (property.hasPreviousOwners() ? property.getPreviousOwners().size() : 0) + "\n" +
                        "Estate Tax: " + property.getEstateTax() + "\n" +
                        "Buy Date: " + TimeFormat.DATE_TIME_SHORT.format(property.getBuyDate()) + "\n" +
                        "Owner: " + (property.hasOwner() ? owner == null ? "Unknown" : owner.getEffectiveName() : "None") + "\n" +
                        "Mortgage: " + (property.isMortgaged() ? property.getMortgage().getAmount() : "None"), false);
            }

            embed.build(event.getJDA()).send(event.getHook());
        }, ignored -> hookReply(event, "❌ That user is not in this server!"));
    }

    private void getPropertyInfo(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void upgradeProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void tradeProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void rentProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void stopRentingProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void pauseRentingProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void resumeRentingProperty(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private void setRentPrice(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {

    }

    private static void hookReply(SlashCommandInteractionEvent event, String message) {
        event.getHook().sendMessage(message).queue();
    }
}
