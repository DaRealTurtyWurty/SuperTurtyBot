package dev.darealturtywurty.superturtybot.commands.economy.blackjack;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.FileUpload;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BlackjackPlayCommand extends BlackjackSubcommand {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    protected BlackjackPlayCommand() {
        super("play", "Play a game of blackjack");
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

        final List<BlackjackCommand.Game> games = BlackjackCommand.GAMES.computeIfAbsent(guild.getIdLong(), ignored -> new ArrayList<>());
        if (!games.isEmpty() && games.stream().anyMatch(game -> game.getGuild() == guild.getIdLong() && game.getUser() == event.getUser().getIdLong())) {
            event.getHook().editOriginal("❌ You are already in a game of Blackjack!").queue();
            return;
        }

        if (!Environment.INSTANCE.isDevelopment()) {
            if (account.getNextBlackjack() > System.currentTimeMillis()) {
                event.getHook().editOriginalFormat("❌ You may play Blackjack again %s!", TimeFormat.RELATIVE.format(account.getNextBlackjack())).queue();
                return;
            }

            account.setNextBlackjack(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
        }

        EconomyManager.removeMoney(account, amount, false);
        EconomyManager.updateAccount(account);

        event.getHook().editOriginalFormat("✅ You have started a game of Blackjack with a bet of %s!",
                StringUtils.numberFormat(amount, config.getEconomyCurrency())).flatMap(message ->
                message.createThreadChannel(event.getUser().getName() + "'s Blackjack Game")).queue(thread -> {
            thread.addThreadMember(event.getUser()).queue();

            thread.sendMessageFormat("%s, your game of Blackjack has started! Your bet: %s",
                    event.getUser().getAsMention(),
                    StringUtils.numberFormat(amount, config.getEconomyCurrency())).queue(ignored -> {
                var game = new BlackjackCommand.Game(guild.getIdLong(), thread.getIdLong(), event.getUser().getIdLong(), amount);
                games.add(game);

                game.start();
                if (game.isFinished()) {
                    games.remove(game);
                    BlackjackCommand.Game.Settlement settlement = game.getSettlement().orElseThrow();
                    applySettlement(account, settlement);
                    thread.sendMessageFormat("%s, your game of Blackjack has ended! %s",
                            event.getUser().getAsMention(),
                            game.getResultMessage(number -> StringUtils.numberFormat(number, config.getEconomyCurrency()))).queue();
                    thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
                    return;
                }

                try (FileUpload upload = BlackjackImageRenderer.createUpload(game, false)) {
                    thread.sendMessage("Use /blackjack hit to draw a card or /blackjack stand to end your turn.")
                            .setFiles(upload)
                            .queue();
                } catch (Exception exception) {
                    Constants.LOGGER.error("Failed to create blackjack image!", exception);
                    thread.sendMessage("❌ An error occurred while creating the blackjack image!").queue();
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
                            BlackjackCommand.Game.Settlement settlement = game.getSettlement().orElseThrow();
                            applySettlement(account, settlement);
                            thread.sendMessageFormat("%s, %s",
                                    event.getUser().getAsMention(),
                                    game.getResultMessage(number -> StringUtils.numberFormat(number, config.getEconomyCurrency()))).queue();
                            thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
                        }
                    }
                }, 0, 5, TimeUnit.SECONDS);

                checkerRef.set(future);
            });
        });
    }
}
