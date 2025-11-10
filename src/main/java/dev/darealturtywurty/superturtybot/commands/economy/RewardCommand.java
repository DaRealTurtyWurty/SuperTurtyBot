package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import oshi.util.tuples.Quintet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class RewardCommand extends EconomyCommand {
    private static final List<Quintet<String, Long, Integer, Function<Economy, Long>, Consumer<Economy>>> REWARDS = List.of(
            new Quintet<>("daily", 1000L, 2, Economy::getNextDaily, account -> account.setNextDaily(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))),
            new Quintet<>("weekly", 10000L, 5, Economy::getNextWeekly, account -> account.setNextWeekly(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))),
            new Quintet<>("monthly", 50000L, 10, Economy::getNextMonthly, account -> account.setNextMonthly(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31))),
            new Quintet<>("yearly", 100000L, 20, Economy::getNextYearly, account -> account.setNextYearly(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)))
    );

    @Override
    public List<SubcommandData> createSubcommandData() {
        return new ArrayList<>(REWARDS.stream().map(quintet ->
                new SubcommandData(quintet.getA(), "Claim your " + quintet.getA() + " reward!")).toList()) {{
            add(new SubcommandData("claimall", "Claim all available rewards at once!"));
        }};
    }

    @Override
    public String getDescription() {
        List<String> rewardNames = REWARDS.stream().map(Quintet::getA).toList();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rewardNames.size(); i++) {
            builder.append(rewardNames.get(i));
            if (i == rewardNames.size() - 1) continue;
            if (i == rewardNames.size() - 2) {
                builder.append(", and ");
            } else {
                builder.append(", ");
            }
        }

        return "Claim your " + builder + " rewards!";
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
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You need to specify a reward to claim!").queue();
            return;
        }

        if (subcommand.equals("claimall")) {
            List<String> results = new ArrayList<>();
            for (Quintet<String, Long, Integer, Function<Economy, Long>, Consumer<Economy>> quintet : REWARDS) {
                results.add(handleReward(event, account, config, quintet));
            }

            boolean anyClaimed = results.stream().anyMatch(line -> line.startsWith("✅"));
            String response = String.join("\n", results);
            if (!anyClaimed) {
                response = "ℹ️ You have no rewards available to claim right now.\n" + response;
            }

            event.getHook().editOriginal(response).queue();
            return;
        }

        Quintet<String, Long, Integer, Function<Economy, Long>, Consumer<Economy>> rewardQuintet = REWARDS.stream()
                .filter(quintet -> quintet.getA().equals(subcommand)).findFirst().orElse(null);
        if (rewardQuintet == null) {
            event.getHook().editOriginal("❌ That is not a valid reward!").queue();
            return;
        }

        String message = handleReward(event, account, config, rewardQuintet);
        event.getHook().editOriginal(message).queue();
    }

    private String handleReward(SlashCommandInteractionEvent event,
                                Economy account,
                                GuildData config,
                                Quintet<String, Long, Integer, Function<Economy, Long>, Consumer<Economy>> rewardInfo) {

        final String name = rewardInfo.getA();
        long rewardAmount = rewardInfo.getB();
        final long nextAllowedTime = rewardInfo.getD().apply(account);

        if (nextAllowedTime > System.currentTimeMillis()) {
            return "⏱️ You can next claim your %s reward %s."
                    .formatted(name, TimeFormat.RELATIVE.format(nextAllowedTime));
        }

        if (account.getJob() != null) {
            rewardAmount = EconomyManager.getPayAmount(account) * rewardInfo.getC();
        }

        Member member = event.getMember();
        if (member != null && member.getTimeBoosted() != null) {
            rewardAmount = (long) (rewardAmount * 2.5f);
        }

        BigInteger rewardBigInteger = BigInteger.valueOf(rewardAmount);
        EconomyManager.addMoney(account, rewardBigInteger, true);
        rewardInfo.getE().accept(account);
        account.addTransaction(rewardBigInteger, MoneyTransaction.REWARD);
        EconomyManager.updateAccount(account);

        return "✅ You claimed your %s reward of %s!"
                .formatted(name, StringUtils.numberFormat(rewardBigInteger, config));
    }
}
