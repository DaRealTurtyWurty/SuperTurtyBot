package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RewardCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("daily", "Claim your daily reward!"),
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        final String subcommand = event.getSubcommandName();
        if(subcommand == null) {
            event.getHook().editOriginal("❌ You need to specify a reward to claim!").queue();
            return;
        }

        switch (subcommand) {
            case "daily" -> {
                if (account.getNextDaily() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You can next claim your daily reward %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextDaily()))).queue();
                    return;
                }

                long dailyReward = 1000;
                if(account.getJob() != null)
                    dailyReward = EconomyManager.getPayAmount(account) * 2;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null)
                    dailyReward = (int) (dailyReward * 2.5f);

                EconomyManager.addMoney(account, dailyReward, true);
                account.setNextDaily(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
                EconomyManager.updateAccount(account);
                event.getHook().editOriginalFormat("✅ You claimed your daily reward of %s%s!",
                        config.getEconomyCurrency(), StringUtils.numberFormat(dailyReward)).queue();
            }

            case "weekly" -> {
                if (account.getNextWeekly() > System.currentTimeMillis()) {
                    event.getHook().editOriginal(
                                    "❌ You can next claim your weekly reward %s!"
                                            .formatted(TimeFormat.RELATIVE.format(account.getNextWeekly())))
                            .mentionRepliedUser(false).queue();
                    return;
                }

                long weeklyReward = 10000;
                if(account.getJob() != null)
                    weeklyReward = EconomyManager.getPayAmount(account) * 5;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null)
                    weeklyReward = (int) (weeklyReward * 2.5f);

                EconomyManager.addMoney(account, weeklyReward, true);
                account.setNextWeekly(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
                EconomyManager.updateAccount(account);
                event.getHook().editOriginalFormat("✅ You claimed your weekly reward of %s%s!",
                        config.getEconomyCurrency(), StringUtils.numberFormat(weeklyReward)).queue();
            }

            case "monthly" -> {
                if (account.getNextMonthly() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You can next claim your monthly reward %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextMonthly()))).queue();
                    return;
                }

                long monthlyReward = 50000;
                if(account.getJob() != null)
                    monthlyReward = EconomyManager.getPayAmount(account) * 10;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null)
                    monthlyReward = (int) (monthlyReward * 2.5f);

                EconomyManager.addMoney(account, monthlyReward, true);
                account.setNextMonthly(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31));
                EconomyManager.updateAccount(account);
                event.getHook().editOriginal("✅ You claimed your monthly reward of %s%s!"
                        .formatted(config.getEconomyCurrency(), StringUtils.numberFormat(monthlyReward))).queue();
            }

            case "yearly" -> {
                if (account.getNextYearly() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You can next claim your yearly reward %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextYearly()))).queue();
                    return;
                }

                long yearlyReward = 100000;
                if(account.getJob() != null)
                    yearlyReward = EconomyManager.getPayAmount(account) * 20;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null)
                    yearlyReward = (int) (yearlyReward * 2.5f);

                EconomyManager.addMoney(account, yearlyReward, true);
                account.setNextYearly(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365));
                EconomyManager.updateAccount(account);
                event.getHook().editOriginal("✅ You claimed your yearly reward of %s%s!"
                        .formatted(config.getEconomyCurrency(), StringUtils.numberFormat(yearlyReward))).queue();
            }
        }
    }
}