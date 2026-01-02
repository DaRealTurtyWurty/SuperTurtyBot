package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.Rent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.math.BigInteger;

public class PropertyResumeRentSubcommand extends PropertySubcommand {
    public PropertyResumeRentSubcommand() {
        super("resume-rent", "Resume renting a property");
        addOption(OptionType.STRING, "property", "The property to resume renting", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Property property = PropertyCommand.getOwnedProperty(account, event.getOption("property", OptionMapping::getAsString));
        if (property == null) {
            PropertyCommand.hookReply(event, "❌ You do not own that property!");
            return;
        }

        if (property.getRent() == null) {
            property.setRent(new Rent(BigInteger.ZERO));
        }

        property.getRent().resumeRent();
        EconomyManager.updateAccount(account);
        PropertyCommand.hookReply(event, "✅ Rent resumed for %s.".formatted(property.getName()));
    }
}
