package dev.darealturtywurty.superturtybot.commands.minigames;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveGeneratorException;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ChessCommand extends CoreCommand {
    private static final BufferedImage[] PIECE_IMAGES = Arrays.stream(Piece.allPieces)
            .filter(piece -> piece != Piece.NONE)
            .map(piece -> TurtyBot.loadImage("chess/" + piece.name().toLowerCase() + ".png"))
            .toArray(BufferedImage[]::new);
    private static final BufferedImage BACKGROUND_IMAGE = createBackgroundImage();

    private static BufferedImage createBackgroundImage() {
        var image = new BufferedImage(900, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, 900, 900);

        graphics.setFont(new Font("Arial", Font.BOLD, 100));
        FontMetrics metrics = graphics.getFontMetrics();

        graphics.setColor(Color.WHITE);
        for (int row = 0; row < 8; row++) {
            String letter = String.valueOf(8 - row);
            graphics.drawString(letter, 50 - metrics.stringWidth(letter) / 2, row * 100 + 90);
        }

        for (int column = 0; column < 8; column++) {
            String letter = String.valueOf((char) (column + 'A'));
            graphics.drawString(letter, column * 100 + 150 - metrics.stringWidth(letter) / 2, 890);
        }
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Square square = Square.squareAt(row * 8 + column);
                graphics.setColor(square.isLightSquare() ?
                        new Color(0xffce9e) :
                        new Color(0xd18b47));

                graphics.fillRect(column * 100 + 100, (7 - row) * 100, 100, 100);
            }
        }

        graphics.dispose();
        return image;
    }

    public ChessCommand() {
        super(new Types(true, false, false, true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of chess!";
    }

    @Override
    public String getName() {
        return "chess";
    }

    @Override
    public String getRichName() {
        return "Chess";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.USER, "opponent", "The opponent you want to play against", true),
                new OptionData(OptionType.STRING, "fen", "The FEN string to start the game with", false)
        );
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Member member = event.getOption("opponent", null, OptionMapping::getAsMember);
        challengeOpponent(event, member, event.getOption("fen", null, OptionMapping::getAsString));
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        challengeOpponent(event, event.getTargetMember(), null);
    }

    private static void challengeOpponent(GenericCommandInteractionEvent event, Member opponent, @Nullable String startingFen) {
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

        if (opponent == null) {
            reply(event, "❌ The opponent you specified is not in this server!", false, true);
            return;
        }

        if (opponent.getUser().isBot()) {
            reply(event, "❌ You cannot play against a bot!", false, true);
            return;
        }

        if (opponent.getUser().getIdLong() == event.getUser().getIdLong()) {
            reply(event, "❌ You cannot play against yourself!", false, true);
            return;
        }

        var game = new Game(guild.getIdLong(), event.getUser().getIdLong(), opponent.getUser().getIdLong());
        if (startingFen != null) {
            try {
                game.board.loadFromFen(startingFen);
                game.board.legalMoves();
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException | MoveGeneratorException e) {
                reply(event, "❌ Invalid FEN string!", false, true);
                return;
            }
        }

        event.reply("✅ Waiting for %s to accept the game of Chess...%s".formatted(
                opponent.getAsMention(), startingFen != null ? "\nStarting FEN: `%s`".formatted(startingFen) : ""))
                .setActionRow(Button.success("chess:accept", "Accept"))
                .flatMap(InteractionHook::retrieveOriginal)
                .queue(message -> TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                        .condition(buttonEvent ->
                                buttonEvent.getChannelIdLong() == message.getChannel().getIdLong() &&
                                buttonEvent.getMessageIdLong() == message.getIdLong() &&
                                buttonEvent.getUser().getIdLong() == opponent.getUser().getIdLong())
                        .timeout(2, TimeUnit.MINUTES)
                        .timeoutAction(() -> message.editMessage("❌ %s did not accept in time!".formatted(opponent.getAsMention())).setComponents().queue())
                        .success(buttonEvent -> {
                            if ("chess:accept".equals(buttonEvent.getButton().getId())) {
                                startGame(event, game, opponent);
                            }
                        })
                        .build());
    }

    private static void startGame(GenericCommandInteractionEvent event, Game game, Member opponent) {
        final String threadName = "Chess - %s vs %s".formatted(event.getUser().getName(), opponent.getUser().getName());
        event.getHook().editOriginal("✅ Successfully created a game of Chess!")
                .setComponents()
                .flatMap(message ->
                        message.createThreadChannel(threadName.length() > 100 ? threadName.substring(0, 100) : threadName))
                .queue(thread -> {
                    game.setThreadId(thread.getIdLong());
                    thread.addThreadMember(event.getUser()).queue();
                    thread.addThreadMember(opponent).queue();
                    thread.sendMessageFormat("✅ %s and %s have started a game of Chess! It is <@%d>'s turn!",
                            event.getUser().getAsMention(), opponent.getUser().getAsMention(), game.getCurrentTurnId()).queue(message -> {
                        game.setMessageId(message.getIdLong());
                        message.editMessage("Select a piece to move!")
                                .setFiles(createFileUpload(game, thread))
                                .flatMap(ignored -> message.pin())
                                .queue(ignored -> createEventWaiter(game, thread).build());
                    });
                });
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createEventWaiter(Game game, ThreadChannel channel) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event ->
                        event.isFromGuild() &&
                        event.getGuild().getIdLong() == game.guildId &&
                        event.getChannel().getIdLong() == game.threadId &&
                        !event.getAuthor().isBot() &&
                        !event.getAuthor().isSystem() &&
                        !event.isWebhookMessage() && game.isTurn(event.getAuthor().getIdLong()))
                .timeout(10, TimeUnit.MINUTES)
                .timeoutAction(() -> channel.sendMessageFormat("❌ <@%d> did not make a move in time!", game.getCurrentTurnId()).queue(
                        ignored -> channel.getManager().setArchived(true).setLocked(true).queue()))
                .failure(() -> channel.sendMessageFormat("❌ Something went wrong! The game has been cancelled!").queue(
                        ignored -> channel.getManager().setArchived(true).setLocked(true).queue()))
                .success(event -> handleMessageReceived(event, game, channel));
    }

    private static void handleMessageReceived(MessageReceivedEvent event, Game game, ThreadChannel channel) {
        String messageContent = event.getMessage().getContentRaw().trim();

        switch (messageContent) {
            case "cancel" -> {
                if (game.selectedSquare == null) {
                    channel.sendMessage("❌ You do not currently have a piece selected!")
                            .queue(ignored -> createEventWaiter(game, channel).build());
                    return;
                }

                game.selectedSquare = null;
                channel.sendMessage("✅ Successfully cancelled the selected piece!")
                        .setFiles(createFileUpload(game, channel))
                        .queue(ignored -> createEventWaiter(game, channel).build());
                return;
            }
            case "give up", "resign" -> {
                channel.sendMessageFormat("✅ <@%d> has given up! <@%d> has won the game!", event.getAuthor().getIdLong(), game.getUserIdFromSide(game.board.getSideToMove().flip()))
                        .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                return;
            }
            case "getfen" -> {
                reply(event, "✅ This game's FEN is: `%s`".formatted(game.board.getFen()), true);
                createEventWaiter(game, channel).build();
                return;
            }
            case "getmoves" -> {
                reply(event, "✅ This game's moves are:\n%s".formatted(game.getMovesText()
                ), true);
                createEventWaiter(game, channel).build();
                return;
            }
        }

        if (game.selectedSquare != null) {
            Square square = null;
            try {
                square = Square.fromValue(messageContent.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
            if (square != null) {
                if (square == game.selectedSquare) {
                    game.selectedSquare = null;
                    channel.sendMessage("✅ Successfully cancelled the selected piece!")
                            .setFiles(createFileUpload(game, channel))
                            .queue(ignored -> createEventWaiter(game, channel).build());
                    return;
                }

                Move move = new Move(game.selectedSquare, square);
                if (tryToMakeMoveAndSendMessage(event, game, channel, move)) return;
            }
        }

        try {
            if (!messageContent.equals("Z0") && game.board.doMove(messageContent)) {
                Move move = game.board.undoMove();
                if (move != null) {
                    if (tryToMakeMoveAndSendMessage(event, game, channel, move)) return;
                }
            }
        } catch (MoveConversionException | IllegalArgumentException ignored) {
        }

        Square square = null;
        try {
            square = Square.fromValue(messageContent.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
        }

        if (square != null && game.selectedSquare == null && game.board.getPiece(square).getPieceSide() == game.board.getSideToMove()) {
            if (game.getAvailableMoves(square).isEmpty()) {
                event.getMessage().replyFormat("❌ There are no moves available from %s!", square.value()).queue();
                createEventWaiter(game, channel).build();
                return;
            }
            game.selectedSquare = square;

            event.getMessage().replyFormat("✅ Selected piece at %s!\nPlease select where you would like to move to.", square.value())
                    .setFiles(createFileUpload(game, channel))
                    .queue(ignored -> createEventWaiter(game, channel).build());
            return;
        }

        createEventWaiter(game, channel).build();
    }

    private static boolean tryToMakeMoveAndSendMessage(MessageReceivedEvent event, Game game, ThreadChannel channel, Move move) {
        if (!game.makeMove(move)) return false;
        game.selectedSquare = null;
        String from = move.getFrom().value();
        String to = move.getTo().value();
        channel.sendMessageFormat("✅ %s has moved %s to %s! It is now <@%d>'s turn!", event.getAuthor().getAsMention(), from, to, game.getCurrentTurnId())
                .setFiles(createFileUpload(game, channel))
                .queue();

        if (game.board.isMated()) {
            channel.sendMessageFormat("✅ %s has won the game!\nThe ending board's FEN is: `%s`\nThe games moves are: %s",
                            event.getAuthor().getAsMention(),
                            game.board.getFen(),
                            game.getMovesText()
                    )
                    .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            return true;
        }

        createEventWaiter(game, channel).build();
        return true;
    }


    private static FileUpload createFileUpload(Game game, ThreadChannel channel) {
        BufferedImage image = createImage(game);

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write image!", exception);
            channel.sendMessageFormat("❌ Something went wrong! The game has been cancelled!").queue(
                    ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            return null;
        }

        return FileUpload.fromData(baos.toByteArray(), "board.png");
    }

    private static BufferedImage createImage(Game game) {
        var image = new BufferedImage(900, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(BACKGROUND_IMAGE, 0, 0, null);

        boolean kingAttacked = game.board.isKingAttacked();
        Piece sideToMoveKing = Piece.make(game.board.getSideToMove(), PieceType.KING);
        List<Move> availableMoves = game.getAvailableMoves(game.selectedSquare);
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Square square = Square.squareAt(row * 8 + column);
                Piece piece = game.board.getPiece(square);

                Color color = null;
                if (kingAttacked && piece == sideToMoveKing) {
                    color = new Color(255, 0, 0, 150);
                }

                if (game.selectedSquare != null) {
                    if (game.selectedSquare == square) {
                        color = new Color(0, 0, 255, 150);
                    } else {
                        if (availableMoves.stream().anyMatch(move -> move.getTo() == square)) {
                            color = new Color(0, 255, 0, 100);
                        }
                    }
                }
                if (color != null) {
                    graphics.setColor(color);
                    graphics.fillRect(column * 100 + 100, (7 - row) * 100, 100, 100);
                }

                if (piece == Piece.NONE) continue;
                BufferedImage pieceImage = PIECE_IMAGES[piece.ordinal()];
                graphics.drawImage(pieceImage, column * 100 + 100, (7 - row) * 100, 100, 100, null);
            }
        }

        graphics.dispose();
        return image;
    }

    public static class Game {
        private final Board board = new Board();
        private final long guildId;
        private final long userId;
        private final long opponentId;
        private long messageId, threadId;
        private Square selectedSquare;

        public Game(long guildId, long userId, long opponentId) {
            this.guildId = guildId;
            this.userId = userId;
            this.opponentId = opponentId;
        }

        public String getMovesText() {
            return board.getBackup()
                    .stream()
                    .map(moveBackup -> "%s%s".formatted(
                            moveBackup.getMove().getFrom().value().toLowerCase(Locale.ROOT),
                            moveBackup.getMove().getTo().value().toLowerCase(Locale.ROOT)
                    ))
                    .collect(Collectors.joining(" "));
        }

        public Side getSideFromUserId(long userId) {
            return userId == this.userId ? Side.WHITE : Side.BLACK;
        }

        public long getUserIdFromSide(Side side) {
            return side == Side.WHITE ? userId : opponentId;
        }

        public long getCurrentTurnId() {
            return getUserIdFromSide(board.getSideToMove());
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

        public boolean makeMove(Move move) {
            return board.legalMoves().contains(move) && board.doMove(move);
        }

        public boolean isTurn(long userId) {
            return getCurrentTurnId() == userId;
        }

        public List<Move> getAvailableMoves(Square square) {
            return board.legalMoves().stream().filter(move -> move.getFrom() == square).toList();
        }
    }
}
