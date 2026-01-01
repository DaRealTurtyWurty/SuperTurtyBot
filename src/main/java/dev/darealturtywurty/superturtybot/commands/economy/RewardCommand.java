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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RewardCommand extends EconomyCommand {
    private static final List<RewardDefinition> REWARDS = List.of(
            new RewardDefinition("daily", 1000L, 2, TimeUnit.DAYS.toMillis(1),
                    Economy::getNextDaily, Economy::setNextDaily),
            new RewardDefinition("weekly", 10000L, 5, TimeUnit.DAYS.toMillis(7),
                    Economy::getNextWeekly, Economy::setNextWeekly),
            new RewardDefinition("monthly", 50000L, 10, TimeUnit.DAYS.toMillis(31),
                    Economy::getNextMonthly, Economy::setNextMonthly),
            new RewardDefinition("yearly", 100000L, 20, TimeUnit.DAYS.toMillis(365),
                    Economy::getNextYearly, Economy::setNextYearly)
    );

    @Override
    public List<SubcommandData> createSubcommandData() {
        return new ArrayList<>(REWARDS.stream().map(reward ->
                new SubcommandData(reward.name(), "Claim your " + reward.name() + " reward!")).toList()) {{
            add(new SubcommandData("claimall", "Claim all available rewards at once!"));
        }};
    }

    @Override
    public String getDescription() {
        List<String> rewardNames = REWARDS.stream().map(RewardDefinition::name).toList();
        var builder = new StringBuilder();
        for (int i = 0; i < rewardNames.size(); i++) {
            builder.append(rewardNames.get(i));
            if (i == rewardNames.size() - 1)
                continue;

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
        if(account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot access your rewards! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        final String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You need to specify a reward to claim!").queue();
            return;
        }

        if (subcommand.equals("claimall")) {
            List<String> results = new ArrayList<>();
            for (RewardDefinition reward : REWARDS) {
                results.add(handleReward(event, account, config, reward));
            }

            boolean anyClaimed = results.stream().anyMatch(line -> line.startsWith("✅"));
            String response = String.join("\n", results);
            if (!anyClaimed) {
                response = "ℹ️ You have no rewards available to claim right now.\n" + response;
            }

            event.getHook().editOriginal(response).queue();
            return;
        }

        RewardDefinition rewardDefinition = REWARDS.stream()
                .filter(reward -> reward.name().equals(subcommand)).findFirst().orElse(null);
        if (rewardDefinition == null) {
            event.getHook().editOriginal("❌ That is not a valid reward!").queue();
            return;
        }

        String message = handleReward(event, account, config, rewardDefinition);
        event.getHook().editOriginal(message).queue();
    }

    private String handleReward(SlashCommandInteractionEvent event,
                                Economy account,
                                GuildData config,
                                RewardDefinition rewardInfo) {
        final String name = rewardInfo.name();
        long rewardAmount = rewardInfo.baseAmount();
        final long nextAllowedTime = rewardInfo.nextAllowedTimeGetter().apply(account);

        if (nextAllowedTime > System.currentTimeMillis())
            return "⏱️ You can next claim your %s reward %s."
                    .formatted(name, TimeFormat.RELATIVE.format(nextAllowedTime));

        if (account.getJob() != null) {
            rewardAmount = EconomyManager.getPayAmount(account) * rewardInfo.jobMultiplier();
        }

        Member member = event.getMember();
        if (member != null && member.getTimeBoosted() != null) {
            rewardAmount = (long) (rewardAmount * 2.5f);
        }

        BigInteger rewardBigInteger = BigInteger.valueOf(rewardAmount);
        EconomyManager.addMoney(account, rewardBigInteger, true);
        long cooldownMillis = rewardInfo.cooldownMillis();
        if (account.getRewardBoostUntil() > System.currentTimeMillis()) {
            cooldownMillis = Math.max(TimeUnit.HOURS.toMillis(6), Math.round(cooldownMillis * 0.75f));
        }

        rewardInfo.nextAllowedTimeSetter().accept(account, System.currentTimeMillis() + cooldownMillis);
        account.addTransaction(rewardBigInteger, MoneyTransaction.REWARD);
        EconomyManager.updateAccount(account);

        return "✅ You claimed your %s reward of %s!"
                .formatted(name, StringUtils.numberFormat(rewardBigInteger, config));
    }

    private record RewardDefinition(String name,
                                    long baseAmount,
                                    int jobMultiplier,
                                    long cooldownMillis,
                                    Function<Economy, Long> nextAllowedTimeGetter,
                                    BiConsumer<Economy, Long> nextAllowedTimeSetter) {
    }
}
