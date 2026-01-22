package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.commands.economy.blackjack.BlackjackCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GoFishAskSubcommand extends GoFishSubcommand {
    protected GoFishAskSubcommand() {
        super("ask", "Ask another player for a rank");
        addOption(OptionType.USER, "player", "The player you want to ask.", true);
        addOption(GoFishCommand.buildRankOption());
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        long channelId = event.getChannel().getIdLong();
        GoFishCommand.Game game = GoFishCommand.getGame(channelId);
        if (game == null) {
            event.getHook().editOriginal("❌ There is no Go Fish game in this channel.").queue();
            return;
        }

        synchronized (game) {
            game.touch();
            if (game.isStarted() && game.getChannelId() != channelId) {
                event.getHook().editOriginal("❌ Use this command in the Go Fish game thread.").queue();
                return;
            }

            if (!game.isStarted() || game.isFinished()) {
                event.getHook().editOriginal("❌ This Go Fish game has not started or is already finished.").queue();
                return;
            }

            GoFishCommand.PlayerState asker = game.getPlayer(event.getUser().getIdLong());
            if (asker == null) {
                event.getHook().editOriginal("❌ You are not part of this Go Fish game.").queue();
                return;
            }

            if (game.getCurrentPlayerId() != event.getUser().getIdLong()) {
                event.getHook().editOriginalFormat("❌ It is not your turn. Current turn: <@%d>.", game.getCurrentPlayerId()).queue();
                return;
            }

            if (asker.hand().isEmpty() && !game.getDeck().isEmpty()) {
                BlackjackCommand.Card drawn = game.getDeck().draw();
                asker.hand().add(drawn);
                List<BlackjackCommand.Card.Rank> newBooks = game.completeBooks(asker);
                event.getHook().editOriginal("🃏 You had no cards, so you drew one. Use `/gofish ask` again.").queue();
                event.getChannel().sendMessageFormat("%s draws a card because they had no cards.", event.getUser().getAsMention()).queue();
                if (!newBooks.isEmpty()) {
                    announceBooks(event, newBooks);
                }

                return;
            }

            if (asker.hand().isEmpty()) {
                if (game.shouldEnd()) {
                    finishGame(event, game, guild, config);
                    event.getHook().editOriginal("🏁 The game has ended. Check the channel for results.").queue();
                } else {
                    game.advanceTurn();
                    event.getHook().editOriginal("❌ You have no cards to ask with. Your turn is skipped.").queue();
                    event.getChannel().sendMessageFormat("%s has no cards and skips their turn.", event.getUser().getAsMention()).queue();
                }

                return;
            }

            User target = event.getOption("player", null, OptionMapping::getAsUser);
            if (target == null) {
                event.getHook().editOriginal("❌ Invalid player specified.").queue();
                return;
            }

            if (target.isBot()) {
                event.getHook().editOriginal("❌ You cannot ask a bot.").queue();
                return;
            }

            if (target.getIdLong() == event.getUser().getIdLong()) {
                event.getHook().editOriginal("❌ You cannot ask yourself.").queue();
                return;
            }

            GoFishCommand.PlayerState targetState = game.getPlayer(target.getIdLong());
            if (targetState == null) {
                event.getHook().editOriginal("❌ That player is not in this Go Fish game.").queue();
                return;
            }

            BlackjackCommand.Card.Rank rank = GoFishCommand.parseRank(event.getOption("rank", "", OptionMapping::getAsString));
            if (rank == null) {
                event.getHook().editOriginal("❌ Invalid rank specified.").queue();
                return;
            }

            if (!asker.hasRank(rank)) {
                event.getHook().editOriginal("❌ You must have at least one card of that rank to ask for it.").queue();
                return;
            }

            List<BlackjackCommand.Card> taken = targetState.removeAllOfRank(rank);
            String rankName = GoFishCommand.formatRank(rank);
            String publicMessage;
            boolean continueTurn = false;
            BlackjackCommand.Card drawn = null;

            if (!taken.isEmpty()) {
                asker.hand().addAll(taken);
                continueTurn = true;
                publicMessage = "%s asked %s for **%s** and received %d card(s). %s goes again."
                        .formatted(event.getUser().getAsMention(), target.getAsMention(), rankName, taken.size(),
                                event.getUser().getAsMention());
            } else {
                if (!game.getDeck().isEmpty()) {
                    drawn = game.getDeck().draw();
                    asker.hand().add(drawn);
                    continueTurn = drawn.rank() == rank;
                }

                if (continueTurn) {
                    publicMessage = "%s asked %s for **%s**. Go fish! They drew the requested rank and go again."
                            .formatted(event.getUser().getAsMention(), target.getAsMention(), rankName);
                } else {
                    publicMessage = "%s asked %s for **%s**. Go fish!"
                            .formatted(event.getUser().getAsMention(), target.getAsMention(), rankName);
                }
            }

            List<BlackjackCommand.Card.Rank> newBooks = game.completeBooks(asker);
            if (game.shouldEnd()) {
                finishGame(event, game, guild, config);
            } else if (!continueTurn) {
                game.advanceTurn();
            }

            var privateMessage = new StringBuilder();
            if (!taken.isEmpty()) {
                privateMessage.append("✅ You received ").append(taken.size()).append(" card(s) from ")
                        .append(target.getAsMention()).append(".\n");
            } else if (drawn != null) {
                privateMessage.append("🎣 Go fish! You drew ").append(GoFishCommand.formatRank(drawn.rank()))
                        .append(".\n");
            } else {
                privateMessage.append("🎣 Go fish! The deck is empty.\n");
            }

            if (!newBooks.isEmpty()) {
                privateMessage.append("📚 You completed a book of ");
                appendRanks(privateMessage, newBooks);
                privateMessage.append(".\n");
            }

            if (game.isFinished()) {
                privateMessage.append("🏁 The game has ended. Check the channel for results.\n");
            } else {
                privateMessage.append("➡️ Next turn: <@").append(game.getCurrentPlayerId()).append(">.\n");
            }

            privateMessage.append("**Your hand:** ").append(GoFishCommand.renderHand(asker))
                    .append("\n**Your books:** ").append(GoFishCommand.renderBooks(asker));

            try (FileUpload upload = GoFishImageRenderer.createUpload(asker.hand())) {
                event.getHook().editOriginal(privateMessage.toString()).setFiles(upload).queue();
            } catch (Exception exception) {
                Constants.LOGGER.error("Failed to create Go Fish hand image!", exception);
                event.getHook().editOriginal(privateMessage + "\n❌ Failed to render the hand image.").queue();
            }

            event.getChannel().sendMessage(publicMessage).queue();
            if (!newBooks.isEmpty()) {
                announceBooks(event, newBooks);
            }

            if (!game.isFinished() && !continueTurn) {
                event.getChannel().sendMessageFormat(
                        "<@%d>, it's your turn! Use `/gofish ask <player> <rank>` or `/gofish hand`.",
                        game.getCurrentPlayerId()
                ).queue();
            }
        }
    }

    private void announceBooks(SlashCommandInteractionEvent event, List<BlackjackCommand.Card.Rank> books) {
        var builder = new StringBuilder();
        builder.append("📚 ").append(event.getUser().getAsMention()).append(" completed a book of ");
        appendRanks(builder, books);
        builder.append(".");
        event.getChannel().sendMessage(builder.toString()).queue();
    }

    private void appendRanks(StringBuilder builder, List<BlackjackCommand.Card.Rank> ranks) {
        var joiner = new StringJoiner(", ");
        for (BlackjackCommand.Card.Rank rank : ranks) {
            joiner.add(GoFishCommand.formatRank(rank));
        }
        builder.append(joiner);
    }

    private void finishGame(SlashCommandInteractionEvent event, GoFishCommand.Game game, Guild guild, GuildData config) {
        List<Long> winners = game.determineWinners();
        BigInteger pot = game.getBet().multiply(BigInteger.valueOf(game.playerCount()));
        BigInteger split = pot.divide(BigInteger.valueOf(winners.size()));
        BigInteger remainder = pot.remainder(BigInteger.valueOf(winners.size()));

        List<Long> winnersCopy = new ArrayList<>(winners);
        for (GoFishCommand.PlayerState player : game.getPlayerStates()) {
            BigInteger payout = winners.contains(player.userId()) ? split : BigInteger.ZERO;
            if (!winnersCopy.isEmpty() && player.userId() == winnersCopy.getFirst() && remainder.signum() > 0) {
                payout = payout.add(remainder);
            }

            Economy playerAccount = EconomyManager.getOrCreateAccount(guild, player.userId());
            if (payout.signum() > 0) {
                EconomyManager.addMoney(playerAccount, payout, false);
            }

            BigInteger net = payout.subtract(game.getBet());
            playerAccount.addTransaction(net, MoneyTransaction.GO_FISH);
            if (net.signum() > 0) {
                EconomyManager.betWin(playerAccount, net);
            } else if (net.signum() < 0) {
                EconomyManager.betLoss(playerAccount, net.negate());
            }
            EconomyManager.updateAccount(playerAccount);
        }

        game.markFinished();
        GoFishCommand.removeGame(game);
        GoFishCommand.cancelInactivityWatch(game);
        GoFishCommand.closeGameThread(guild, game);

        var winnerJoiner = new StringJoiner(", ");
        for (Long winner : winners) {
            GoFishCommand.PlayerState winnerState = game.getPlayer(winner);
            int books = winnerState == null ? 0 : winnerState.books().size();
            winnerJoiner.add("<@" + winner + "> (" + books + " books)");
        }

        String splitText = winners.size() == 1
                ? StringUtils.numberFormat(split.add(remainder), config)
                : StringUtils.numberFormat(split, config);

        String resultMessage = "🏁 **Go Fish finished!**\nWinners: %s\nPot: %s | Payout per winner: %s"
                .formatted(winnerJoiner, StringUtils.numberFormat(pot, config), splitText);
        if (winners.size() > 1 && remainder.signum() > 0) {
            resultMessage += " (+" + StringUtils.numberFormat(remainder, config) + " to the first winner)";
        }

        event.getChannel().sendMessage(resultMessage).queue();
    }
}
