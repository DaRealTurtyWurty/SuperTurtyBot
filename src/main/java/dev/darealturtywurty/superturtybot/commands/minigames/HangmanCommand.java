package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.request.RandomWordRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class HangmanCommand extends CoreCommand {
    private static final List<Game> GAMES = new ArrayList<>();
    private static final BasicStroke THIN_STROKE = new BasicStroke(3);
    private static final BasicStroke THICK_STROKE = new BasicStroke(5);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 50);
    private static final Font LETTER_FONT = new Font("Arial", Font.BOLD, 25);
    private static final List<Consumer<Graphics2D>> STAGES;

    static {
        List<Consumer<Graphics2D>> stages = new ArrayList<>();

        stages.add(graphics -> {
            graphics.setStroke(THICK_STROKE);
            graphics.drawLine(100, 400, 400, 400);
        });

        stages.add(graphics -> graphics.drawLine(250, 400, 250, 100));

        stages.add(graphics -> graphics.drawLine(250, 100, 350, 100));

        stages.add(graphics -> {
            graphics.setStroke(THIN_STROKE);
            graphics.drawLine(350, 100, 350, 150);
        });

        stages.add(graphics -> {
            graphics.setStroke(THICK_STROKE);
            graphics.drawOval(325, 150, 50, 50);
        });

        stages.add(graphics -> {
            graphics.setStroke(THIN_STROKE);
            graphics.drawLine(350, 200, 350, 300);
        });

        stages.add(graphics -> {
            graphics.setStroke(THICK_STROKE);
            graphics.drawLine(350, 300, 325, 350);
        });

        stages.add(graphics -> graphics.drawLine(350, 300, 375, 350));

        stages.add(graphics -> {
            graphics.setStroke(THIN_STROKE);
            graphics.drawLine(350, 225, 325, 250);
        });

        stages.add(graphics -> graphics.drawLine(350, 225, 375, 250));

        STAGES = List.copyOf(stages);
    }

    public HangmanCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play a game of hangman!";
    }

    @Override
    public String getName() {
        return "hangman";
    }

    @Override
    public String getRichName() {
        return "Hangman";
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
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

        if (GAMES.stream().anyMatch(game -> game.getGuildId() == guild.getIdLong() && game.getUserId() == event.getUser().getIdLong())) {
            reply(event, "❌ You are already playing a game of hangman in this server!", false, true);
            return;
        }

        event.deferReply().setContent("Creating a game of hangman...").queue();

        Either<Game, HttpStatus> response = Game.create(guild.getIdLong(), event.getChannel().getIdLong(), event.getUser().getIdLong());
        if (response.isRight()) {
            HttpStatus status = response.getRight();
            if (status == HttpStatus.NOT_FOUND) {
                event.getHook().editOriginal("❌ No words were found!").queue();
                return;
            }

            event.getHook().editOriginal("❌ An error occurred while creating a game of hangman!").queue();
            return;
        }

        Game game = response.getLeft();
        event.getHook().editOriginal("✅ Game of hangman created!").queue(message ->
                message.createThreadChannel(event.getUser().getName() + "'s Hangman Game").queue(thread -> {
                    game.setThreadId(thread.getIdLong());
                    GAMES.add(game);

                    // Create hangman image
                    BufferedImage hangmanImage = createHangmanImage(game);
                    try (FileUpload upload = createFileUpload(hangmanImage)) {
                        if (upload == null) {
                            thread.sendMessage("❌ An error occurred while creating a hangman image!").queue(ignored -> {
                                thread.delete().queue();
                                GAMES.remove(game);
                            });
                            return;
                        }

                        thread.sendMessage("The word is: `" + "_ ".repeat(game.getWord().length()).trim() + "` (" + game.getWord().length() + " letters)")
                                .setFiles(upload)
                                .queue(ignored -> thread.sendMessage("Guess a letter by typing it in chat!").queue());

                        createWaiter(game, thread).build();
                    } catch (IOException exception) {
                        Constants.LOGGER.error("An error occurred while uploading a hangman image!", exception);
                        thread.sendMessage("❌ An error occurred while uploading a hangman image!").queue(ignored -> {
                            thread.delete().queue();
                            GAMES.remove(game);
                        });
                    }
                }));
    }

    private static FileUpload createFileUpload(BufferedImage image) {
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while creating a hangman image!", exception);
            return null;
        }

        return FileUpload.fromData(baos.toByteArray(), "hangman.png");
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createWaiter(Game game, ThreadChannel threadChannel) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event -> event.getGuild().getIdLong() == game.getGuildId()
                        && event.getChannel().getIdLong() == game.getThreadId()
                        && event.getAuthor().getIdLong() == game.getUserId()
                        && event.getMessage().getContentRaw().length() == 1
                        && Character.isLetter(event.getMessage().getContentRaw().charAt(0)))
                .timeout(2, TimeUnit.MINUTES)
                .timeoutAction(() -> {
                    threadChannel.sendMessage("❌ You took too long to guess a letter!").queue(ignored ->
                            threadChannel.getManager().setLocked(true).setArchived(true).queue());
                    GAMES.remove(game);
                })
                .success(event -> {
                    String guess = event.getMessage().getContentRaw().toLowerCase(Locale.ROOT);
                    boolean isCorrect = game.guess(guess);
                    if (!isCorrect) {
                        threadChannel.sendMessage("❌ `" + guess + "` is not in the word!").queue();
                    }

                    if (game.hasWon()) {
                        threadChannel.sendMessage("✅ `" + guess + "` is in the word!").queue();

                        try (FileUpload upload = createFileUpload(createHangmanImage(game))) {
                            if (upload == null) {
                                threadChannel.sendMessage("❌ An error occurred while creating a hangman image!").queue(ignored -> {
                                    threadChannel.getManager().setLocked(true).setArchived(true).queue();
                                    GAMES.remove(game);
                                });
                                return;
                            }

                            threadChannel.sendMessage("✅ You won! The word was `" + game.getWord() + "`!")
                                    .setFiles(upload)
                                    .queue(ignored -> threadChannel.getManager().setLocked(true).setArchived(true).queue());
                            GAMES.remove(game);
                        } catch (IOException exception) {
                            Constants.LOGGER.error("An error occurred while uploading a hangman image!", exception);
                            threadChannel.sendMessage("❌ An error occurred while uploading a hangman image!").queue(ignored -> {
                                threadChannel.getManager().setLocked(true).setArchived(true).queue();
                                GAMES.remove(game);
                            });
                        }

                        return;
                    }

                    if (game.hasLost()) {
                        try (FileUpload upload = createFileUpload(createHangmanImage(game))) {
                            if (upload == null) {
                                threadChannel.sendMessage("❌ An error occurred while creating a hangman image!").queue(ignored -> {
                                    threadChannel.getManager().setLocked(true).setArchived(true).queue();
                                    GAMES.remove(game);
                                });

                                return;
                            }

                            threadChannel.sendMessage("❌ You lost! The word was `" + game.getWord() + "`!")
                                    .setFiles(upload)
                                    .queue(ignored -> threadChannel.getManager().setLocked(true).setArchived(true).queue());
                            GAMES.remove(game);
                        } catch (IOException exception) {
                            Constants.LOGGER.error("An error occurred while uploading a hangman image!", exception);
                            threadChannel.sendMessage("❌ An error occurred while uploading a hangman image!").queue(ignored -> {
                                threadChannel.getManager().setLocked(true).setArchived(true).queue();
                                GAMES.remove(game);
                            });
                        }

                        return;
                    }

                    if (!isCorrect) {
                        try (FileUpload upload = createFileUpload(createHangmanImage(game))) {
                            if (upload == null) {
                                threadChannel.sendMessage("❌ An error occurred while creating a hangman image!").queue(ignored -> {
                                    threadChannel.getManager().setLocked(true).setArchived(true).queue();
                                    GAMES.remove(game);
                                });

                                return;
                            }

                            threadChannel.sendMessage("The word is: `" + game.getWord().chars()
                                            .mapToObj(character -> (char) character)
                                            .map(character -> game.getGuessedLetters().contains(character) ? character.toString() : "_")
                                            .reduce((first, second) -> first + " " + second).orElse("") + "` (" + game.getWord().length() + " letters)")
                                    .setFiles(upload)
                                    .queue(ignored -> createWaiter(game, threadChannel).build());
                        } catch (IOException exception) {
                            Constants.LOGGER.error("An error occurred while uploading a hangman image!", exception);
                            threadChannel.sendMessage("❌ An error occurred while uploading a hangman image!").queue(ignored -> {
                                threadChannel.getManager().setLocked(true).setArchived(true).queue();
                                GAMES.remove(game);
                            });
                        }

                        return;
                    }

                    threadChannel.sendMessage("✅ `" + guess + "` is in the word!").queue();

                    BufferedImage hangmanImage = createHangmanImage(game);
                    try (var upload = createFileUpload(hangmanImage)) {
                        threadChannel.sendMessage("The word is: `" + game.getWord().chars()
                                        .mapToObj(character -> (char) character)
                                        .map(character -> game.getGuessedLetters().contains(character) ? character.toString() : "_")
                                        .reduce((first, second) -> first + " " + second).orElse("") + "` (" + game.getWord().length() + " letters)")
                                .setFiles(upload)
                                .queue(ignored -> createWaiter(game, threadChannel).build());
                    } catch (IOException exception) {
                        Constants.LOGGER.error("An error occurred while uploading a hangman image!", exception);
                        threadChannel.sendMessage("❌ An error occurred while uploading a hangman image!").queue(ignored -> {
                            threadChannel.getManager().setLocked(true).setArchived(true).queue();
                            GAMES.remove(game);
                        });
                    }
                });
    }

    private static BufferedImage createHangmanImage(Game game) {
        var image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 500, 500);

        graphics.setColor(Color.BLACK);
        graphics.setFont(TITLE_FONT);
        graphics.drawString("Hangman", 250 - (graphics.getFontMetrics().stringWidth("Hangman") / 2), 50);

        // map stage to from 0-GAME.MAX_LIVES to 0-STAGES.size() - 1
        int stage = MathUtils.map(Game.MAX_LIVES - game.getLives(), 0, Game.MAX_LIVES, 0, STAGES.size() - 1);
        for (int index = 0; index <= stage; index++) {
            STAGES.get(index).accept(graphics);
        }

        graphics.setColor(Color.BLACK);
        graphics.setStroke(THICK_STROKE);
        int wordLength = game.getWord().length();

        int startX = 250 - (wordLength * 25) + (wordLength % 2 == 0 ? 12 : 0);
        for (int index = 0; index < wordLength; index++) {
            graphics.drawLine(startX + (index * 50), 495, startX + (30 + index * 50), 495);
        }

        graphics.setColor(Color.BLACK);
        graphics.setFont(LETTER_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        for (int index = 0; index < game.word.toCharArray().length; index++) {
            char letter = game.word.toCharArray()[index];
            if (game.getGuessedLetters().contains(letter)) {
                String letterString = String.valueOf(letter);
                graphics.drawString(
                        letterString,
                        startX + (index * 50) + 15 - (metrics.stringWidth(letterString) / 2),
                        485);
            }
        }

        graphics.dispose();
        return image;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Game {
        private static final RandomWordRequestData RANDOM_WORD_REQUEST_DATA =
                new RandomWordRequestData.Builder()
                        .amount(1)
                        .minLength(4)
                        .maxLength(10)
                        .build();
        private static final int MAX_LIVES = 10;

        private final List<Character> guessedLetters = new ArrayList<>();
        private final long guildId, channelId, userId;
        private final String word;
        @Setter
        private long threadId;
        private int lives = MAX_LIVES;

        public boolean guess(String guess) {
            if (guess.length() != 1)
                return false;

            char letter = guess.charAt(0);
            if (guessedLetters.contains(letter))
                return false;

            guessedLetters.add(letter);
            if (!word.contains(guess)) {
                lives--;
                return false;
            }

            return true;
        }

        public boolean hasWon() {
            return word.chars().allMatch(character -> guessedLetters.contains((char) character));
        }

        public boolean hasLost() {
            return lives <= 0;
        }

        public static Either<Game, HttpStatus> create(long guildId, long channelId, long userId) {
            Either<List<String>, HttpStatus> response = ApiHandler.getWords(RANDOM_WORD_REQUEST_DATA);
            if (response.isRight())
                return Either.right(response.getRight());

            List<String> words = response.getLeft();
            if (words.isEmpty())
                return Either.right(HttpStatus.NOT_FOUND);

            return Either.left(new Game(guildId, channelId, userId, words.getFirst().toLowerCase(Locale.ROOT)));
        }
    }
}
