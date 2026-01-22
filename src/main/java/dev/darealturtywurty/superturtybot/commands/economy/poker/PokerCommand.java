package dev.darealturtywurty.superturtybot.commands.economy.poker;

import dev.darealturtywurty.superturtybot.commands.economy.EconomyCommand;
import dev.darealturtywurty.superturtybot.commands.economy.blackjack.BlackjackCommand;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PokerCommand extends EconomyCommand {
    protected static final Map<Long, List<Game>> GAMES = new ConcurrentHashMap<>();

    public PokerCommand() {
        addSubcommands(
                new PokerPlayCommand(),
                new PokerCheckCommand(),
                new PokerBetCommand(),
                new PokerFoldCommand(),
                new PokerHowToPlayCommand()
        );
    }

    public static Game getOngoingGame(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null)
            return null;

        List<Game> games = GAMES.get(event.getGuild().getIdLong());
        if (games == null)
            return null;

        for (Game game : games) {
            if (game.getGuild() == event.getGuild().getIdLong()
                    && game.getUser() == event.getUser().getIdLong()
                    && !game.isFinished())
                return game;
        }

        return null;
    }

    @Override
    public String getDescription() {
        return "Play a simplified Texas Hold'em hand against the dealer.";
    }

    @Override
    public String getName() {
        return "poker";
    }

    @Override
    public String getRichName() {
        return "Poker";
    }

    @Override
    public String getHowToUse() {
        return "/poker play <bet>";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 0L);
    }

    @Getter
    public enum Stage {
        PRE_FLOP("Pre-Flop"),
        FLOP("Flop"),
        TURN("Turn"),
        RIVER("River"),
        FINISHED("Finished");

        private final String label;

        Stage(String label) {
            this.label = label;
        }
    }

    public enum Result {
        PLAYER_WIN,
        DEALER_WIN,
        PUSH,
        PLAYER_FOLD,
        TIMEOUT
    }

    public record Settlement(Result result, BigInteger bet, BigInteger payout,
                             HandRank playerRank, HandRank dealerRank) {
        public Settlement(Result result, BigInteger bet, BigInteger payout,
                          HandRank playerRank, HandRank dealerRank) {
            this.result = Objects.requireNonNull(result, "result");
            this.bet = Objects.requireNonNull(bet, "bet");
            this.payout = Objects.requireNonNull(payout, "payout");
            this.playerRank = playerRank;
            this.dealerRank = dealerRank;
        }
    }

    @Getter
    public static final class Game {
        private final long guild;
        private final long channel;
        private final long user;
        private final BigInteger bet;
        private BigInteger totalBet;
        private final BlackjackCommand.Deck deck;
        private final List<BlackjackCommand.Card> playerHand = new ArrayList<>(2);
        private final List<BlackjackCommand.Card> dealerHand = new ArrayList<>(2);
        private final List<BlackjackCommand.Card> communityCards = new ArrayList<>(5);

        private Stage stage = Stage.PRE_FLOP;
        private boolean awaitingAction = true;
        private final Instant createdAt = Instant.now();
        private Instant lastActionAt = createdAt;
        private Settlement settlement;

        public Game(long guild, long channel, long user, BigInteger bet) {
            if (bet == null || bet.signum() <= 0)
                throw new IllegalArgumentException("Bet must be a positive value.");

            this.guild = guild;
            this.channel = channel;
            this.user = user;
            this.bet = bet;
            this.totalBet = bet;
            this.deck = new BlackjackCommand.Deck(1);
        }

        public void start() {
            ensureNotFinished();

            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());
            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());
            lastActionAt = Instant.now();
        }

        public void check() {
            ensureAwaitingAction();
            advanceStageOrShowdown();
        }

        public void bet(BigInteger amount) {
            ensureAwaitingAction();
            if (amount == null || amount.signum() <= 0)
                throw new IllegalArgumentException("Bet must be a positive value.");

            totalBet = totalBet.add(amount);
            advanceStageOrShowdown();
        }

        public void fold() {
            if (settlement != null || stage == Stage.FINISHED)
                return;

            finish(Result.PLAYER_FOLD, null, null);
        }

        public void timeout() {
            if (settlement != null || stage == Stage.FINISHED)
                return;

            finish(Result.TIMEOUT, null, null);
        }

        public boolean isFinished() {
            return stage == Stage.FINISHED;
        }

        public long getLastActionTime() {
            return lastActionAt.toEpochMilli();
        }

        public String displayPlayerHand() {
            return displayCards(playerHand);
        }

        public String displayCommunity() {
            return communityCards.isEmpty() ? "(none)" : displayCards(communityCards);
        }

        public String displayDealerHand(boolean reveal) {
            if (reveal || isFinished())
                return displayCards(dealerHand);

            return "?? ??";
        }

        public String getResultMessage(Function<BigInteger, String> currencyFormatter) {
            if (settlement == null)
                throw new IllegalStateException("Game has not finished yet.");

            String payoutText = settlement.payout().signum() == 0
                    ? currencyFormatter.apply(settlement.bet())
                    : currencyFormatter.apply(settlement.payout());

            String handInfo = "";
            if (settlement.playerRank() != null && settlement.dealerRank() != null) {
                handInfo = " Your hand: **%s**. Dealer: **%s**."
                        .formatted(settlement.playerRank().getDisplayName(),
                                settlement.dealerRank().getDisplayName());
            }

            return switch (settlement.result()) {
                case PLAYER_WIN -> "You win %s.%s".formatted(payoutText, handInfo);
                case DEALER_WIN -> "The dealer wins. You lost %s.%s".formatted(payoutText, handInfo);
                case PUSH -> "It's a push! Your bet of %s has been returned.%s".formatted(payoutText, handInfo);
                case PLAYER_FOLD -> "You folded and lost %s.".formatted(payoutText);
                case TIMEOUT -> "Your game timed out and you lost %s.".formatted(payoutText);
            };
        }

        private void showdown() {
            List<BlackjackCommand.Card> playerCards = new ArrayList<>(playerHand);
            playerCards.addAll(communityCards);
            List<BlackjackCommand.Card> dealerCards = new ArrayList<>(dealerHand);
            dealerCards.addAll(communityCards);

            EvaluatedHand playerBest = EvaluatedHand.bestOf(playerCards);
            EvaluatedHand dealerBest = EvaluatedHand.bestOf(dealerCards);

            int comparison = playerBest.compareTo(dealerBest);
            if (comparison > 0) {
                finish(Result.PLAYER_WIN, playerBest.rank(), dealerBest.rank());
            } else if (comparison < 0) {
                finish(Result.DEALER_WIN, playerBest.rank(), dealerBest.rank());
            } else {
                finish(Result.PUSH, playerBest.rank(), dealerBest.rank());
            }
        }

        private void finish(Result result, HandRank playerRank, HandRank dealerRank) {
            if (settlement != null)
                return;

            stage = Stage.FINISHED;
            lastActionAt = Instant.now();

            BigInteger payout = switch (result) {
                case PLAYER_WIN -> totalBet.multiply(BigInteger.valueOf(2));
                case PUSH -> totalBet;
                case DEALER_WIN, PLAYER_FOLD, TIMEOUT -> BigInteger.ZERO;
            };

            settlement = new Settlement(result, totalBet, payout, playerRank, dealerRank);
        }

        private void ensureNotFinished() {
            if (settlement != null || stage == Stage.FINISHED)
                throw new IllegalStateException("Game has already finished.");
        }

        private void ensureAwaitingAction() {
            ensureNotFinished();
            if (!awaitingAction)
                throw new IllegalStateException("You have already acted this round.");
        }

        private void advanceStageOrShowdown() {
            ensureNotFinished();

            awaitingAction = false;
            switch (stage) {
                case PRE_FLOP -> {
                    communityCards.add(deck.draw());
                    communityCards.add(deck.draw());
                    communityCards.add(deck.draw());
                    stage = Stage.FLOP;
                }
                case FLOP -> {
                    communityCards.add(deck.draw());
                    stage = Stage.TURN;
                }
                case TURN -> {
                    communityCards.add(deck.draw());
                    stage = Stage.RIVER;
                }
                case RIVER -> {
                    showdown();
                    lastActionAt = Instant.now();
                    return;
                }
                case FINISHED -> throw new IllegalStateException("Game has already finished.");
            }

            awaitingAction = true;
            lastActionAt = Instant.now();
        }
    }

    private static String displayCards(List<BlackjackCommand.Card> cards) {
        if (cards.isEmpty())
            return "(none)";

        return cards.stream()
                .map(BlackjackCommand.Card::display)
                .collect(Collectors.joining(" "));
    }

    @Getter
    public enum HandRank {
        ROYAL_FLUSH("Royal Flush", 10),
        STRAIGHT_FLUSH("Straight Flush", 9),
        FOUR_OF_A_KIND("Four of a Kind", 8),
        FULL_HOUSE("Full House", 7),
        FLUSH("Flush", 6),
        STRAIGHT("Straight", 5),
        THREE_OF_A_KIND("Three of a Kind", 4),
        TWO_PAIR("Two Pair", 3),
        ONE_PAIR("One Pair", 2),
        HIGH_CARD("High Card", 1);

        private final String displayName;
        private final int strength;

        HandRank(String displayName, int strength) {
            this.displayName = displayName;
            this.strength = strength;
        }
    }

    public record EvaluatedHand(HandRank rank, List<Integer> tiebreakers) implements Comparable<EvaluatedHand> {
        public EvaluatedHand(HandRank rank, List<Integer> tiebreakers) {
            this.rank = Objects.requireNonNull(rank, "rank");
            this.tiebreakers = List.copyOf(tiebreakers);
        }

        @Override
        public int compareTo(EvaluatedHand other) {
            int rankDiff = Integer.compare(this.rank.getStrength(), other.rank.getStrength());
            if (rankDiff != 0)
                return rankDiff;

            int size = Math.min(this.tiebreakers.size(), other.tiebreakers.size());
            for (int i = 0; i < size; i++) {
                int diff = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
                if (diff != 0)
                    return diff;
            }

            return Integer.compare(this.tiebreakers.size(), other.tiebreakers.size());
        }

        public static EvaluatedHand bestOf(List<BlackjackCommand.Card> cards) {
            if (cards.size() < 5)
                throw new IllegalArgumentException("Need at least 5 cards to evaluate.");

            EvaluatedHand best = null;
            int size = cards.size();
            for (int a = 0; a < size - 4; a++) {
                for (int b = a + 1; b < size - 3; b++) {
                    for (int c = b + 1; c < size - 2; c++) {
                        for (int d = c + 1; d < size - 1; d++) {
                            for (int e = d + 1; e < size; e++) {
                                List<BlackjackCommand.Card> hand = List.of(
                                        cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e));
                                EvaluatedHand current = evaluateFive(hand);
                                if (best == null || current.compareTo(best) > 0) {
                                    best = current;
                                }
                            }
                        }
                    }
                }
            }

            return Objects.requireNonNull(best, "best");
        }

        private static EvaluatedHand evaluateFive(List<BlackjackCommand.Card> cards) {
            Map<Integer, Integer> counts = new HashMap<>();
            for (BlackjackCommand.Card card : cards) {
                counts.merge(rankValue(card.rank()), 1, Integer::sum);
            }

            boolean isFlush = cards.stream().map(BlackjackCommand.Card::suit).distinct().count() == 1;
            List<Integer> uniqueRanks = counts.keySet().stream().sorted().toList();
            int straightHigh = straightHigh(uniqueRanks);
            boolean isStraight = straightHigh > 0;

            List<Map.Entry<Integer, Integer>> groups = counts.entrySet().stream()
                    .sorted((a, b) -> {
                        int countDiff = Integer.compare(b.getValue(), a.getValue());
                        if (countDiff != 0)
                            return countDiff;

                        return Integer.compare(b.getKey(), a.getKey());
                    })
                    .toList();

            if (isStraight && isFlush) {
                return straightHigh == 14 && uniqueRanks.contains(10)
                        ? new EvaluatedHand(HandRank.ROYAL_FLUSH, List.of(14))
                        : new EvaluatedHand(HandRank.STRAIGHT_FLUSH, List.of(straightHigh));

            }

            if (groups.get(0).getValue() == 4) {
                int quad = groups.get(0).getKey();
                int kicker = groups.get(1).getKey();
                return new EvaluatedHand(HandRank.FOUR_OF_A_KIND, List.of(quad, kicker));
            }

            if (groups.get(0).getValue() == 3 && groups.size() == 2) {
                int trips = groups.get(0).getKey();
                int pair = groups.get(1).getKey();
                return new EvaluatedHand(HandRank.FULL_HOUSE, List.of(trips, pair));
            }

            if (isFlush)
                return new EvaluatedHand(HandRank.FLUSH, sortedRanksDesc(counts));

            if (isStraight)
                return new EvaluatedHand(HandRank.STRAIGHT, List.of(straightHigh));

            if (groups.get(0).getValue() == 3) {
                int trips = groups.getFirst().getKey();
                List<Integer> kickers = groups.stream()
                        .filter(entry -> entry.getValue() == 1)
                        .map(Map.Entry::getKey)
                        .sorted((a, b) -> Integer.compare(b, a))
                        .toList();
                List<Integer> tiebreakers = new ArrayList<>();
                tiebreakers.add(trips);
                tiebreakers.addAll(kickers);
                return new EvaluatedHand(HandRank.THREE_OF_A_KIND, tiebreakers);
            }

            if (groups.get(0).getValue() == 2 && groups.get(1).getValue() == 2) {
                int highPair = Math.max(groups.get(0).getKey(), groups.get(1).getKey());
                int lowPair = Math.min(groups.get(0).getKey(), groups.get(1).getKey());
                int kicker = groups.get(2).getKey();
                return new EvaluatedHand(HandRank.TWO_PAIR, List.of(highPair, lowPair, kicker));
            }

            if (groups.get(0).getValue() == 2) {
                int pair = groups.getFirst().getKey();
                List<Integer> kickers = groups.stream()
                        .filter(entry -> entry.getValue() == 1)
                        .map(Map.Entry::getKey)
                        .sorted((a, b) -> Integer.compare(b, a))
                        .toList();
                List<Integer> tiebreakers = new ArrayList<>();
                tiebreakers.add(pair);
                tiebreakers.addAll(kickers);
                return new EvaluatedHand(HandRank.ONE_PAIR, tiebreakers);
            }

            return new EvaluatedHand(HandRank.HIGH_CARD, sortedRanksDesc(counts));
        }

        private static List<Integer> sortedRanksDesc(Map<Integer, Integer> counts) {
            return counts.keySet().stream()
                    .sorted((a, b) -> Integer.compare(b, a))
                    .toList();
        }

        private static int straightHigh(List<Integer> sortedRanksAsc) {
            if (sortedRanksAsc.size() != 5)
                return 0;

            if (sortedRanksAsc.get(0) == 2
                    && sortedRanksAsc.get(1) == 3
                    && sortedRanksAsc.get(2) == 4
                    && sortedRanksAsc.get(3) == 5
                    && sortedRanksAsc.get(4) == 14)
                return 5;

            int first = sortedRanksAsc.get(0);
            for (int i = 1; i < sortedRanksAsc.size(); i++) {
                if (sortedRanksAsc.get(i) != first + i)
                    return 0;
            }

            return sortedRanksAsc.get(4);
        }

        private static int rankValue(BlackjackCommand.Card.Rank rank) {
            return switch (rank) {
                case TWO -> 2;
                case THREE -> 3;
                case FOUR -> 4;
                case FIVE -> 5;
                case SIX -> 6;
                case SEVEN -> 7;
                case EIGHT -> 8;
                case NINE -> 9;
                case TEN -> 10;
                case JACK -> 11;
                case QUEEN -> 12;
                case KING -> 13;
                case ACE -> 14;
            };
        }
    }
}
