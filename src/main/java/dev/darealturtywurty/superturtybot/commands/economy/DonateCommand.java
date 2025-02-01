package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DonateCommand extends EconomyCommand {
    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "The user to donate money to.", true),
                new OptionData(OptionType.INTEGER, "amount", "The amount of money to donate.", true).setMinValue(1)
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
        if (event.getOption("user") == null || event.getOption("amount") == null) {
            event.getHook().editOriginal("❌ You must provide a user and an amount!").queue();
            return;
        }

        User user = event.getOption("user", null, OptionMapping::getAsUser);
        if (user == null) {
            event.getHook().editOriginal("❌ You must provide a valid user!").queue();
            return;
        }

        CompletableFuture<Member> future = new CompletableFuture<>();
        guild.retrieveMember(user).queue(future::complete);

        if (user.isBot()) {
            event.getHook().editOriginal("❌ You cannot donate money to a bot!").queue();
            return;
        }

        if (user.getIdLong() == event.getUser().getIdLong()) {
            event.getHook().editOriginal("❌ You cannot donate money to yourself!").queue();
            return;
        }

        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        if (amount < 1) {
            event.getHook().editOriginal("❌ You must donate at least %s1!"
                    .formatted(config.getEconomyCurrency())).queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.getBank() < amount) {
            event.getHook().editOriginal("❌ You are missing %s%d!"
                    .formatted(config.getEconomyCurrency(), amount - account.getBank())).queue();
            return;
        }

        future.thenAccept(member -> {
            if (member == null) {
                event.getHook().editOriginal("❌ You must provide a valid user in this server!").queue();
                return;
            }

            Economy otherAccount = EconomyManager.getOrCreateAccount(guild, user);

            EconomyManager.removeMoney(account, amount, true);
            account.addTransaction(-amount, MoneyTransaction.DONATE, otherAccount.getUser());

            EconomyManager.addMoney(otherAccount, amount);
            otherAccount.addTransaction(amount, MoneyTransaction.DONATE, account.getUser());

            EconomyManager.updateAccount(account);
            EconomyManager.updateAccount(otherAccount);

            event.getHook().editOriginal("✅ Donated %s%s to %s!"
                            .formatted(config.getEconomyCurrency(), StringUtils.numberFormat(amount), user.getAsMention()))
                    .setAllowedMentions(List.of())
                    .queue(ignored -> {
                        MessageChannelUnion channel = event.getChannel();
                        channel.sendMessage("%s has donated %s%s to %s!"
                                        .formatted(event.getUser().getAsMention(),
                                                config.getEconomyCurrency(), StringUtils.numberFormat(amount),
                                                user.getAsMention()))
                                .queue();
                    });
        });
    }
}
