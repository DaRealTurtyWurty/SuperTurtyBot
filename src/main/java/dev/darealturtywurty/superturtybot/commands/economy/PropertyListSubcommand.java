package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Instant;
import java.util.List;

public class PropertyListSubcommand extends PropertySubcommand {
    public PropertyListSubcommand() {
        super("list", "List all properties");
        addOption(OptionType.USER, "user", "The user to list properties for", false);
        addOption(OptionType.BOOLEAN, "include-sold", "Include sold properties", false);
        addOption(OptionType.BOOLEAN, "include-rented", "Include rented properties", false);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Member member = event.getOption("user", event.getMember(), OptionMapping::getAsMember);
        if (member == null) {
            PropertyCommand.hookReply(event, "❌ That user is not in this server!");
            return;
        }

        boolean includeSold = event.getOption("include-sold", false, OptionMapping::getAsBoolean);
        boolean includeRented = event.getOption("include-rented", false, OptionMapping::getAsBoolean);

        if (member.getUser().isBot()) {
            PropertyCommand.hookReply(event, "❌ You cannot list properties for a bot!");
            return;
        }

        if (member.getUser().isSystem()) {
            PropertyCommand.hookReply(event, "❌ You cannot list properties for a system user!");
            return;
        }

        boolean isSelf = member.getUser().getIdLong() == event.getUser().getIdLong();
        Economy userAccount = isSelf ? account : EconomyManager.getOrCreateAccount(guild, member.getUser());

        List<Property> properties = userAccount.getProperties();
        if (properties.isEmpty()) {
            PropertyCommand.hookReply(event, isSelf ? "❌ You do not have any properties!" : "❌ That user does not have any properties!");
            return;
        }

        properties.removeIf(property -> !includeSold && !property.hasOwner());
        properties.removeIf(property -> !includeRented && property.isRentActive());

        if (properties.isEmpty()) {
            PropertyCommand.hookReply(event, isSelf ? "❌ You do not have any properties!" : "❌ That user does not have any properties!");
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
            PropertyCommand.normalizeRent(property);
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
                                    property.getRent() == null ? "None" : StringUtils.numberFormat(PropertyCommand.calculateRent(property), config),
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
                                    PropertyCommand.formatRenter(property, guild),
                                    property.isRentActive() ? TimeFormat.RELATIVE.format(property.getRentEndsAt()) : "N/A",
                                    property.isMortgaged() ? StringUtils.numberFormat(property.getMortgage().getAmount(), config) : "None"),
                    false);
        }

        embed.build(event.getJDA()).send(event.getHook());
    }
}
