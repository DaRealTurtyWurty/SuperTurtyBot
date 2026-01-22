package dev.darealturtywurty.superturtybot.commands.economy.poker;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PokerBetCommand extends PokerSubcommand {
    public PokerBetCommand() {
        super("bet", "Bet to continue to the next stage");
        addOption(OptionType.STRING, "amount", "The amount to bet.", true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        PokerCommand.Game game = PokerCommand.getOngoingGame(event);
        if (game == null) {
            event.getHook().editOriginal("❌ You do not have an ongoing poker hand!").queue();
            return;
        }

        if (game.isFinished()) {
            event.getHook().editOriginal("❌ Your poker hand is already finished!").queue();
            return;
        }

        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.getHook().editOriginal("❌ You must use this command in your poker thread!").queue();
            return;
        }

        if (thread.getIdLong() != game.getChannel()) {
            event.getHook().editOriginal("❌ This is not your current poker thread!").queue();
            return;
        }

        BigInteger amount = event.getOption("amount", null, StringUtils.getAsBigInteger(event));
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

        synchronized (game) {
            try {
                game.bet(amount);
            } catch (IllegalStateException exception) {
                event.getHook().editOriginal("❌ " + exception.getMessage()).queue();
                return;
            }

            EconomyManager.removeMoney(account, amount, false);
            EconomyManager.updateAccount(account);

            if (game.isFinished()) {
                PokerCommand.Settlement settlement = game.getSettlement();
                applySettlement(account, settlement);
                games.remove(game);

                String stateMessage = PokerPlayCommand.buildStateMessage(game, config);
                String resultMessage = game.getResultMessage(number -> StringUtils.numberFormat(number, config));
                String content = stateMessage + "\n" + resultMessage;
                try (FileUpload upload = PokerImageRenderer.createUpload(game, true)) {
                    event.getHook().editOriginal(content).setFiles(upload).queue();
                } catch (Exception exception) {
                    Constants.LOGGER.error("Failed to create poker image!", exception);
                    event.getHook().editOriginal(content + "\n❌ Failed to create the poker image.").queue();
                }

                thread.getManager().setArchived(true).setLocked(true).queueAfter(5, TimeUnit.SECONDS);
            } else {
                String content = PokerPlayCommand.buildStateMessage(game, config);
                try (FileUpload upload = PokerImageRenderer.createUpload(game, false)) {
                    event.getHook().editOriginal(content).setFiles(upload).queue();
                } catch (Exception exception) {
                    Constants.LOGGER.error("Failed to create poker image!", exception);
                    event.getHook().editOriginal(content + "\n❌ Failed to create the poker image.").queue();
                }
            }
        }
    }
}
