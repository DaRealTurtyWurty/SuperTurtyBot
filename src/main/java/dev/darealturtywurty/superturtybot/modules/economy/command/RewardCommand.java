package dev.darealturtywurty.superturtybot.modules.economy.command;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;
import java.util.Objects;

public class RewardCommand extends EconomyCommand {

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("daily", "Claim your daily reward!"),
                new SubcommandData("weekly", "Claim your weekly reward!"),
                new SubcommandData("monthly", "Claim your monthly reward!"),
                new SubcommandData("yearly", "Claim your yearly reward!"));
    }

    @Override
    public String getDescription() {
        return "Claim your daily, weekly, monthly, and yearly rewards!";
    }

    @Override
    public String getName() {
        return "reward";
    }

    @Override
    public String getRichName() {
        return "Reward";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        final var subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "daily" -> {
                if (account.getNextDaily() > System.currentTimeMillis()) {
                    reply(event, "❌ You must wait until " + account.getNextDaily() + " to claim your daily reward!",
                            false, true);
                    return;
                }

                int dailyReward = 100;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) dailyReward += 50;

                EconomyManager.addMoney(account, dailyReward, true);
                account.setNextDaily(System.currentTimeMillis() + 86400000L);
                EconomyManager.updateAccount(account);
                reply(event, "✅ You claimed your daily reward of " + dailyReward + "!");
            }

            case "weekly" -> {
                if (account.getNextWeekly() > System.currentTimeMillis()) {
                    reply(event, "❌ You must wait until " + account.getNextWeekly() + " to claim your weekly reward!");
                    return;
                }

                int weeklyReward = 250;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) weeklyReward += 50;

                EconomyManager.addMoney(account, weeklyReward, true);
                account.setNextWeekly(System.currentTimeMillis() + 8400000L * 27);
                EconomyManager.updateAccount(account);
                reply(event, "✅ You claimed your weekly reward of " + weeklyReward + "!");
            }

            case "monthly" -> {
                if (account.getNextMonthly() > System.currentTimeMillis()) {
                    reply(event,
                            "❌ You must wait until " + account.getNextMonthly() + " to claim your monthly reward!");
                    return;
                }

                int monthlyReward = 500;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) monthlyReward += 50;

                EconomyManager.addMoney(account, monthlyReward, true);
                account.setNextMonthly(System.currentTimeMillis() + 8400000L * 27 * 31);
                EconomyManager.updateAccount(account);
                reply(event, "✅ You claimed your monthly reward of " + monthlyReward + "!");
            }

            case "yearly" -> {
                if (account.getNextYearly() > System.currentTimeMillis()) {
                    reply(event, "❌ You must wait until " + account.getNextYearly() + " to claim your yearly reward!");
                    return;
                }

                int yearlyReward = 750;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) yearlyReward += 50;

                EconomyManager.addMoney(account, yearlyReward, true);
                account.setNextYearly(System.currentTimeMillis() + 8400000L * 27 * 31 * 12);
                EconomyManager.updateAccount(account);
                reply(event, "✅ You claimed your yearly reward of " + yearlyReward + "!");
            }
        }
    }
}
