package dev.darealturtywurty.superturtybot.commands.economy.poker;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PokerPlayCommand extends PokerSubcommand {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    protected PokerPlayCommand() {
        super("play", "Start a hand of Texas Hold'em against the dealer");
        addOption(OptionType.STRING, "bet", "The amount you want to bet.", true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        if (event.getChannelType().isThread()) {
            event.getHook().editOriginal("❌ This command cannot be used in a thread!").queue();
            return;
        }

        BigInteger amount = event.getOption("bet", StringUtils.getAsBigInteger(event));
        if (amount == null) {
            event.getHook().editOriginal("❌ Invalid bet amount specified!").queue();
            return;
        }

        if (amount.signum() <= 0) {
            event.getHook().editOriginal("❌ You must bet at least %s1!".formatted(config.getEconomyCurrency())).queue();
            return;
        }

        if (amount.compareTo(account.getWallet()) > 0) {
            event.getHook().editOriginal("❌ You cannot bet more than you have in your wallet!").queue();
            return;
        }

        final List<PokerCommand.Game> games = PokerCommand.GAMES.computeIfAbsent(guild.getIdLong(), ignored -> new ArrayList<>());
        if (!games.isEmpty() && games.stream().anyMatch(game -> game.getGuild() == guild.getIdLong()
                && game.getUser() == event.getUser().getIdLong()
                && !game.isFinished())) {
            event.getHook().editOriginal("❌ You are already in a poker game!").queue();
            return;
        }

        EconomyManager.removeMoney(account, amount, false);
        EconomyManager.updateAccount(account);

        event.getHook().editOriginalFormat("✅ You have started a poker hand with a bet of %s!",
                StringUtils.numberFormat(amount, config)).flatMap(message ->
                message.createThreadChannel(event.getUser().getName() + "'s Poker Game")).queue(thread -> {
            thread.addThreadMember(event.getUser()).queue();

            thread.sendMessageFormat("%s, your poker hand has started! Your bet: %s\n" +
                            "Use `/poker check` to continue, `/poker bet` to bet, or `/poker fold` to fold.",
                    event.getUser().getAsMention(),
                    StringUtils.numberFormat(amount, config)).queue(ignored -> {
                var game = new PokerCommand.Game(guild.getIdLong(), thread.getIdLong(), event.getUser().getIdLong(), amount);
                games.add(game);

                game.start();
                String content = buildStateMessage(game, config);
                try (FileUpload upload = PokerImageRenderer.createUpload(game, false)) {
                    thread.sendMessage(content).setFiles(upload).queue();
                } catch (Exception exception) {
                    Constants.LOGGER.error("Failed to create poker image!", exception);
                    thread.sendMessage(content + "\n❌ Failed to create the poker image.").queue();
                }

                AtomicReference<ScheduledFuture<?>> checkerRef = new AtomicReference<>();
                ScheduledFuture<?> future = SCHEDULER.scheduleAtFixedRate(() -> {
                    if (game.isFinished()) {
                        ScheduledFuture<?> checker = checkerRef.get();
                        if (checker != null) {
                            checker.cancel(false);
                        }

                        return;
                    }

                    long now = System.currentTimeMillis();
                    if (now - game.getLastActionTime() >= TimeUnit.MINUTES.toMillis(5)) {
                        if (games.remove(game)) {
                            game.timeout();
                            PokerCommand.Settlement settlement = game.getSettlement();
                            applySettlement(account, settlement);
                            thread.sendMessageFormat("%s, %s",
                                    event.getUser().getAsMention(),
                                    game.getResultMessage(number -> StringUtils.numberFormat(number, config))).queue();
                            thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
                        }
                    }
                }, 0, 5, TimeUnit.SECONDS);

                checkerRef.set(future);
            });
        });
    }

    static String buildStateMessage(PokerCommand.Game game, GuildData config) {
        PokerCommand.Settlement settlement = game.getSettlement();
        String stage = game.isFinished() && settlement != null
                ? (settlement.result() == PokerCommand.Result.PLAYER_FOLD || settlement.result() == PokerCommand.Result.TIMEOUT
                ? "Finished"
                : "Showdown")
                : game.getStage().getLabel();
        String prompt = game.isFinished()
                ? ""
                : "\nUse `/poker check` to continue, `/poker bet` to bet, or `/poker fold` to fold.";
        return "**Stage:** " + stage + "\n"
                + "**Your total bet:** " + StringUtils.numberFormat(game.getTotalBet(), config) + "\n"
                + "**Pot:** " + StringUtils.numberFormat(game.getTotalBet().multiply(BigInteger.valueOf(2)), config)
                + prompt;
    }
}
