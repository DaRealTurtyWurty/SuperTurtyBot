package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.math.BigInteger;
import java.util.List;

public class DonateCommand extends EconomyCommand {
    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "The user to donate money to.", true),
                new OptionData(OptionType.STRING, "amount", "The amount of money to donate.", true)
        );
    }

    @Override
    public String getDescription() {
        return "Donates money to a user.";
    }

    @Override
    public String getName() {
        return "donate";
    }

    @Override
    public String getRichName() {
        return "Donate";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {

        Member member = event.getOption("user", OptionMapping::getAsMember);
        if (member == null) {
            event.getHook().editOriginal("❌ You must provide a valid user in this server!").queue();
            return;
        }

        User user = member.getUser();

        if (user.isBot()) {
            event.getHook().editOriginal("❌ You cannot donate money to a bot!").queue();
            return;
        }

        if (user.getIdLong() == event.getUser().getIdLong()) {
            event.getHook().editOriginal("❌ You cannot donate money to yourself!").queue();
            return;
        }

        BigInteger amount = event.getOption("amount", StringUtils.getAsBigInteger(event));
        if (amount == null) return;
        if (amount.signum() <= 0) {
            event.getHook().editOriginal("❌ You must donate at least %s1!"
                    .formatted(config.getEconomyCurrency())).queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.getBank().compareTo(amount) < 0) {
            event.getHook().editOriginal("❌ You are missing %s!"
                    .formatted(StringUtils.numberFormat(amount.subtract(account.getBank()), config))).queue();
            return;
        }

        Economy otherAccount = EconomyManager.getOrCreateAccount(guild, user);

        EconomyManager.removeMoney(account, amount, true);
        EconomyManager.addMoney(otherAccount, amount);

        EconomyManager.updateAccount(account);
        EconomyManager.updateAccount(otherAccount);

        event.getHook().editOriginal("✅ %s has donated %s to %s!"
                        .formatted(event.getUser().getAsMention(),
                                StringUtils.numberFormat(amount, config),
                                member.getAsMention()))
                .queue();
    }
}
