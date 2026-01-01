package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class BoostCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("list", "List available convenience boosts"),
                new SubcommandData("status", "See your active boosts"),
                new SubcommandData("buy", "Buy a convenience boost")
                        .addOptions(new OptionData(OptionType.STRING, "boost", "The boost to buy", true, true))
        );
    }

    @Override
    public String getDescription() {
        return "Buy convenience boosts that reduce cooldowns for a limited time.";
    }

    @Override
    public String getName() {
        return "boost";
    }

    @Override
    public String getRichName() {
        return "Boost";
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(getName())) return;
        if (!event.getFocusedOption().getName().equalsIgnoreCase("boost")) return;

        event.replyChoices(Stream.of(BoostType.values())
                .map(boost -> new Command.Choice(boost.displayName(), boost.key()))
                .toList()).queue();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if(account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot access boosters! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        switch (subcommand) {
            case "list" -> listBoosts(event, config);
            case "status" -> showStatus(event, account);
            case "buy" -> buyBoost(event, account, config);
            default -> event.getHook().editOriginal("❌ That is not a valid subcommand!").queue();
        }
    }

    private void listBoosts(SlashCommandInteractionEvent event, GuildData config) {
        var builder = new StringBuilder("**Available Boosts**\n");
        for (BoostType boost : BoostType.values()) {
            builder.append("- ")
                    .append(boost.displayName())
                    .append(" - ")
                    .append(boost.description())
                    .append(" (")
                    .append(StringUtils.numberFormat(boost.cost(), config))
                    .append(")\n");
        }

        event.getHook().editOriginal(builder.toString().trim()).queue();
    }

    private void showStatus(SlashCommandInteractionEvent event, Economy account) {
        var builder = new StringBuilder("**Active Boosts**\n");
        boolean any = false;
        for (BoostType boost : BoostType.values()) {
            long until = boost.getBoostUntil(account);
            if (until > System.currentTimeMillis()) {
                any = true;
                builder.append("- ")
                        .append(boost.displayName())
                        .append(" until ")
                        .append(TimeFormat.RELATIVE.format(until))
                        .append("\n");
            }
        }

        if (!any) {
            builder.append("No active boosts.");
        }

        event.getHook().editOriginal(builder.toString().trim()).queue();
    }

    private void buyBoost(SlashCommandInteractionEvent event, Economy account, GuildData config) {
        String boostName = event.getOption("boost", "", opt -> opt.getAsString().toLowerCase(Locale.ROOT));
        BoostType boost = BoostType.fromKey(boostName);
        if (boost == null) {
            event.getHook().editOriginal("❌ That boost does not exist!").queue();
            return;
        }

        long currentUntil = boost.getBoostUntil(account);
        if (currentUntil > System.currentTimeMillis()) {
            event.getHook().editOriginal("❌ That boost is already active until %s!"
                    .formatted(TimeFormat.RELATIVE.format(currentUntil))).queue();
            return;
        }

        if (!EconomyManager.removeBalance(account, boost.cost())) {
            event.getHook().editOriginal("❌ You need another %s to buy this boost!"
                    .formatted(StringUtils.numberFormat(boost.cost().subtract(EconomyManager.getBalance(account)), config))).queue();
            return;
        }

        boost.apply(account);
        account.addTransaction(boost.cost().negate(), MoneyTransaction.BOOST);
        EconomyManager.updateAccount(account);

        event.getHook().editOriginal("✅ You bought %s! It expires %s."
                .formatted(boost.displayName(), TimeFormat.RELATIVE.format(boost.getBoostUntil(account)))).queue();
    }

    private enum BoostType {
        WORK("work", "Work Boost", "25% shorter work cooldowns for 24h",
                BigInteger.valueOf(25_000), TimeUnit.HOURS.toMillis(24)) {
            @Override
            void apply(Economy account) {
                account.setWorkBoostUntil(System.currentTimeMillis() + durationMillis());
            }

            @Override
            long getBoostUntil(Economy account) {
                return account.getWorkBoostUntil();
            }
        },
        CRIME("crime", "Crime Boost", "25% shorter crime cooldowns for 24h",
                BigInteger.valueOf(40_000), TimeUnit.HOURS.toMillis(24)) {
            @Override
            void apply(Economy account) {
                account.setCrimeBoostUntil(System.currentTimeMillis() + durationMillis());
            }

            @Override
            long getBoostUntil(Economy account) {
                return account.getCrimeBoostUntil();
            }
        },
        REWARD("reward", "Reward Boost", "25% shorter reward cooldowns for 24h",
                BigInteger.valueOf(30_000), TimeUnit.HOURS.toMillis(24)) {
            @Override
            void apply(Economy account) {
                account.setRewardBoostUntil(System.currentTimeMillis() + durationMillis());
            }

            @Override
            long getBoostUntil(Economy account) {
                return account.getRewardBoostUntil();
            }
        };

        private final String key;
        private final String displayName;
        private final String description;
        private final BigInteger cost;
        private final long durationMillis;

        BoostType(String key, String displayName, String description, BigInteger cost, long durationMillis) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
            this.cost = cost;
            this.durationMillis = durationMillis;
        }

        String key() {
            return key;
        }

        String displayName() {
            return displayName;
        }

        String description() {
            return description;
        }

        BigInteger cost() {
            return cost;
        }

        long durationMillis() {
            return durationMillis;
        }

        abstract void apply(Economy account);

        abstract long getBoostUntil(Economy account);

        static BoostType fromKey(String key) {
            for (BoostType boost : values()) {
                if (boost.key.equalsIgnoreCase(key) || boost.displayName.equalsIgnoreCase(key))
                    return boost;
            }

            return null;
        }
    }
}
