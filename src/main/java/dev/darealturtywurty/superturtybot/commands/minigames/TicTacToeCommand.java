package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class TicTacToeCommand extends CoreCommand {
    private static final List<Game> GAMES = new ArrayList<>();
    private static final Font LETTER_FONT = new Font("Arial", Font.BOLD, 100);
    private static final BasicStroke STROKE = new BasicStroke(5);

    public TicTacToeCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of Tic Tac Toe with someone or against the bot!";
    }

    @Override
    public String getName() {
        return "tictactoe";
    }

    @Override
    public String getRichName() {
        return "Tic Tac Toe";
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

        reply(event, "✅ Creating a game of Tic Tac Toe...");

        // check that the user is in the server
        guild.retrieveMemberById(opponent.getIdLong()).queue(member -> {
            if (member == null) {
                event.getHook().editOriginal("❌ The opponent you specified is not in this server!").queue();
                return;
            }

            List<Game> games = GAMES.stream().filter(game -> game.getGuildId() == guild.getIdLong()).toList();

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
            var game = new Game(guild.getIdLong(), event.getChannel().getIdLong(), event.getUser().getIdLong(), opponent.getIdLong(), opponent.isBot());
            GAMES.add(game);

            // create the thread
            final String threadName = "Tic Tac Toe - %s vs %s".formatted(event.getUser().getName(), opponent.getName());
            event.getHook().editOriginal("✅ Successfully created a game of Tic Tac Toe!")
                    .flatMap(message ->
                            message.createThreadChannel(threadName.length() > 100 ? threadName.substring(0, 100) : threadName))
                    .queue(thread -> {
                        game.setThreadId(thread.getIdLong());
                        thread.addThreadMember(event.getUser()).queue();
                        thread.addThreadMember(opponent).queue();
                        thread.sendMessageFormat("✅ <@%d> and <@%d> have started a game of Tic Tac Toe! It is <@%d>'s turn!",
                                event.getUser().getIdLong(), opponent.getIdLong(), event.getUser().getIdLong()).queue(message -> {
                            game.setMessageId(message.getIdLong());
                            message.editMessageComponents(createRows(game))
                                    .setFiles(createFileUpload(game, thread))
                                    .flatMap(ignored -> message.pin())
                                    .queue(ignored -> createEventWaiter(game, thread).build());
                        });
                    });
        }, throwable ->
                event.getHook().editOriginal("❌ The opponent you specified is not in this server!").queue());
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createEventWaiter(Game game, ThreadChannel channel) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(event -> {
                    if (event.getGuild() == null ||
                            event.getGuild().getIdLong() != game.getGuildId() ||
                            event.getChannel().getIdLong() != game.getThreadId() ||
                            event.getMessageIdLong() != game.getMessageId())
                        return false;

                    if (!game.isTurn(event.getUser().getIdLong())) {
                        event.deferEdit().queue();
                        return false;
                    }

                    return true;
                })
                .timeout(1, TimeUnit.MINUTES)
                .timeoutAction(() -> {
                    channel.sendMessageFormat("❌ <@%d> did not make a move in time! The game has been cancelled!", game.getCurrentTurn())
                            .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                    GAMES.remove(game);
                })
                .failure(() -> {
                    channel.sendMessageFormat("❌ Something went wrong! The game has been cancelled!").queue(
                            ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                    GAMES.remove(game);
                })
                .success(event -> {
                    String[] data = event.getComponentId().split("-");
                    int x = Integer.parseInt(data[1]), y = Integer.parseInt(data[2]);

                    game.makeMove(x, y);

                    if (game.hasWon(event.getUser().getIdLong())) {
                        respondToButton(game, channel, event, false);

                        channel.sendMessageFormat("✅ <@%d> has won the game!", event.getUser().getIdLong())
                                .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                        GAMES.remove(game);

                        return;
                    } else if (handleDraw(game, channel, event))
                        return;

                    if (!game.isBot()) {
                        respondToButton(game, channel, event);
                    } else {
                        game.playBot();

                        if (game.hasWon(game.getOpponentId())) {
                            respondToButton(game, channel, event, false);

                            channel.sendMessageFormat("✅ <@%d> has won the game!", game.getOpponentId())
                                    .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                            GAMES.remove(game);

                            return;
                        } else {
                            if (handleDraw(game, channel, event))
                                return;
                        }

                        respondToButton(game, channel, event);
                    }
                });
    }

    private static boolean handleDraw(Game game, ThreadChannel channel, ButtonInteractionEvent event) {
        if (game.isDraw()) {
            respondToButton(game, channel, event, false);

            channel.sendMessageFormat("✅ The game has ended in a draw!")
                    .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
            GAMES.remove(game);

            return true;
        } else {
            channel.sendMessageFormat("✅ It is <@%d>'s turn!", game.getCurrentTurn()).queue();
        }

        return false;
    }

    private static void respondToButton(Game game, ThreadChannel channel, ButtonInteractionEvent event) {
        respondToButton(game, channel, event, true);
    }

    private static void respondToButton(Game game, ThreadChannel channel, ButtonInteractionEvent event, boolean wait) {
        FileUpload file = createFileUpload(game, channel);
        MessageEditCallbackAction editAction = event.deferEdit().setComponents(createRows(game));
        if (file != null)
            editAction.setFiles(file);

        editAction.queue(ignored -> {
            if (!channel.isLocked() && wait)
                createEventWaiter(game, channel).build();
        });
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
            GAMES.remove(game);
            return null;
        }

        return FileUpload.fromData(baos.toByteArray(), "tictactoe.png");
    }

    private static BufferedImage createImage(Game game) {
        var image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 300, 300);

        graphics.setColor(Color.BLACK);
        graphics.setStroke(STROKE);
        graphics.drawLine(100, 0, 100, 300);
        graphics.drawLine(200, 0, 200, 300);
        graphics.drawLine(0, 100, 300, 100);
        graphics.drawLine(0, 200, 300, 200);

        graphics.setFont(LETTER_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                char c = game.get(x, y);
                if (!game.isEmpty(x, y)) {
                    String character = String.valueOf(c);
                    graphics.drawString(
                            character,
                            x * 100 + 50 - metrics.stringWidth(character) / 2,
                            y * 100 + 50 + metrics.getHeight() / 2 - metrics.getDescent());
                }
            }
        }

        graphics.dispose();
        return image;
    }

    private static List<ActionRow> createRows(Game game) {
        List<ActionRow> actionRows = new ArrayList<>();
        int index = 0;
        for (int row = 0; row < 3; row++) {
            List<Button> buttons = new ArrayList<>();
            for (int column = 0; column < 3; column++) {
                // label the buttons with the index
                var button = Button.primary(
                        "tictactoe-%d-%d".formatted(column, row),
                        "%d".formatted(1 + index++));

                // disable the button if it has already been used, or it is the bot's turn
                if (!game.isEmpty(column, row) || (game.isBot() && game.isTurn(game.getOpponentId())))
                    button = button.asDisabled();

                buttons.add(button);
            }

            actionRows.add(ActionRow.of(buttons));
        }

        return actionRows;
    }

    @Getter
    public static class Game {
        private final long guildId, channelId, userId, opponentId;
        private final boolean isBot;
        private final char[][] board = new char[3][3];
        private long currentTurn;
        private long messageId, threadId;

        public Game(long guildId, long channelId, long userId, long opponentId, boolean isBot) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.userId = userId;
            this.opponentId = opponentId;
            this.isBot = isBot;
            this.currentTurn = userId;
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

        public void makeMove(int x, int y) {
            if (!isEmpty(x, y))
                return;

            board[x][y] = currentTurn == userId ? 'X' : 'O';
            switchTurn();
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

        public boolean hasWon(long userId) {
            // Top left to bottom right
            if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[2][2] == (userId == this.userId ? 'X' : 'O'))
                return true;

            // Top right to bottom left
            if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[2][0] == (userId == this.userId ? 'X' : 'O'))
                return true;

            for (int x = 0; x < 3; x++) {
                // Horizontal
                if (board[x][0] == board[x][1] && board[x][1] == board[x][2] && board[x][2] == (userId == this.userId ? 'X' : 'O'))
                    return true;

                // Vertical
                if (board[0][x] == board[1][x] && board[1][x] == board[2][x] && board[2][x] == (userId == this.userId ? 'X' : 'O'))
                    return true;
            }

            return false;
        }

        public boolean isDraw() {
            return Arrays.stream(board)
                    .flatMapToInt(row -> new String(row).chars())
                    .allMatch(column -> column != '\u0000');
        }

        public void playBot() {
            List<Map.Entry<Integer, Integer>> moves = new ArrayList<>();
            for (int column = 0; column < 3; column++) {
                for (int row = 0; row < 3; row++) {
                    if (isEmpty(column, row)) {
                        moves.add(Map.entry(column, row));
                    }
                }
            }

            Map.Entry<Integer, Integer> move = moves.get(ThreadLocalRandom.current().nextInt(moves.size()));
            makeMove(move.getKey(), move.getValue());
        }

        public char get(int x, int y) {
            return board[x][y];
        }
    }
}
