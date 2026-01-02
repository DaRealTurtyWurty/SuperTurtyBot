package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class PropertyTradeSubcommand extends PropertySubcommand {
    public PropertyTradeSubcommand() {
        super("trade", "Trade a property for another user's property");
        addOption(OptionType.USER, "user", "The user to trade with", true);
        addOption(OptionType.STRING, "your-property", "The property you want to offer", true, true);
        addOption(OptionType.STRING, "their-property", "The property you want from them", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Member otherMember = event.getOption("user", OptionMapping::getAsMember);
        if (otherMember == null) {
            PropertyCommand.hookReply(event, "❌ That user is not in this server!");
            return;
        }

        String yourPropertyName = event.getOption("your-property", OptionMapping::getAsString);
        String theirPropertyName = event.getOption("their-property", OptionMapping::getAsString);
        if (yourPropertyName == null || theirPropertyName == null) {
            PropertyCommand.hookReply(event, "❌ You must specify both properties to trade!");
            return;
        }

        Property yourProperty = PropertyCommand.getOwnedProperty(account, yourPropertyName);
        if (yourProperty == null) {
            PropertyCommand.hookReply(event, "❌ You do not own the property you want to trade!");
            return;
        }

        if (otherMember.getIdLong() == event.getUser().getIdLong()) {
            PropertyCommand.hookReply(event, "❌ You cannot trade with yourself!");
            return;
        }

        Economy otherAccount = EconomyManager.getOrCreateAccount(guild, otherMember.getUser());
        Property otherProperty = PropertyCommand.getOwnedProperty(otherAccount, theirPropertyName);
        if (otherProperty == null) {
            PropertyCommand.hookReply(event, "❌ That user does not own the property you want!");
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

        PropertyCommand.hookReply(event, "✅ Trade complete! You swapped %s with %s."
                .formatted(yourProperty.getName(), otherProperty.getName()));
    }
}
