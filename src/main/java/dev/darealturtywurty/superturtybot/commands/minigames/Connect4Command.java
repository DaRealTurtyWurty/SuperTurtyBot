package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Connect4Command extends CoreCommand {
    private static final List<Connect4Command.Game> GAMES = new ArrayList<>();
    private static final BasicStroke STROKE = new BasicStroke(5);

    public Connect4Command() {
        super(new Types(true, false, false, false));
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createEventWaiter(Connect4Command.Game game, ThreadChannel channel) {
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
                    game.makeMove(Integer.parseInt(data[1]));

                    if (game.hasWon(event.getUser().getIdLong())) {
                        respondToButton(game, channel, event, false);

                        channel.sendMessageFormat("✅ <@%d> has won the game!", event.getUser().getIdLong())
                                .setFiles(createFileUpload(game, channel))
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
                                    .setFiles(createFileUpload(game, channel))
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

    private static boolean handleDraw(Connect4Command.Game game, ThreadChannel channel, ButtonInteractionEvent event) {
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

    private static void respondToButton(Connect4Command.Game game, ThreadChannel channel, ButtonInteractionEvent event) {
        respondToButton(game, channel, event, true);
    }

    private static void respondToButton(Connect4Command.Game game, ThreadChannel channel, ButtonInteractionEvent event, boolean wait) {
        FileUpload file = createFileUpload(game, channel);
        MessageEditCallbackAction editAction = event.deferEdit().setComponents(createRows(game));
        if (file != null)
            editAction.setFiles(file);

        editAction.queue(ignored -> {
            if (!channel.isLocked() && wait)
                createEventWaiter(game, channel).build();
        });
    }

    private static FileUpload createFileUpload(Connect4Command.Game game, ThreadChannel channel) {
        BufferedImage image = createImage(channel.getJDA(), game);
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

        return FileUpload.fromData(baos.toByteArray(), "connect4.png");
    }

    private static BufferedImage createImage(JDA jda, Connect4Command.Game game) {
        var image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 700, 700);

        graphics.setColor(Color.BLACK);
        graphics.setStroke(STROKE);
        for (int x = 0; x < 8; x++) {
            graphics.drawLine(x * 100, 0, x * 100, 700);
        }

        for (int y = 0; y < 8; y++) {
            graphics.drawLine(0, y * 100, 700, y * 100);
        }

        BufferedImage red = null;
        BufferedImage yellow = null;

        try {
            red = ImageIO.read(jda.getUserById(game.getUserId()).getEffectiveAvatar().download().join());
            yellow = ImageIO.read(game.isBot() ? jda.getSelfUser().getEffectiveAvatar().download().join() : jda.getUserById(game.getOpponentId()).getEffectiveAvatar().download().join());
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read image!", exception);
        }

        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                char symbol = game.get(x, y);
                if (symbol != '\u0000') {
                    // draw rounded image
                    BufferedImage avatar = symbol == 'X' ? red : yellow;
                    graphics.setClip(new Ellipse2D.Float(x * 100 + 5, y * 100 + 5, 90, 90));
                    graphics.drawImage(avatar, x * 100 + 5, y * 100 + 5, 90, 90, null);
                    graphics.setClip(null);
                }
            }
        }

        graphics.dispose();
        return image;
    }

    // Add buttons 1-7 (there can only be 5 buttons in a row)
    private static List<ActionRow> createRows(Connect4Command.Game game) {
        List<ActionRow> actionRows = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < game.board.length; i++) {
            var button = Button.primary(
                    "connect4-%d".formatted(i),
                    "%d".formatted(i + 1));

            if (!game.canPlace(i) || (game.isBot() && !game.isTurn(game.getUserId())))
                button = button.asDisabled();

            buttons.add(button);
            if (buttons.size() == 5) {
                actionRows.add(ActionRow.of(buttons));
                buttons.clear();
            }
        }

        if (!buttons.isEmpty()) {
            actionRows.add(ActionRow.of(buttons));
        }

        return actionRows;
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of Connect 4 with another user or against the bot!";
    }

    @Override
    public String getName() {
        return "connect4";
    }

    @Override
    public String getRichName() {
        return "Connect 4";
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

        reply(event, "✅ Creating a game of Connect 4...");

        // check that the user is in the server
        guild.retrieveMemberById(opponent.getIdLong()).queue(member -> {
            if (member == null) {
                event.getHook().editOriginal("❌ The opponent you specified is not in this server!").queue();
                return;
            }

            List<Connect4Command.Game> games = GAMES.stream().filter(game -> game.getGuildId() == guild.getIdLong()).toList();

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
            var game = new Connect4Command.Game(guild.getIdLong(), event.getChannel().getIdLong(), event.getUser().getIdLong(), opponent.getIdLong(), opponent.isBot());
            GAMES.add(game);

            // create the thread
            final String threadName = "Connect 4 - %s vs %s".formatted(event.getUser().getName(), opponent.getName());
            event.getHook().editOriginal("✅ Successfully created a game of Connect 4!")
                    .flatMap(message ->
                            message.createThreadChannel(threadName.length() > 100 ? threadName.substring(0, 100) : threadName))
                    .queue(thread -> {
                        game.setThreadId(thread.getIdLong());
                        thread.addThreadMember(event.getUser()).queue();
                        thread.addThreadMember(opponent).queue();
                        thread.sendMessageFormat("✅ <@%d> and <@%d> have started a game of Connect 4! It is <@%d>'s turn!",
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

    @Getter
    public static class Game {
        private final long guildId, channelId, userId, opponentId;
        private final boolean isBot;
        private final char[][] board = new char[7][7];
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

        public void makeMove(int column) {
            if (!canPlace(column))
                return;

            int row = findLowestEmptyRow(column);
            if (row == -1)
                return;

            board[column][row] = isTurn(userId) ? 'X' : 'O';
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

        // Check for 4 in a row or column or diagonal
        public boolean hasWon(long userId) {
            char symbol = userId == this.userId ? 'X' : 'O';

            // Check for 4 in a row
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 4; x++) {
                    if (board[x][y] == symbol && board[x + 1][y] == symbol && board[x + 2][y] == symbol && board[x + 3][y] == symbol) {
                        return true;
                    }
                }
            }

            // Check for 4 in a column
            for (int x = 0; x < 7; x++) {
                for (int y = 0; y < 4; y++) {
                    if (board[x][y] == symbol && board[x][y + 1] == symbol && board[x][y + 2] == symbol && board[x][y + 3] == symbol) {
                        return true;
                    }
                }
            }

            // Check for 4 in an upward-right diagonal
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    if (board[x][y] == symbol && board[x + 1][y + 1] == symbol && board[x + 2][y + 2] == symbol && board[x + 3][y + 3] == symbol) {
                        return true;
                    }
                }
            }

            // Check for 4 in an downward-right diagonal
            for (int x = 0; x < 4; x++) {
                for (int y = 3; y < 7; y++) {
                    if (board[x][y] == symbol && board[x + 1][y - 1] == symbol && board[x + 2][y - 2] == symbol && board[x + 3][y - 3] == symbol) {
                        return true;
                    }
                }
            }

            // Check for 4 in an upward-left diagonal
            for (int x = 3; x < 7; x++) {
                for (int y = 0; y < 4; y++) {
                    if (board[x][y] == symbol && board[x - 1][y + 1] == symbol && board[x - 2][y + 2] == symbol && board[x - 3][y + 3] == symbol) {
                        return true;
                    }
                }
            }

            // Check for 4 in a downward-left diagonal
            for (int x = 3; x < 7; x++) {
                for (int y = 3; y < 7; y++) {
                    if (board[x][y] == symbol && board[x - 1][y - 1] == symbol && board[x - 2][y - 2] == symbol && board[x - 3][y - 3] == symbol) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean isDraw() {
            return Arrays.stream(board)
                    .flatMapToInt(row -> new String(row).chars())
                    .allMatch(column -> column != '\u0000');
        }

        public void playBot() {
            List<Integer> availableColumns = getAvailableColumns();
            makeMove(availableColumns.get(ThreadLocalRandom.current().nextInt(availableColumns.size())));
        }

        public char get(int x, int y) {
            return board[x][y];
        }

        public boolean canPlace(int column) {
            return board[column][0] == '\u0000';
        }

        public List<Integer> getAvailableColumns() {
            List<Integer> availableColumns = new ArrayList<>();
            for (int column = 0; column < this.board.length; column++) {
                if (canPlace(column)) {
                    availableColumns.add(column);
                }
            }

            return availableColumns;
        }

        public int findLowestEmptyRow(int column) {
            char[] columnData = this.board[column];
            for (int i = columnData.length - 1; i >= 0; i--) {
                if (columnData[i] == '\u0000') {
                    return i;
                }
            }

            return -1;
        }
    }
}