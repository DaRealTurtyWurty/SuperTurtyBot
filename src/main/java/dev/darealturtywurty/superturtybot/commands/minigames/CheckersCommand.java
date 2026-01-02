package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CheckersCommand extends CoreCommand {
    private static final List<CheckersCommand.Game> GAMES = new ArrayList<>();
    private static final Font LETTER_FONT = new Font("Arial", Font.BOLD, 100);

    public CheckersCommand() {
        super(new Types(true, false, false, false));
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createEventWaiter(CheckersCommand.Game game, ThreadChannel channel) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event -> {
                    if (!event.isFromGuild() ||
                            event.getGuild().getIdLong() != game.getGuildId() ||
                            event.getChannel().getIdLong() != game.getThreadId() ||
                            event.getAuthor().isBot() ||
                            event.getAuthor().isSystem() ||
                            event.isWebhookMessage())
                        return false;

                    return game.isTurn(event.getAuthor().getIdLong());
                })
                .timeout(1, TimeUnit.MINUTES)
                .timeoutAction(() -> skipTurn(game, channel))
                .failure(() -> {
                    channel.sendMessageFormat("❌ Something went wrong! The game has been cancelled!").queue(
                            ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                    GAMES.remove(game);
                })
                .success(event -> handleMessageReceived(event, game, channel));
    }

    private static void skipTurn(CheckersCommand.Game game, ThreadChannel channel) {
        game.switchTurn();
        channel.sendMessage("❌ You took too long to move, it is now <@%d>'s turn!".formatted(game.getCurrentTurn())).queue();

        if(game.isBot() && !game.isTurn(game.getOpponentId())) {
            Pair<Pair<Integer, Integer>,Pair<Integer, Integer>> botMove = game.playBot();
            String botFrom = "%s%s".formatted((char) (botMove.getLeft().getRight() + 'A'), botMove.getLeft().getLeft() + 1);
            String botTo = "%s%s".formatted((char) (botMove.getRight().getRight() + 'A'), botMove.getRight().getLeft() + 1);

            if (game.hasWon(game.getOpponentId())) {
                channel.sendMessageFormat("✅ <@%d> has won the game!", game.getOpponentId())
                        .setFiles(createFileUpload(game, channel))
                        .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                GAMES.remove(game);

                return;
            }

            channel.sendMessageFormat("✅ <@%d> has moved %s to %s! It is now <@%d>'s turn!", game.getOpponentId(), botFrom, botTo, game.getCurrentTurn())
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> createEventWaiter(game, channel).build());
            return;
        }

        createEventWaiter(game, channel).build();
    }

    private static void handleMessageReceived(MessageReceivedEvent event, CheckersCommand.Game game, ThreadChannel channel) {
        String data = event.getMessage().getContentRaw().trim().toUpperCase();
        if (data.equalsIgnoreCase("skip")) {
            skipTurn(game, channel);
            return;
        }

        if(data.equalsIgnoreCase("cancel")) {
            if(game.getSelectedPiece() == null) {
                channel.sendMessage("❌ You do not currently have a piece selected!")
                        .queue(ignored -> createEventWaiter(game, channel).build());
                return;
            }

            game.setSelectedPiece(event.getAuthor().getIdLong(), null);
            channel.sendMessage("✅ Successfully cancelled the selected piece! Please select a new piece to move.")
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> createEventWaiter(game, channel).build());
            return;
        }

        if(data.equalsIgnoreCase("give up")) {
            channel.sendMessageFormat("✅ <@%d> has given up! <@%d> has won the game!", event.getAuthor().getIdLong(), game.isTurn(event.getAuthor().getIdLong()) ? game.getOpponentId() : game.getUserId())
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            GAMES.remove(game);
            return;
        }

        if(data.length() != 2) {
            createEventWaiter(game, channel).build();
            return;
        }

        char[] split = data.toCharArray();

        String rowStr, columnStr;
        // row and column can be interchanged
        if (Character.isDigit(split[0])) {
            rowStr = String.valueOf(split[0]);
            columnStr = String.valueOf(split[1]);
        } else if(Character.isDigit(split[1])) {
            rowStr = String.valueOf(split[1]);
            columnStr = String.valueOf(split[0]);
        } else {
            reply(event, "❌ That is not a valid move!");
            createEventWaiter(game, channel).build();
            return;
        }

        int parsedRow = Integer.parseInt(rowStr);
        if(parsedRow < 1 || parsedRow > 8) {
            reply(event, "❌ That is not a valid row!");
            createEventWaiter(game, channel).build();
            return;
        }

        char parsedColumn = columnStr.charAt(0);
        if(parsedColumn < 'A' || parsedColumn > 'H') {
            reply(event, "❌ That is not a valid column!");
            createEventWaiter(game, channel).build();
            return;
        }

        int row = parsedRow - 1;
        int column = parsedColumn - 'A';

        if(game.getSelectedPiece() == null) {
            if(!game.setSelectedPiece(event.getAuthor().getIdLong(), Pair.of(row, column))) {
                reply(event, "❌ You do not have a piece at %s!".formatted(data));
                createEventWaiter(game, channel).build();
                return;
            }

            event.getMessage().replyFormat("✅ Selected piece at %s!\nPlease select where you would like to move to.", columnStr + rowStr)
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> createEventWaiter(game, channel).build());
            return;
        }

        Pair<Integer, Integer> selectedPiece = game.getSelectedPiece();
        if(!game.makeMove(event.getAuthor().getIdLong(), selectedPiece.getLeft(), selectedPiece.getRight(), row, column)) {
            reply(event, "❌ You cannot place a piece there!");
            createEventWaiter(game, channel).build();
            return;
        }

        game.setSelectedPiece(event.getAuthor().getIdLong(), null);

        if (game.hasWon(event.getAuthor().getIdLong())) {
            channel.sendMessageFormat("✅ <@%d> has won the game!", event.getAuthor().getIdLong())
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            GAMES.remove(game);
            return;
        }

        String from = "%s%s".formatted((char) (selectedPiece.getRight() + 'A'), selectedPiece.getLeft() + 1);
        String to = "%s%s".formatted(rowStr, columnStr);
        channel.sendMessageFormat("✅ <@%d> has moved %s to %s! It is now <@%d>'s turn!", event.getAuthor().getIdLong(), from, to, game.getCurrentTurn())
                .setFiles(createFileUpload(game, channel))
                .queue();

        if (!game.isBot()) {
            createEventWaiter(game, channel).build();
        } else {
            Pair<Pair<Integer, Integer>,Pair<Integer, Integer>> botMove = game.playBot();
            String botFrom = "%s%s".formatted((char) (botMove.getLeft().getRight() + 'A'), botMove.getLeft().getLeft() + 1);
            String botTo = "%s%s".formatted((char) (botMove.getRight().getRight() + 'A'), botMove.getRight().getLeft() + 1);

            if (game.hasWon(game.getOpponentId())) {
                channel.sendMessageFormat("✅ <@%d> has won the game!", game.getOpponentId())
                        .setFiles(createFileUpload(game, channel))
                        .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                GAMES.remove(game);

                return;
            }

            channel.sendMessageFormat("✅ <@%d> has moved %s to %s! It is now <@%d>'s turn!", game.getOpponentId(), botFrom, botTo, game.getCurrentTurn())
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> createEventWaiter(game, channel).build());
        }
    }

    private static FileUpload createFileUpload(CheckersCommand.Game game, ThreadChannel channel) {
        BufferedImage image = createImage(channel.getJDA(), game);
        if (image == null) {
            channel.sendMessageFormat("❌ Something went wrong! The game has been cancelled!").queue(
                    ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            GAMES.remove(game);
            return null;
        }

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write image!", exception);
            channel.sendMessageFormat("❌ Something went wrong! The game has been cancelled!").queue(
                    ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            GAMES.remove(game);
            return null;
        }

        return FileUpload.fromData(baos.toByteArray(), "checkers.png");
    }

    private static BufferedImage createImage(JDA jda, CheckersCommand.Game game) {
        var image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 800, 800);

        for (int row = 0; row < game.board.length; row++) {
            for (int column = 0; column < game.board[row].length; column++) {
                if((row + column) % 2 == 0) {
                    graphics.setColor(Color.BLACK);
                    if(game.getSelectedPiece() != null) {
                        if(game.getSelectedPiece().getLeft() == row && game.getSelectedPiece().getRight() == column) {
                            graphics.setColor(Color.BLUE);
                        } else {
                            List<Pair<Integer, Integer>> availableMoves = game.getAvailableMoves(game.getSelectedPiece().getLeft(), game.getSelectedPiece().getRight());

                            final int finalRow = row, finalColumn = column;
                            if(availableMoves.stream().anyMatch(pair -> pair.getLeft() == finalRow && pair.getRight() == finalColumn)) {
                                graphics.setColor(Color.GREEN);
                            }
                        }
                    }

                    graphics.fillRect(row * 100, column * 100, 100, 100);
                }
            }
        }

        BufferedImage red, yellow;

        try {
            red = ImageIO.read(jda.getUserById(game.getUserId()).getEffectiveAvatar().download().join());
            yellow = ImageIO.read(game.isBot() ? jda.getSelfUser().getEffectiveAvatar().download().join() : jda.getUserById(game.getOpponentId()).getEffectiveAvatar().download().join());
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read image!", exception);
            return null;
        }

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                char symbol = game.get(row, column);
                if (symbol != '\u0000') {
                    // draw rounded image
                    BufferedImage avatar = symbol == 'X' ? red : yellow;
                    graphics.setClip(new Ellipse2D.Float(row * 100 + 5, column * 100 + 5, 90, 90));
                    graphics.drawImage(avatar, row * 100 + 5, column * 100 + 5, 90, 90, null);
                    graphics.setClip(null);
                }
            }
        }

        graphics.dispose();

        var keyedImage = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D keyGraphics = keyedImage.createGraphics();
        keyGraphics.setColor(Color.GRAY);
        keyGraphics.fillRect(0, 0, 1000, 1000);
        keyGraphics.drawImage(image, 100, 100, null);

        keyGraphics.setFont(LETTER_FONT);
        FontMetrics metrics = keyGraphics.getFontMetrics();

        keyGraphics.setColor(Color.WHITE);
        for (int row = 0; row < 8; row++) {
            String letter = String.valueOf((char) (row + 'A'));
            keyGraphics.drawString(letter, 50 - metrics.stringWidth(letter) / 2, row * 100 + 140 + metrics.getDescent() * 2);
        }

        for (int column = 0; column < 8; column++) {
            String letter = String.valueOf(column + 1);
            keyGraphics.drawString(letter, column * 100 + 150 - metrics.stringWidth(letter) / 2, 940 + metrics.getDescent() * 2);
        }

        keyGraphics.dispose();
        return keyedImage;
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of Checkers with another user or against the bot!";
    }

    @Override
    public String getName() {
        return "checkers";
    }

    @Override
    public String getRichName() {
        return "Checkers";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "opponent", "The opponent you want to play against", false));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (event.getChannelType().isThread()) {
            reply(event, "❌ This command cannot be used in a thread!", false, true);
            return;
        }

        if (!guild.getSelfMember().hasPermission(event.getGuildChannel(), Permission.CREATE_PUBLIC_THREADS, Permission.MANAGE_THREADS)) {
            reply(event, "❌ I do not have permission to create or manage threads in this channel!", false, true);
            return;
        }

        User opponent = event.getOption("opponent", event.getJDA().getSelfUser(), OptionMapping::getAsUser);
        if (opponent == null) {
            reply(event, "❌ You must specify an opponent to play against!", false, true);
            return;
        }

        if (opponent.isBot() && event.getJDA().getSelfUser().getIdLong() != opponent.getIdLong()) {
            reply(event, "❌ You cannot play against a bot!", false, true);
            return;
        }

        if (opponent.getIdLong() == event.getUser().getIdLong()) {
            reply(event, "❌ You cannot play against yourself!", false, true);
            return;
        }

        reply(event, "✅ Creating a game of Checkers...");

        // check that the user is in the server
        guild.retrieveMemberById(opponent.getIdLong()).queue(member -> {
            if (member == null) {
                event.getHook().editOriginal("❌ The opponent you specified is not in this server!").queue();
                return;
            }

            List<CheckersCommand.Game> games = GAMES.stream().filter(game -> game.getGuildId() == guild.getIdLong()).toList();

            // check that the user is not already in a game
            if (games.stream().anyMatch(game -> game.getUserId() == event.getUser().getIdLong() || game.getOpponentId() == event.getUser().getIdLong())) {
                event.getHook().editOriginal("❌ You are already in a game!").queue();
                return;
            }

            // check that the opponent is not already in a game
            if (games.stream().anyMatch(game -> game.getUserId() == opponent.getIdLong() || game.getOpponentId() == opponent.getIdLong())) {
                event.getHook().editOriginal("❌ The opponent you specified is already in a game!").queue();
                return;
            }

            // create the game
            var game = new CheckersCommand.Game(guild.getIdLong(), event.getChannel().getIdLong(), event.getUser().getIdLong(), opponent.getIdLong(), opponent.isBot());
            GAMES.add(game);

            // create the thread
            final String threadName = "Checkers - %s vs %s".formatted(event.getUser().getName(), opponent.getName());
            event.getHook().editOriginal("✅ Successfully created a game of Checkers!")
                    .flatMap(message ->
                            message.createThreadChannel(threadName.length() > 100 ? threadName.substring(0, 100) : threadName))
                    .queue(thread -> {
                        game.setThreadId(thread.getIdLong());
                        thread.addThreadMember(event.getUser()).queue();
                            thread.addThreadMember(opponent).queue();
                            thread.sendMessageFormat("✅ <@%d> and <@%d> have started a game of Checkers! It is <@%d>'s turn!",
                                    event.getUser().getIdLong(), opponent.getIdLong(), event.getUser().getIdLong()).queue(message -> {
                                game.setMessageId(message.getIdLong());
                                message.editMessage("Select a piece to move!")
                                        .setFiles(createFileUpload(game, thread))
                                        .flatMap(ignored -> message.pin())
                                    .queue(ignored -> createEventWaiter(game, thread).build());
                        });
                    });
        }, throwable ->
                event.getHook().editOriginal("❌ The opponent you specified is not in this server!").queue());
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Getter
    public static class Game {
        private final long guildId, channelId, userId, opponentId;
        private final boolean isBot;
        private final char[][] board = new char[8][8];
        private long currentTurn;
        private long messageId, threadId;
        private Pair<Integer, Integer> selectedPiece;

        public Game(long guildId, long channelId, long userId, long opponentId, boolean isBot) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.userId = userId;
            this.opponentId = opponentId;
            this.isBot = isBot;
            this.currentTurn = userId;

            fillBoard();
        }

        // -O-O-O-O
        // O-O-O-O-
        // -O-O-O-O
        // --------
        // --------
        // X-X-X-X-
        // -X-X-X-X
        // X-X-X-X-
        private void fillBoard() {
            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 8; column++) {
                    if (row < 3) {
                        board[column][row] = (column + row) % 2 == 0 ? 'O' : '\u0000';
                    } else if (row > 4) {
                        board[column][row] = (column + row) % 2 == 0 ? 'X' : '\u0000';
                    } else {
                        board[column][row] = '\u0000';
                    }
                }
            }
        }

        public void setMessageId(long messageId) {
            if (this.messageId != 0)
                return;

            this.messageId = messageId;
        }

        public void setThreadId(long threadId) {
            if (this.threadId != 0)
                return;

            this.threadId = threadId;
        }

        public boolean setSelectedPiece(long userId, @Nullable Pair<Integer, Integer> selectedPiece) {
            if (selectedPiece == null) {
                this.selectedPiece = null;
                return true;
            }

            int row = selectedPiece.getLeft();
            int column = selectedPiece.getRight();

            char symbol = userId == this.userId ? 'X' : 'O';
            if(get(row, column) == symbol) {
                this.selectedPiece = selectedPiece;
                return true;
            }

            return false;
        }

        public boolean makeMove(long userId, int fromRow, int fromColumn, int toRow, int toColumn) {
            if (!canPlace(userId, fromRow, fromColumn, toRow, toColumn))
                return false;

            board[toRow][toColumn] = board[fromRow][fromColumn];
            board[fromRow][fromColumn] = '\u0000';
            switchTurn();
            return true;
        }

        public boolean isEmpty(int x, int y) {
            return get(x, y) == '\u0000';
        }

        private void switchTurn() {
            currentTurn = isTurn(userId) ? opponentId : userId;
        }

        public boolean isTurn(long userId) {
            return currentTurn == userId;
        }

        // go through the board and check if the opponent has no spaces left
        public boolean hasWon(long userId) {
            char opponentSymbol = userId == this.userId ? 'O' : 'X';
            return Arrays.stream(board)
                    .flatMapToInt(row -> new String(row).chars())
                    .noneMatch(column -> column == opponentSymbol);
        }

        public Pair<Pair<Integer, Integer>,Pair<Integer, Integer>> playBot() {
            List<Pair<Integer, Integer>> availablePieces = getMovablePiecesForCurrentPlayer();
            Pair<Integer, Integer> selectedPiece = availablePieces.get(ThreadLocalRandom.current().nextInt(availablePieces.size()));
            List<Pair<Integer, Integer>> availableMoves = getAvailableMoves(selectedPiece.getLeft(), selectedPiece.getRight());
            while (availableMoves.isEmpty() && !availablePieces.isEmpty()) {
                availablePieces.remove(selectedPiece);
                selectedPiece = availablePieces.get(ThreadLocalRandom.current().nextInt(availablePieces.size()));
                availableMoves = getAvailableMoves(selectedPiece.getLeft(), selectedPiece.getRight());
            }

            Pair<Integer, Integer> selectedMove = availableMoves.get(ThreadLocalRandom.current().nextInt(availableMoves.size()));
            if(makeMove(this.opponentId, selectedPiece.getLeft(), selectedPiece.getRight(), selectedMove.getLeft(), selectedMove.getRight())) {
                return Pair.of(selectedPiece, selectedMove);
            } else {
                return playBot();
            }
        }

        public char get(int row, int column) {
            if (row < 0 || row >= 8 || column < 0 || column >= 8)
                return '#';

            return board[row][column];
        }

        public boolean canPlace(long userId, int fromRow, int fromColumn, int toRow, int toColumn) {
            if(!isTurn(userId))
                return false;

            if (fromRow < 0 || fromRow >= 8 || fromColumn < 0 || fromColumn >= 8 || toRow < 0 || toRow >= 8 || toColumn < 0 || toColumn >= 8)
                return false;

            char symbol = userId == this.userId ? 'X' : 'O';
            char opponentSymbol = userId == this.userId ? 'O' : 'X';
            if (get(fromRow, fromColumn) != symbol)
                return false;

            char toSymbol = get(toRow, toColumn);
            // only allow the user to place their piece in an empty space or the opponent's piece
            if (toSymbol != '\u0000' && toSymbol != opponentSymbol)
                return false;

            List<Pair<Integer, Integer>> availableMoves = getAvailableMoves(fromRow, fromColumn);
            return availableMoves.stream().anyMatch(pair -> pair.getLeft() == toRow && pair.getRight() == toColumn);
        }

        @Deprecated
        public long getUserIdFromPosition(int row, int column) {
            if (row < 0 || row >= 8 || column < 0 || column >= 8)
                return 0;

            return switch (column) {
                case 0, 2, 4, 6 -> row % 2 == 0 ? userId : opponentId;
                case 1, 3, 5, 7 -> row % 2 == 0 ? opponentId : userId;
                default -> -1L;
            };
        }

        public List<Pair<Integer, Integer>> getMovablePiecesForCurrentPlayer() {
            List<Pair<Integer, Integer>> pieces = new ArrayList<>();
            for (int x = 0; x < this.board.length; x++) {
                for (int y = 0; y < this.board[x].length; y++) {
                    if (get(x, y) == (isBot ? 'O' : 'X')) {
                        pieces.add(Pair.of(x, y));
                    }
                }
            }

            return pieces;
        }

        public List<Pair<Integer, Integer>> getAvailableMoves(int row, int column) {
            List<Pair<Integer, Integer>> moves = new ArrayList<>();
            if (get(row, column) == (this.userId == this.currentTurn ? 'X' : 'O')) {
                char opponentSymbol = this.userId == this.currentTurn ? 'O' : 'X';
                if (row > 0 && column > 0 && (isEmpty(row - 1, column - 1) || get(row - 1, column - 1) == opponentSymbol)) {
                    moves.add(Pair.of(row - 1, column - 1));
                }

                if (row > 0 && column < 7 && (isEmpty(row - 1, column + 1) || get(row - 1, column + 1) == opponentSymbol)) {
                    moves.add(Pair.of(row - 1, column + 1));
                }

                if (row < 7 && column > 0 && (isEmpty(row + 1, column - 1) || get(row + 1, column - 1) == opponentSymbol)) {
                    moves.add(Pair.of(row + 1, column - 1));
                }

                if (row < 7 && column < 7 && (isEmpty(row + 1, column + 1) || get(row + 1, column + 1) == opponentSymbol)) {
                    moves.add(Pair.of(row + 1, column + 1));
                }
            }

            return moves;
        }
    }
}