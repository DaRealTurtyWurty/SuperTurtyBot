package dev.darealturtywurty.superturtybot.modules.economy.command;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.List;
import java.util.Objects;

public class RewardCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommands() {
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        GuildConfig config = Database.getDatabase().guildConfig.find(
                Filters.eq("guild", event.getGuild().getIdLong())).first();
        if (config == null) {
            config = new GuildConfig(event.getGuild().getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        event.deferReply().queue();

        Economy account = EconomyManager.getAccount(event.getGuild(), event.getUser());
        final var subcommand = event.getSubcommandName();
        switch (Objects.requireNonNull(subcommand)) {
            case "daily" -> {
                if (account.getNextDaily() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You can next claim your daily reward %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextDaily()))).queue();
                    return;
                }

                int dailyReward = 100;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) dailyReward += 50;

                EconomyManager.addMoney(account, dailyReward, true);
                account.setNextDaily(System.currentTimeMillis() + 86400000L);
                EconomyManager.updateAccount(account);
                event.getHook().editOriginal("✅ You claimed your daily reward of %s%d!"
                        .formatted(config.getEconomyCurrency(), dailyReward)).queue();
            }

            case "weekly" -> {
                if (account.getNextWeekly() > System.currentTimeMillis()) {
                    reply(event,
                            "❌ You can next claim your weekly reward %s!"
                                    .formatted(TimeFormat.RELATIVE.format(account.getNextWeekly())),
                            false, true);
                    return;
                }

                int weeklyReward = 250;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) weeklyReward += 50;

                EconomyManager.addMoney(account, weeklyReward, true);
                account.setNextWeekly(System.currentTimeMillis() + 86400000L * 7);
                EconomyManager.updateAccount(account);
                event.getHook().editOriginal("✅ You claimed your weekly reward of %s%d!"
                        .formatted(config.getEconomyCurrency(), weeklyReward)).queue();
            }

            case "monthly" -> {
                if (account.getNextMonthly() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You can next claim your monthly reward %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextMonthly()))).queue();
                    return;
                }

                int monthlyReward = 500;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) monthlyReward += 50;

                EconomyManager.addMoney(account, monthlyReward, true);
                account.setNextMonthly(System.currentTimeMillis() + 86400000L * 7 * 30);
                EconomyManager.updateAccount(account);
                event.getHook().editOriginal("✅ You claimed your monthly reward of %s%d!"
                        .formatted(config.getEconomyCurrency(), monthlyReward)).queue();
            }

            case "yearly" -> {
                if (account.getNextYearly() > System.currentTimeMillis()) {
                    event.getHook().editOriginal("❌ You can next claim your yearly reward %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextYearly()))).queue();
                    return;
                }

                int yearlyReward = 750;
                if (Objects.requireNonNull(event.getMember()).getTimeBoosted() != null) yearlyReward += 50;

                EconomyManager.addMoney(account, yearlyReward, true);
                account.setNextYearly(System.currentTimeMillis() + 8400000L * 27 * 31 * 12);
                EconomyManager.updateAccount(account);
                event.getHook().editOriginal("✅ You claimed your yearly reward of %s%d!"
                        .formatted(config.getEconomyCurrency(), yearlyReward)).queue();
            }
        }
    }
}