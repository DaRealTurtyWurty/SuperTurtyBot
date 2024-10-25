package dev.darealturtywurty.superturtybot.commands.economy.property;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.List;
import java.util.Optional;

public class BuyPropertyCommand extends SubcommandCommand {
    public BuyPropertyCommand(CoreCommand parent) {
        super(parent, "buy", "Buy a property");
        addOption(OptionType.STRING, "property", "The property to buy", true, true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String propertyName = event.getOption("property", null, OptionMapping::getAsString);
        if (propertyName == null) {
            reply(event, "You must provide a property to buy!");
            return;
        }

        event.deferReply().queue();

        Guild guild = event.getGuild();
        GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (guildData == null) {
            guildData = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(guildData);
        }

        Economy account = EconomyManager.getOrCreateAccount(event.getGuild(), event.getUser());
        if (!PropertyManager.canBuyProperty(account)) {
            event.getHook().sendMessageFormat("❌ You need at least %s10k in your bank account and have less than %s properties to buy a property!",
                    guildData.getEconomyCurrency(), (int) Math.floor(EconomyManager.getCreditScore(account) * 100)).queue();
            return;
        }

        Optional<Property> propertyOpt = PropertyManager.findProperty(guild.getIdLong(), propertyName);
        if (propertyOpt.isEmpty()) {
            event.getHook().sendMessage("❌ That property does not exist! To view the available properties, use `/property list`").queue();
            return;
        }

        Property property = propertyOpt.get();
        if (account.getProperties().contains(property)) {
            event.getHook().sendMessage("❌ You already own that property!").queue();
            return;
        }

        long cost = PropertyManager.calculatePropertyCost(property);
        if (account.getBank() < cost) {
            event.getHook().sendMessageFormat("❌ This property costs %s%s, you need another %s%s to buy it!",
                    guildData.getEconomyCurrency(), StringUtils.numberFormat(cost), guildData.getEconomyCurrency(), StringUtils.numberFormat(cost - account.getBank())).queue();
            return;
        }

        account.getProperties().add(property);
        account.setBank(account.getBank() - cost);
        Database.getDatabase().economy.updateOne(Filters.eq("user", account.getUser()), List.of(
                Updates.set("properties", account.getProperties()),
                Updates.set("bank", account.getBank())
        ));

        event.getHook().sendMessageFormat("✅ You have successfully bought the property `%s` for %s%s!",
                property.getName(), guildData.getEconomyCurrency(), StringUtils.numberFormat(cost)).queue();
    }

    @Override
    public void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
        if(!event.isFromGuild())
            return;

        AutoCompleteQuery query = event.getFocusedOption();
        if(!query.getName().equals("property"))
            return;

        String input = query.getValue();

        List<String> propertyNames = PropertyManager.getPropertyNames(event.getGuild().getIdLong());
        propertyNames = StringUtils.getMatching(propertyNames, input, true, false);
        propertyNames = StringUtils.closestMatches(propertyNames, input, 25);

        event.replyChoiceStrings(propertyNames).queue();
    }
}