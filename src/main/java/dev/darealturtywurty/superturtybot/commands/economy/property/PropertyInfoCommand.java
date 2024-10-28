package dev.darealturtywurty.superturtybot.commands.economy.property;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class PropertyInfoCommand extends SubcommandCommand {
    public PropertyInfoCommand(CoreCommand parent) {
        super(parent, "info", "Get information about a property you own.");
        addOption(OptionType.STRING, "property", "The property you want to get information about.", true, true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setContent("Loading property information...").queue();

        Guild guild = event.getGuild();
        Member member = event.getMember();

        Economy account = EconomyManager.getOrCreateAccount(guild, member.getUser());
        String propertyName = event.getOption("property", null, OptionMapping::getAsString);
        if (propertyName == null) {
            event.getHook().editOriginal("❌ You must provide a property name!").queue();
            return;
        }

        Optional<Property> propertyOpt = account.getProperties().stream()
                .filter(property -> property.getName().equalsIgnoreCase(propertyName))
                .findFirst();
        if (propertyOpt.isEmpty()) {
            event.getHook().editOriginalFormat("❌ You do not own a property with the name: **%s**!", propertyName).queue();
            return;
        }

        Property property = propertyOpt.get();

        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(config);
        }

        var builder = new EmbedBuilder()
                .setTimestamp(Instant.now())
                .setColor(member.getColorRaw())
                .setFooter("Requested by " + member.getEffectiveName(), member.getUser().getEffectiveAvatarUrl())
                .setTitle(property.getName())
                .setDescription(property.getDescription())
                .addField("Worth", config.getEconomyCurrency() + StringUtils.numberFormat(property.calculateCurrentWorth()), false)
                .addField("Original Price", config.getEconomyCurrency() + StringUtils.numberFormat(property.getOriginalPrice()), false)
                .addField("Mortgage", property.getMortgage().isPaidOff() ?
                        "Paid Off" :
                        (config.getEconomyCurrency() + StringUtils.numberFormat(property.getMortgage().calculateAmountLeftToPay())), false);

        if (property.isMortgaged() && !property.getMortgage().isPaidOff()) {
            builder.addField("Mortgage Interest", "%.02f%%".formatted(property.getMortgage().getInterestRate()), false);
        }

        builder.addField("Estate Tax", config.getEconomyCurrency() + StringUtils.numberFormat(property.getEstateTax()), false)
                .addField("Upgrade Details", "Current Level: " + property.getUpgradeLevel() +
                        "\nUpgrade cost: " + config.getEconomyCurrency() + StringUtils.numberFormat(property.getUpgradePriceForLevel()), false)
                .addField("Bought", TimeFormat.RELATIVE.format(property.getBuyDate()), false);

        if (property.hasRent() && !property.getRent().isPaused()) {
            builder.addField("Rent", config.getEconomyCurrency() + StringUtils.numberFormat(property.getRent().getCurrentRent()), false);
        }

        event.getHook().editOriginalEmbeds(builder.build()).queue();
    }

    @Override
    public void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
        if(!event.isFromGuild())
            return;

        AutoCompleteQuery query = event.getFocusedOption();
        if(!query.getName().equals("property"))
            return;

        String input = query.getValue();

        Economy account = EconomyManager.getOrCreateAccount(event.getGuild(), event.getUser());
        List<String> propertyNames = account.getProperties().stream()
                .map(Property::getName)
                .toList();

        propertyNames = StringUtils.getMatching(propertyNames, input, true, false);
        propertyNames = StringUtils.closestMatches(propertyNames, input, 25);

        event.replyChoiceStrings(propertyNames).queue();
    }
}
