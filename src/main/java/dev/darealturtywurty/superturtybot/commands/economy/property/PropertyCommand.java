package dev.darealturtywurty.superturtybot.commands.economy.property;

import dev.darealturtywurty.superturtybot.commands.economy.EconomyCommand;

// TODO: Finish this command
public class PropertyCommand extends EconomyCommand {
    public PropertyCommand() {
        addSubcommands(new BuyPropertyCommand(this), new ListPropertiesCommand(this), new PropertyInfoCommand(this));
    }

//    @Override
//    public List<SubcommandData> createSubcommandData() {
//        return List.of(
//                new SubcommandData("sell", "Sell a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to sell", true, true),
//                        new OptionData(OptionType.INTEGER, "price", "The price to sell the property for", true)),
//                new SubcommandData("list", "List all properties").addOptions(
//                        new OptionData(OptionType.STRING, "user", "The user to list properties for", false),
//                        new OptionData(OptionType.BOOLEAN, "include-sold", "Include sold properties", false),
//                        new OptionData(OptionType.BOOLEAN, "include-rented", "Include rented properties", false)),
//                new SubcommandData("info", "Get info on a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to get info on", true, true)),
//                new SubcommandData("upgrade", "Upgrade a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to upgrade", true, true)),
//                new SubcommandData("trade", "Trade a property for another user's property").addOptions(
//                        new OptionData(OptionType.USER, "user", "The user to trade with", true),
//                        new OptionData(OptionType.STRING, "property-to-trade", "The property to trade for", true, true)),
//                new SubcommandData("rent", "Rent a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to rent", true, true),
//                        new OptionData(OptionType.INTEGER, "price", "The price to rent the property for", true)),
//                new SubcommandData("stop-rent", "Stop renting a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to stop renting", true, true)),
//                new SubcommandData("pause-rent", "Pause renting a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to pause renting", true, true)),
//                new SubcommandData("resume-rent", "Resume renting a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to resume renting", true, true)),
//                new SubcommandData("set-rent", "Set the rent price of a property").addOptions(
//                        new OptionData(OptionType.STRING, "property", "The property to set the rent price of", true, true),
//                        new OptionData(OptionType.INTEGER, "price", "The price to set the rent price to", true)));
//    }

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
                To sell a property:
                `/property sell <property> <price>`
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
}
