package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import lombok.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class CrashCommand extends EconomyCommand {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Long, List<Game>> GAMES = new HashMap<>();

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "amount", "The amount of money to bet.", true));
    }

    @Override
    public String getDescription() {
        return "Bet money in a game of Crash!";
    }

    @Override
    public String getName() {
        return "crash";
    }

    @Override
    public String getRichName() {
        return "Crash";
    }

    @Override
    public String getHowToUse() {
        return "/crash <amount>";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        BigInteger amount = event.getOption("amount", StringUtils.getAsBigInteger(event));
        if (amount == null) return;
        if (amount.signum() <= 0) {
            event.getHook().editOriginal("❌ You must bet at least %s1!".formatted(config.getEconomyCurrency())).queue();
            return;
        }

        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (amount.compareTo(account.getWallet()) > 0) {
            event.getHook().editOriginal("❌ You cannot bet more than you have in your wallet!").queue();
            return;
        }

        final List<Game> games = GAMES.computeIfAbsent(guild.getIdLong(), ignored -> new ArrayList<>());
        if (!games.isEmpty() && games.stream().anyMatch(game -> game.getGuild() == guild.getIdLong() && game.getUser() == event.getUser().getIdLong())) {
            event.getHook().editOriginal("❌ You are already in a game of Crash!").queue();
            return;
        }

        if (account.getNextCrash() > System.currentTimeMillis()) {
            event.getHook().editOriginalFormat("❌ You may crash again %s!", TimeFormat.RELATIVE.format(account.getNextCrash())).queue();
            return;
        }

        account.setNextCrash(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30));
        EconomyManager.removeMoney(account, amount, false);
        EconomyManager.updateAccount(account);

        event.getHook().editOriginal("✅ You have bet %s!".formatted(StringUtils.numberFormat(amount, config))).flatMap(message ->
                message.createThreadChannel(event.getUser().getName() + "'s Crash Game")).queue(thread -> {
            thread.addThreadMember(event.getUser()).queue();
            thread.sendMessage(("""
                    You have bet %s! The multiplier has started at 0.25x!
                    
                    It will increase by a random amount every 3 seconds, however, it will crash at a random point between 0.3x and 10.0x!
                    
                    You need to type `cashout` in order to cashout before it crashes. Good luck!""").formatted(StringUtils.numberFormat(amount, config))).queue(ignored -> {
                var game = new Game(guild.getIdLong(), thread.getIdLong(), event.getUser().getIdLong(), amount);
                games.add(game);

                TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                        .condition(msgEvent -> msgEvent.isFromGuild()
                                && msgEvent.getGuild().getIdLong() == guild.getIdLong()
                                && msgEvent.getChannel().getIdLong() == thread.getIdLong()
                                && msgEvent.getAuthor().getIdLong() == event.getUser().getIdLong()
                                && msgEvent.getMessage().getContentRaw().equalsIgnoreCase("cashout"))
                        .success(msgEvent -> game.cashout(event.getJDA(), config, account))
                        .failure(() -> game.close(thread))
                        .timeout(10, TimeUnit.MINUTES)
                        .build();

                game.start(event.getJDA(), config, account);
            });
        });
    }

    public static boolean isPlaying(long guild, long user) {
        return GAMES.getOrDefault(guild, List.of()).stream().anyMatch(game -> game.getUser() == user);
    }

    @Data
    public static class Game {
        private final long guild;
        private final long channel;
        private final long user;
        private final BigInteger amount;

        private double multiplier = 0.25;
        private int ticksPassed = 0;
        private ScheduledFuture<?> future;

        public Game(long guild, long channel, long user, BigInteger amount) {
            this.guild = guild;
            this.channel = channel;
            this.user = user;
            this.amount = amount;
        }

        public void start(final JDA jda, final GuildData config, final Economy account) {
            if (this.future != null) {
                this.future.cancel(true);
            }

            int crashChance = ThreadLocalRandom.current().nextInt(5, 25);
            this.future = EXECUTOR.scheduleAtFixedRate(
                    () -> tick(jda, config, account, crashChance),
                    5,
                    3,
                    TimeUnit.SECONDS);

            Guild guild = jda.getGuildById(this.guild);
            if (guild == null)
                return;

            ThreadChannel thread = guild.getThreadChannelById(this.channel);
            if (thread == null)
                return;

            thread.sendMessage("The multiplier has started at 0.25x!").queue();
        }

        private void tick(final JDA jda, final GuildData config, final Economy account, final int crashChance) {
            Guild guild = jda.getGuildById(this.guild);
            if (guild == null)
                return;

            ThreadChannel thread = guild.getThreadChannelById(this.channel);
            if (thread == null)
                return;

            boolean crashed = ThreadLocalRandom.current().nextInt(crashChance) == 0 && this.ticksPassed > 5;
            if (crashed) {
                thread.sendMessage("The multiplier has crashed at %s! You have lost %s!"
                        .formatted(stringifyMultiplier(multiplier), StringUtils.numberFormat(this.amount, config))).queue(
                        ignored -> close(thread));
                EconomyManager.betLoss(account, this.amount);
                account.addTransaction(this.amount.negate(), MoneyTransaction.CRASH);
                EconomyManager.updateAccount(account);

                close(thread);
                return;
            }

            if (multiplier >= 10.0) {
                cashout(jda, config, account);
                return;
            }

            multiplier += getMultiplier();

            thread.sendMessage("The multiplier is now at %s!".formatted(stringifyMultiplier(multiplier))).queue();
            this.ticksPassed++;
        }

        public void cashout(JDA jda, GuildData config, Economy account) {
            Guild guild = jda.getGuildById(this.guild);
            if (guild == null) {
                close(null);
                return;
            }

            ThreadChannel thread = guild.getThreadChannelById(this.channel);
            if (thread == null) {
                close(null);
                return;
            }

            double clampedMultiplier = MathUtils.clamp(multiplier, 0.25, 10.0);
            BigInteger amount = new BigDecimal(this.amount).multiply(BigDecimal.valueOf(clampedMultiplier)).toBigInteger();
            if (multiplier >= 10) {
                thread.sendMessage("The multiplier has reached 10.0x! You have won %s!"
                                .formatted(StringUtils.numberFormat(amount.subtract(this.amount), config)))
                        .queue(ignored -> close(thread));
            } else {
                thread.sendMessage("You have cashed out at %s! You have won %s!"
                                .formatted(stringifyMultiplier(multiplier), StringUtils.numberFormat(amount.subtract(this.amount), config)))
                        .queue(ignored -> close(thread));
            }

            EconomyManager.addMoney(account, amount);
            EconomyManager.betWin(account, amount.subtract(this.amount));
            account.addTransaction(amount.subtract(this.amount), MoneyTransaction.CRASH);
            EconomyManager.updateAccount(account);
        }

        private void close(@Nullable ThreadChannel thread) {
            this.future.cancel(true);
            if (thread != null) {
                thread.getManager().setArchived(true).setLocked(true).queue();
            }

            List<Game> games = GAMES.computeIfAbsent(this.guild, ignored -> new ArrayList<>());
            games.remove(this);
        }

        /**
         * Gets a random multiplier between 0 and 0.25 truncated to 2 decimal places
         *
         * @return The multiplier
         */
        private static double getMultiplier() {
            return MathUtils.clamp(ThreadLocalRandom.current().nextInt(25) / 100d, 0.01, 1);
        }

        /**
         * Converts a multiplier to a string
         *
         * @param multiplier The multiplier to convert
         * @return The multiplier as a string
         */
        private static String stringifyMultiplier(double multiplier) {
            return String.format("%.2fx", multiplier).replace(".00", ".0");
        }
    }
}
