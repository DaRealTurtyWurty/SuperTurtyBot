package dev.darealturtywurty.superturtybot.commands.economy.blackjack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.economy.EconomyCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BlackjackCommand extends EconomyCommand {
    protected static final Map<Long, List<BlackjackCommand.Game>> GAMES = new ConcurrentHashMap<>();

    public BlackjackCommand() {
        addSubcommands(
                new BlackjackPlayCommand(),
                new BlackjackHitCommand(),
                new BlackjackStandCommand(),
                new BlackjackHowToPlayCommand()
        );
    }

    public static Game getOngoingGame(SlashCommandInteractionEvent event) {
        List<Game> games = GAMES.get(event.getGuild().getIdLong());
        if (games == null)
            return null;

        for (Game game : games) {
            if (game.getGuild() == event.getGuild().getIdLong() && game.getUser() == event.getUser().getIdLong() && !game.isFinished())
                return game;
        }

        return null;
    }

    @Override
    public String getDescription() {
        return "Play a game of blackjack against the bot.";
    }

    @Override
    public String getName() {
        return "blackjack";
    }

    @Override
    public String getRichName() {
        return "Blackjack";
    }

    @Override
    public String getHowToUse() {
        return "/blackjack <bet>";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 0L);
    }

    @Data
    public static class Game {
        private static final boolean DEALER_STANDS_ON_SOFT_17 = true;
        private static final int DECKS_IN_SHOE = 1;
        private static final boolean BLACKJACK_PAYS_3_TO_2 = true;

        private final long guild;
        private final long channel;
        private final long user;
        private final BigInteger bet;

        private final Deck deck;
        private final Hand playerHand = new Hand();
        private final Hand dealerHand = new Hand();

        private Status status = Status.PLAYER_TURN;

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
            this.deck = new Deck(DECKS_IN_SHOE);
        }

        public void start() {
            ensureNotStartedOrFinished();

            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());
            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());

            lastActionAt = Instant.now();

            boolean playerBJ = playerHand.isBlackjack();
            boolean dealerBJ = dealerHand.isBlackjack();

            if (playerBJ || dealerBJ) {
                if (playerBJ && dealerBJ) {
                    finish(Result.PUSH);
                } else if (playerBJ) {
                    finish(Result.PLAYER_BLACKJACK);
                } else {
                    finish(Result.DEALER_BLACKJACK);
                }
            } else {
                status = Status.PLAYER_TURN;
            }
        }

        public Card hit() {
            ensureStatus(Status.PLAYER_TURN);

            Card drawn = deck.draw();
            playerHand.add(drawn);
            lastActionAt = Instant.now();

            if (playerHand.isBust()) {
                finish(Result.PLAYER_BUST);
                return drawn;
            }

            if (playerHand.bestValue() == 21) {
                stand();
            }

            return drawn;
        }

        public void stand() {
            ensureStatus(Status.PLAYER_TURN);

            status = Status.DEALER_TURN;
            lastActionAt = Instant.now();

            playDealerTurn();

            if (dealerHand.isBust()) {
                finish(Result.DEALER_BUST);
                return;
            }

            int playerValue = playerHand.bestValue();
            int dealerValue = dealerHand.bestValue();

            if (playerValue > dealerValue) {
                finish(Result.PLAYER_WIN);
            } else if (dealerValue > playerValue) {
                finish(Result.DEALER_WIN);
            } else {
                finish(Result.PUSH);
            }
        }

        public boolean isFinished() {
            return status == Status.FINISHED;
        }

        public Optional<Settlement> getSettlement() {
            return Optional.ofNullable(settlement);
        }

        public String displayDealerHand(boolean revealHoleCard) {
            if (dealerHand.cardCount() == 0)
                return "(empty)";

            if (revealHoleCard || status != Status.PLAYER_TURN)
                return dealerHand.display();

            return dealerHand.getCards().getFirst().display() + " ??";
        }

        private void playDealerTurn() {
            while (true) {
                int dealerValue = dealerHand.bestValue();

                if (dealerValue < 17) {
                    dealerHand.add(deck.draw());
                    continue;
                }

                if (dealerValue > 17)
                    break; // Dealer busts

                if (!DEALER_STANDS_ON_SOFT_17 && dealerHand.isSoft()) {
                    dealerHand.add(deck.draw());
                    continue;
                }

                break; // Dealer stands
            }
        }

        private void finish(Result result) {
            if (status == Status.FINISHED)
                return;

            status = Status.FINISHED;
            lastActionAt = Instant.now();

            BigInteger payout = switch (result) {
                case PUSH -> bet;
                case PLAYER_WIN, DEALER_BUST -> bet.multiply(BigInteger.valueOf(2));
                case PLAYER_BLACKJACK -> BLACKJACK_PAYS_3_TO_2
                        ? bet.multiply(BigInteger.valueOf(5)).divide(BigInteger.valueOf(2))
                        : bet.multiply(BigInteger.valueOf(2));
                case DEALER_WIN, PLAYER_BUST, DEALER_BLACKJACK, TIMEOUT -> BigInteger.ZERO;
            };

            settlement = new Settlement(result, bet, payout);
        }

        private void ensureStatus(Status expected) {
            if (status != expected)
                throw new IllegalStateException("Invalid game state: expected %s but was %s".formatted(expected, status));

            if (settlement != null)
                throw new IllegalStateException("Game has already finished.");
        }

        private void ensureNotStartedOrFinished() {
            if (settlement != null || status == Status.FINISHED)
                throw new IllegalStateException("Game has already finished.");

            if (playerHand.cardCount() > 0 || dealerHand.cardCount() > 0)
                throw new IllegalStateException("Game has already started.");
        }

        public long getLastActionTime() {
            return lastActionAt.toEpochMilli();
        }

        public void timeout() {
            if (isFinished())
                return;

            finish(Result.TIMEOUT);
        }

        public String getResultMessage(Function<BigInteger, String> currencyFormatter) {
            if (settlement == null)
                throw new IllegalStateException("Game has not finished yet.");

            return switch (settlement.result()) {
                case PLAYER_BUST ->
                        "You busted and lost your bet of %s.".formatted(currencyFormatter.apply(settlement.bet()));
                case DEALER_BUST ->
                        "The dealer busted! You win %s.".formatted(currencyFormatter.apply(settlement.payout()));
                case PLAYER_WIN -> "You win! You receive %s.".formatted(currencyFormatter.apply(settlement.payout()));
                case DEALER_WIN ->
                        "The dealer wins. You lost your bet of %s.".formatted(currencyFormatter.apply(settlement.bet()));
                case PUSH ->
                        "It's a push! Your bet of %s has been returned.".formatted(currencyFormatter.apply(settlement.bet()));
                case PLAYER_BLACKJACK ->
                        "Blackjack! You win %s.".formatted(currencyFormatter.apply(settlement.payout()));
                case DEALER_BLACKJACK ->
                        "The dealer has blackjack. You lost your bet of %s.".formatted(currencyFormatter.apply(settlement.bet()));
                case TIMEOUT ->
                        "Your game has timed out due to inactivity. You lost your bet of %s.".formatted(currencyFormatter.apply(settlement.bet()));
            };
        }

        public enum Status {
            PLAYER_TURN,
            DEALER_TURN,
            FINISHED;
        }

        public enum Result {
            PLAYER_BUST,
            DEALER_BUST,
            PLAYER_WIN,
            DEALER_WIN,
            PUSH,
            PLAYER_BLACKJACK,
            DEALER_BLACKJACK,
            TIMEOUT;
        }

        public record Settlement(Result result, BigInteger bet, BigInteger payout) {
            public Settlement(Result result, BigInteger bet, BigInteger payout) {
                this.result = Objects.requireNonNull(result, "result cannot be null");
                this.bet = Objects.requireNonNull(bet, "bet cannot be null");
                this.payout = Objects.requireNonNull(payout, "payout cannot be null");
            }
        }
    }

    public static final class Hand {
        private final List<Card> cards = new ArrayList<>();

        public Hand(Card... initialCards) {
            Collections.addAll(this.cards, initialCards);
        }

        public Hand(Collection<Card> initialCards) {
            this.cards.addAll(initialCards);
        }

        public void add(Card card) {
            cards.add(card);
        }

        public List<Card> getCards() {
            return Collections.unmodifiableList(cards);
        }

        public int cardCount() {
            return cards.size();
        }

        public int bestValue() {
            int total = 0;
            int aces = 0;

            for (Card card : cards) {
                total += card.baseValue();
                if (card.isAce()) {
                    aces++;
                }
            }

            while (total > 21 && aces > 0) {
                total -= 10;
                aces--;
            }

            return total;
        }

        public boolean isBust() {
            return bestValue() > 21;
        }

        public boolean isSoft() {
            int total = 0;
            int aces = 0;

            for (Card card : cards) {
                total += card.baseValue();
                if (card.isAce()) {
                    aces++;
                }
            }

            while (total > 21 && aces > 0) {
                total -= 10;
                aces--;
            }

            return aces > 0;
        }

        public boolean isBlackjack() {
            return cards.size() == 2 && bestValue() == 21;
        }

        public String display() {
            if (cards.isEmpty())
                return "(empty)";

            var sb = new StringBuilder();
            for (int i = 0; i < cards.size(); i++) {
                if (i > 0) {
                    sb.append(' ');
                }

                sb.append(cards.get(i).display());
            }

            return sb.toString();
        }
    }

    public static final class Deck {
        private final Random random;
        private final List<Card> cards;

        public Deck(int decks, Random rand) {
            if (decks <= 0)
                throw new IllegalArgumentException("Number of decks must be greater than zero.");

            this.random = rand;
            this.cards = new ArrayList<>(52 * decks);

            for (int i = 0; i < decks; i++) {
                for (Card.Suit suit : Card.Suit.values()) {
                    for (Card.Rank rank : Card.Rank.values()) {
                        cards.add(new Card(rank, suit));
                    }
                }
            }

            shuffle();
        }

        public Deck(int decks) {
            this(decks, new Random());
        }

        public void shuffle() {
            Collections.shuffle(cards, random);
        }

        public int size() {
            return cards.size();
        }

        public boolean isEmpty() {
            return cards.isEmpty();
        }

        public Card draw() {
            if (isEmpty())
                throw new IllegalStateException("The deck is empty.");

            return cards.removeLast();
        }

        public Card drawOrReshuffle(int decksToRebuildIfEmpty) {
            if (isEmpty()) {
                cards.clear();
                for (int i = 0; i < decksToRebuildIfEmpty; i++) {
                    for (Card.Suit suit : Card.Suit.values()) {
                        for (Card.Rank rank : Card.Rank.values()) {
                            cards.add(new Card(rank, suit));
                        }
                    }
                }

                shuffle();
            }

            return draw();
        }
    }

    public record Card(Rank rank, Suit suit) {
        public int baseValue() {
            return rank.getBaseValue();
        }

        public boolean isAce() {
            return rank.isAce();
        }

        public String display() {
            return "<:" + getEmojiName() + ":" + getEmojiId() + ">";
        }

        public String getAssetName() {
            return rank.getName() + "_of_" + suit.getName();
        }

        public String getFriendlyName() {
            return toTitleCase(rank.name()) + " of " + toTitleCase(suit.name());
        }

        @Override
        public @NotNull String toString() {
            return display();
        }

        private String getEmojiName() {
            return rank.getName() + "_of_" + suit.getName();
        }

        private long getEmojiId() {
            InputStream inputStream = TurtyBot.loadResource("cards/cards.json");
            if (inputStream == null)
                throw new IllegalStateException("Could not load cards.json resource.");

            JsonObject cardsJson = Constants.GSON.fromJson(new InputStreamReader(inputStream), JsonObject.class);
            String emojiName = rank.getName() + "_of_" + suit.getName();
            JsonObject environmentEmojis = cardsJson.getAsJsonObject(Environment.INSTANCE.isDevelopment() ? "dev" : "prod");
            if (!environmentEmojis.has(emojiName))
                throw new IllegalStateException("Emoji for card %s not found.".formatted(emojiName));

            JsonElement emojiElement = environmentEmojis.get(emojiName);
            if (!emojiElement.isJsonPrimitive())
                throw new IllegalStateException("Emoji entry for card %s is not a primitive.".formatted(emojiName));

            JsonPrimitive emojiPrimitive = emojiElement.getAsJsonPrimitive();
            if (!emojiPrimitive.isNumber())
                throw new IllegalStateException("Emoji entry for card %s is not a number.".formatted(emojiName));

            try {
                return emojiElement.getAsLong();
            } catch (Exception exception) {
                throw new IllegalStateException("Emoji entry for card %s is not a valid long.".formatted(emojiName), exception);
            }
        }

        private static String toTitleCase(String value) {
            String lower = value.toLowerCase(Locale.ROOT);
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }

        @Getter
        public enum Rank {
            TWO(2, "2"),
            THREE(3, "3"),
            FOUR(4, "4"),
            FIVE(5, "5"),
            SIX(6, "6"),
            SEVEN(7, "7"),
            EIGHT(8, "8"),
            NINE(9, "9"),
            TEN(10, "10"),
            JACK(10, "J"),
            QUEEN(10, "Q"),
            KING(10, "K"),
            ACE(11, "A");

            private final int baseValue;
            private final String display;
            private final String name = name().toLowerCase(Locale.ROOT);

            Rank(int baseValue, String display) {
                this.baseValue = baseValue;
                this.display = display;
            }

            public boolean isAce() {
                return this == ACE;
            }
        }

        @Getter
        public enum Suit {
            CLUBS("♣"),
            DIAMONDS("♦"),
            HEARTS("♥"),
            SPADES("♠");

            private final String symbol;
            private final String name = name().toLowerCase(Locale.ROOT);

            Suit(String symbol) {
                this.symbol = symbol;
            }
        }
    }
}
