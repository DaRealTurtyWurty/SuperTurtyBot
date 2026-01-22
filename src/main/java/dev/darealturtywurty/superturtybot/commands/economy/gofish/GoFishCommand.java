package dev.darealturtywurty.superturtybot.commands.economy.gofish;

import dev.darealturtywurty.superturtybot.commands.economy.EconomyCommand;
import dev.darealturtywurty.superturtybot.commands.economy.blackjack.BlackjackCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import lombok.Getter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class GoFishCommand extends EconomyCommand {
    protected static final Map<Long, Game> GAMES = new ConcurrentHashMap<>();
    static final int MIN_PLAYERS = 2;
    static final int MAX_PLAYERS = 6;
    static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private static final long INACTIVITY_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

    public GoFishCommand() {
        addSubcommands(
                new GoFishCreateSubcommand(),
                new GoFishHowToPlaySubcommand(),
                new GoFishAskSubcommand(),
                new GoFishHandSubcommand(),
                new GoFishStatusSubcommand(),
                new GoFishLeaveSubcommand()
        );
    }

    @Override
    public String getDescription() {
        return "Play a multiplayer game of Go Fish in this channel.";
    }

    @Override
    public String getName() {
        return "gofish";
    }

    @Override
    public String getRichName() {
        return "Go Fish";
    }

    @Override
    public String getHowToUse() {
        return "/gofish create <bet>";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 0L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        event.getHook().editOriginal("Use `/gofish create <bet>` to start a game, then join via the button.").queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("gofish:"))
            return;

        String[] parts = componentId.split(":");
        if (parts.length < 3)
            return;

        String action = parts[1];
        long channelId;
        try {
            channelId = Long.parseLong(parts[2]);
        } catch (NumberFormatException exception) {
            return;
        }

        if (event.getGuild() == null || event.getChannel().getIdLong() != channelId) {
            event.reply("❌ This Go Fish join button is no longer valid.").setEphemeral(true).queue();
            return;
        }

        Game game = GAMES.get(channelId);
        if (game == null) {
            event.reply("❌ This Go Fish game no longer exists.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        Guild guild = event.getGuild();
        GuildData config = GuildData.getOrCreateGuildData(guild);

        synchronized (game) {
            if (game.isStarted()) {
                event.getHook().editOriginal("❌ This Go Fish game has already started.").queue();
                return;
            }

            switch (action) {
                case "join" -> {
                    game.touch();
                    handleJoin(event, guild, config, game);
                }
                case "start" -> {
                    game.touch();
                    handleForceStart(event, guild, config, game);
                }
                case "cancel" -> {
                    game.touch();
                    handleCancel(event, guild, config, game);
                }
                default -> event.getHook().editOriginal("❌ Unknown action.").queue();
            }
        }
    }

    private void handleJoin(ButtonInteractionEvent event, Guild guild, GuildData config, Game game) {
        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        Game existing = findGameByUser(guild.getIdLong(), event.getUser().getIdLong());
        if (existing != null) {
            if (existing.getChannelId() == game.getChannelId()) {
                event.getHook().editOriginal("❌ You are already in this Go Fish game.").queue();
            } else {
                event.getHook().editOriginal("❌ You are already in a Go Fish game in another channel.").queue();
            }

            return;
        }

        if (game.isFull()) {
            event.getHook().editOriginal("❌ This Go Fish game is full.").queue();
            return;
        }

        if (game.getBet().compareTo(account.getWallet()) > 0) {
            event.getHook().editOriginal("❌ You cannot bet more than you have in your wallet!").queue();
            return;
        }

        EconomyManager.removeMoney(account, game.getBet(), false);
        EconomyManager.updateAccount(account);
        game.addPlayer(event.getUser().getIdLong(), game.getBet());

        event.getHook().editOriginal("✅ You joined the Go Fish game!").queue();
        event.getChannel().sendMessageFormat(
                "✅ %s joined the Go Fish game. Players: %d/%d | Bet: %s",
                event.getUser().getAsMention(),
                game.playerCount(),
                game.getMaxPlayers(),
                StringUtils.numberFormat(game.getBet(), config)
        ).queue();

        updateLobbyMessage(guild, config, game, false);
    }

    private void handleForceStart(ButtonInteractionEvent event, Guild guild, GuildData config, Game game) {
        if (game.getHostId() != event.getUser().getIdLong()) {
            event.getHook().editOriginal("❌ Only the host can force start this game.").queue();
            return;
        }

        if (game.playerCount() < MIN_PLAYERS) {
            event.getHook().editOriginal("❌ You need at least 2 players to start Go Fish.").queue();
            return;
        }

        cancelAutoStart(game);
        TextChannel lobby = guild.getTextChannelById(game.getLobbyChannelId());
        if (lobby == null) {
            event.getHook().editOriginal("❌ Unable to start the game because the lobby channel is missing.").queue();
            return;
        }

        event.getHook().editOriginal("✅ Starting Go Fish...").queue();
        startGameInThread(guild, config, game, lobby, false);
    }

    private void handleCancel(ButtonInteractionEvent event, Guild guild, GuildData config, Game game) {
        if (game.getHostId() != event.getUser().getIdLong()) {
            event.getHook().editOriginal("❌ Only the host can cancel this game.").queue();
            return;
        }

        cancelAutoStart(game);
        refundAndCancel(guild, config, game,
                "⚠️ The Go Fish game was canceled by the host. All bets were refunded.");
        event.getHook().editOriginal("✅ Go Fish game canceled. All bets have been refunded.").queue();
    }

    static Game getGame(long channelId) {
        return GAMES.get(channelId);
    }

    static Game findGameByUser(long guildId, long userId) {
        for (Game game : GAMES.values()) {
            if (game.getGuildId() == guildId && game.hasPlayer(userId)) {
                return game;
            }
        }

        return null;
    }

    static void removeGame(Game game) {
        if (game != null) {
            GAMES.remove(game.getLobbyChannelId());
            GAMES.remove(game.getChannelId());
        }
    }

    static void addGameChannelMapping(Game game, long channelId) {
        if (game != null) {
            GAMES.put(channelId, game);
        }
    }

    static void scheduleAutoStart(Guild guild, Game game) {
        ScheduledFuture<?> future = SCHEDULER.schedule(() -> autoStartGame(guild, game), 2, TimeUnit.MINUTES);
        game.setAutoStartFuture(future);
    }

    static void scheduleInactivityWatch(Guild guild, Game game) {
        ScheduledFuture<?> future = SCHEDULER.scheduleAtFixedRate(
                () -> checkInactivity(guild, game),
                10,
                10,
                TimeUnit.SECONDS
        );
        game.setInactivityFuture(future);
    }

    static void cancelInactivityWatch(Game game) {
        ScheduledFuture<?> future = game.getInactivityFuture();
        if (future != null) {
            future.cancel(false);
            game.setInactivityFuture(null);
        }
    }

    private static void checkInactivity(Guild guild, Game game) {
        synchronized (game) {
            if (!game.isStarted() || game.isFinished())
                return;

            if (System.currentTimeMillis() - game.getLastActionMillis() >= INACTIVITY_TIMEOUT_MS) {
                GuildData config = GuildData.getOrCreateGuildData(guild);
                refundAndCancel(guild, config, game,
                        "⏱️ The Go Fish game timed out due to inactivity. All bets were refunded.");
                closeGameThread(guild, game);
                cancelInactivityWatch(game);
            }
        }
    }

    static void cancelAutoStart(Game game) {
        ScheduledFuture<?> future = game.getAutoStartFuture();
        if (future != null) {
            future.cancel(false);
            game.setAutoStartFuture(null);
        }
    }

    private static void autoStartGame(Guild guild, Game game) {
        synchronized (game) {
            if (game.isStarted() || game.isFinished())
                return;

            game.setAutoStartFuture(null);

            GuildData config = GuildData.getOrCreateGuildData(guild);
            if (game.playerCount() >= MIN_PLAYERS) {
                TextChannel lobby = guild.getTextChannelById(game.getLobbyChannelId());
                if (lobby == null) {
                    refundAndCancel(guild, config, game,
                            "⚠️ Not enough players joined. The Go Fish game was canceled and all bets were refunded.");
                    return;
                }
                startGameInThread(guild, config, game, lobby, true);
            } else {
                refundAndCancel(guild, config, game,
                        "⚠️ Not enough players joined. The Go Fish game was canceled and all bets were refunded.");
            }
        }
    }

    static void refundAndCancel(Guild guild, GuildData config, Game game, String message) {
        for (PlayerState player : game.getPlayerStates()) {
            Economy account = EconomyManager.getOrCreateAccount(guild, player.userId());
            EconomyManager.addMoney(account, game.getBet(), false);
            EconomyManager.updateAccount(account);
        }

        cancelInactivityWatch(game);
        removeGame(game);
        updateLobbyMessage(guild, config, game, true);
        sendChannelMessage(guild, game, message);
    }

    static void updateLobbyMessage(Guild guild, GuildData config, Game game, boolean closeLobby) {
        TextChannel channel = guild.getTextChannelById(game.getLobbyChannelId());
        if (channel == null || game.getLobbyMessageId() == 0L)
            return;

        channel.retrieveMessageById(game.getLobbyMessageId()).queue(message -> {
            String content = buildLobbyMessage(game, config, closeLobby);
            Button joinButton = Button.success("gofish:join:" + game.getChannelId(), "Join Game")
                    .withDisabled(closeLobby || game.isFull() || game.isStarted());
            Button startButton = Button.primary("gofish:start:" + game.getChannelId(), "Force Start")
                    .withDisabled(closeLobby || game.isStarted());
            Button cancelButton = Button.danger("gofish:cancel:" + game.getChannelId(), "Cancel")
                    .withDisabled(closeLobby || game.isStarted());
            message.editMessage(content).setComponents(ActionRow.of(joinButton, startButton, cancelButton)).queue();
        }, ignored -> {
        });
    }

    private static String buildLobbyMessage(Game game, GuildData config, boolean closed) {
        String status = closed ? " (lobby closed)" : "";
        return "🃏 **Go Fish** created by <@" + game.getHostId() + ">" + status + "\n"
                + "Bet: " + StringUtils.numberFormat(game.getBet(), config)
                + " | Players: " + game.playerCount() + "/" + game.getMaxPlayers()
                + "\nJoin with the button below.";
    }

    private static void sendChannelMessage(Guild guild, Game game, String message) {
        TextChannel channel = guild.getTextChannelById(game.getChannelId());
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }

    private static void startGameInThread(Guild guild, GuildData config, Game game, TextChannel lobby, boolean autoStart) {
        lobby.createThreadChannel("Go Fish Game").queue(thread -> {
            game.setChannelId(thread.getIdLong());
            addGameChannelMapping(game, thread.getIdLong());
            game.start();
            scheduleInactivityWatch(guild, game);
            updateLobbyMessage(guild, config, game, true);

            for (PlayerState player : game.getPlayerStates()) {
                thread.addThreadMemberById(player.userId()).queue();
            }

            String startPrefix = autoStart ? "🃏 **Go Fish** started automatically!" : "🃏 **Go Fish** started!";
            thread.sendMessage(startPrefix + "\nFirst turn: <@" + game.getCurrentPlayerId() + ">\n"
                    + "It's your turn! Use `/gofish ask <player> <rank>` or `/gofish hand`.").queue();
        }, ignored -> {
        });
    }

    static void closeGameThread(Guild guild, Game game) {
        var thread = guild.getThreadChannelById(game.getChannelId());
        if (thread != null) {
            thread.getManager().setArchived(true).setLocked(true).queue();
        }
    }

    static OptionData buildRankOption() {
        OptionData option = new OptionData(OptionType.STRING, "rank", "The rank you want to ask for.", true);
        option.addChoice("Ace", "A");
        option.addChoice("2", "2");
        option.addChoice("3", "3");
        option.addChoice("4", "4");
        option.addChoice("5", "5");
        option.addChoice("6", "6");
        option.addChoice("7", "7");
        option.addChoice("8", "8");
        option.addChoice("9", "9");
        option.addChoice("10", "10");
        option.addChoice("Jack", "J");
        option.addChoice("Queen", "Q");
        option.addChoice("King", "K");
        return option;
    }

    static BlackjackCommand.Card.Rank parseRank(String value) {
        if (value == null)
            return null;

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "A", "ACE" -> BlackjackCommand.Card.Rank.ACE;
            case "2" -> BlackjackCommand.Card.Rank.TWO;
            case "3" -> BlackjackCommand.Card.Rank.THREE;
            case "4" -> BlackjackCommand.Card.Rank.FOUR;
            case "5" -> BlackjackCommand.Card.Rank.FIVE;
            case "6" -> BlackjackCommand.Card.Rank.SIX;
            case "7" -> BlackjackCommand.Card.Rank.SEVEN;
            case "8" -> BlackjackCommand.Card.Rank.EIGHT;
            case "9" -> BlackjackCommand.Card.Rank.NINE;
            case "10" -> BlackjackCommand.Card.Rank.TEN;
            case "J", "JACK" -> BlackjackCommand.Card.Rank.JACK;
            case "Q", "QUEEN" -> BlackjackCommand.Card.Rank.QUEEN;
            case "K", "KING" -> BlackjackCommand.Card.Rank.KING;
            default -> null;
        };
    }

    static String formatRank(BlackjackCommand.Card.Rank rank) {
        return switch (rank) {
            case ACE -> "Ace";
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case TEN -> "10";
            case JACK -> "Jack";
            case QUEEN -> "Queen";
            case KING -> "King";
        };
    }

    static String renderHand(PlayerState player) {
        if (player.hand().isEmpty())
            return "(empty)";

        var builder = new StringBuilder();
        for (int i = 0; i < player.hand().size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(player.hand().get(i).display());
        }
        return builder.toString();
    }

    static String renderBooks(PlayerState player) {
        if (player.books().isEmpty())
            return "(none)";

        var joiner = new StringJoiner(", ");
        for (BlackjackCommand.Card.Rank rank : player.books()) {
            joiner.add(formatRank(rank));
        }
        return joiner.toString();
    }

    @Getter
    public static final class Game {
        private final long guildId;
        private final long lobbyChannelId;
        private long channelId;
        private final long hostId;
        private final BigInteger bet;
        private final int maxPlayers;
        private final Map<Long, PlayerState> players = new LinkedHashMap<>();
        private final BlackjackCommand.Deck deck = new BlackjackCommand.Deck(1);
        private final Instant createdAt = Instant.now();
        private Instant lastActionAt = createdAt;
        private boolean started;
        private boolean finished;
        private List<Long> turnOrder = new ArrayList<>();
        private int turnIndex;
        private long lobbyMessageId;
        private ScheduledFuture<?> autoStartFuture;
        private ScheduledFuture<?> inactivityFuture;

        Game(long guildId, long channelId, long hostId, BigInteger bet, int maxPlayers) {
            this.guildId = guildId;
            this.lobbyChannelId = channelId;
            this.channelId = channelId;
            this.hostId = hostId;
            this.bet = bet;
            this.maxPlayers = maxPlayers;
        }

        boolean hasPlayer(long userId) {
            return players.containsKey(userId);
        }

        PlayerState getPlayer(long userId) {
            return players.get(userId);
        }

        int playerCount() {
            return players.size();
        }

        boolean isFull() {
            return players.size() >= maxPlayers;
        }

        Collection<PlayerState> getPlayerStates() {
            return players.values();
        }

        void addPlayer(long userId, BigInteger betPaid) {
            players.put(userId, new PlayerState(userId, betPaid));
        }

        void setLobbyMessageId(long lobbyMessageId) {
            this.lobbyMessageId = lobbyMessageId;
        }

        void setAutoStartFuture(ScheduledFuture<?> autoStartFuture) {
            this.autoStartFuture = autoStartFuture;
        }

        void setInactivityFuture(ScheduledFuture<?> inactivityFuture) {
            this.inactivityFuture = inactivityFuture;
        }

        long getLastActionMillis() {
            return lastActionAt.toEpochMilli();
        }

        void setChannelId(long channelId) {
            this.channelId = channelId;
        }

        void removePlayer(long userId) {
            players.remove(userId);
        }

        boolean canStart() {
            return !started && players.size() >= MIN_PLAYERS;
        }

        long getCurrentPlayerId() {
            if (turnOrder.isEmpty())
                return -1L;

            return turnOrder.get(turnIndex);
        }

        void start() {
            if (started)
                return;

            started = true;
            turnOrder = new ArrayList<>(players.keySet());
            Collections.shuffle(turnOrder);
            turnIndex = 0;

            int handSize = players.size() <= 3 ? 7 : 5;
            for (int i = 0; i < handSize; i++) {
                for (PlayerState player : players.values()) {
                    player.hand().add(deck.draw());
                }
            }

            for (PlayerState player : players.values()) {
                completeBooks(player);
            }

            lastActionAt = Instant.now();
        }

        void markFinished() {
            finished = true;
            lastActionAt = Instant.now();
        }

        boolean shouldEnd() {
            return totalBooks() == 13 || (deck.isEmpty() && players.values().stream().allMatch(player -> player.hand().isEmpty()));
        }

        int totalBooks() {
            return players.values().stream().mapToInt(player -> player.books().size()).sum();
        }

        List<Long> determineWinners() {
            int maxBooks = players.values().stream().mapToInt(player -> player.books().size()).max().orElse(0);
            List<Long> winners = new ArrayList<>();
            for (PlayerState player : players.values()) {
                if (player.books().size() == maxBooks) {
                    winners.add(player.userId());
                }
            }

            return winners;
        }

        void advanceTurn() {
            if (turnOrder.isEmpty())
                return;

            int attempts = 0;
            do {
                turnIndex = (turnIndex + 1) % turnOrder.size();
                attempts++;
            } while (attempts <= turnOrder.size()
                    && players.get(turnOrder.get(turnIndex)) != null
                    && players.get(turnOrder.get(turnIndex)).hand().isEmpty()
                    && deck.isEmpty());

            lastActionAt = Instant.now();
        }

        void touch() {
            lastActionAt = Instant.now();
        }

        List<BlackjackCommand.Card.Rank> completeBooks(PlayerState player) {
            Map<BlackjackCommand.Card.Rank, Integer> counts = new EnumMap<>(BlackjackCommand.Card.Rank.class);
            for (BlackjackCommand.Card card : player.hand()) {
                counts.merge(card.rank(), 1, Integer::sum);
            }

            List<BlackjackCommand.Card.Rank> completed = new ArrayList<>();
            for (Map.Entry<BlackjackCommand.Card.Rank, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= 4 && player.books().add(entry.getKey())) {
                    completed.add(entry.getKey());
                }
            }

            if (!completed.isEmpty()) {
                player.hand().removeIf(card -> completed.contains(card.rank()));
            }

            return completed;
        }
    }

    record PlayerState(long userId, BigInteger betPaid, List<BlackjackCommand.Card> hand,
                       Set<BlackjackCommand.Card.Rank> books) {
        PlayerState(long userId, BigInteger betPaid) {
            this(userId, betPaid, new ArrayList<>(), new LinkedHashSet<>());
        }

        boolean hasRank(BlackjackCommand.Card.Rank rank) {
            for (BlackjackCommand.Card card : hand) {
                if (card.rank() == rank) {
                    return true;
                }
            }

            return false;
        }

        List<BlackjackCommand.Card> removeAllOfRank(BlackjackCommand.Card.Rank rank) {
            List<BlackjackCommand.Card> removed = new ArrayList<>();
            Iterator<BlackjackCommand.Card> iterator = hand.iterator();
            while (iterator.hasNext()) {
                BlackjackCommand.Card card = iterator.next();
                if (card.rank() == rank) {
                    removed.add(card);
                    iterator.remove();
                }
            }

            return removed;
        }
    }
}
