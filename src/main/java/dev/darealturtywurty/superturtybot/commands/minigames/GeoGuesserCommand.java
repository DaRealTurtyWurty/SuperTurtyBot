package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Geoguesser;
import dev.darealturtywurty.superturtybot.core.api.pojo.Region;
import dev.darealturtywurty.superturtybot.core.api.request.RegionExcludeRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GeoGuesserCommand extends SubcommandCommand {
    private static final List<Game> GAMES = new ArrayList<>();

    public GeoGuesserCommand() {
        super("geoguesser", "Play a game of GeoGuesser!");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        // Check if the user is already playing a game
        for (Game game : GAMES) {
            if (game.getUserId() == event.getUser().getIdLong()) {
                reply(event, "❌ You are already playing a game!", false, true);
                return;
            }
        }

        event.deferReply().queue();

        Either<Geoguesser, HttpStatus> response = ApiHandler.geoguesser();
        if (response.isRight()) {
            event.getHook().editOriginal(
                    "❌ There was an error while trying to get a GeoGuesser game! ||Response Code: " +
                            response.getRight() + "||").queue();
            return;
        }

        Geoguesser geoguesser = response.getLeft();
        BufferedImage image = geoguesser.image();

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Error while writing image to byte array!", exception);
            event.getHook().editOriginal("❌ There was an error while trying to get a GeoGuesser game!").queue();
            return;
        }

        byte[] bytes = baos.toByteArray();
        FileUpload upload = FileUpload.fromData(bytes, "geoguesser.png");
        event.getHook().editOriginal("🌎 **Where is this?**").setFiles(upload).queue(message -> {
            message.createThreadChannel(event.getUser().getName() + "'s Geo Guesser Game").queue(thread -> {
                thread.addThreadMember(event.getUser()).queue();
                var game = new Game(
                        guild.getIdLong(),
                        event.getChannel().getIdLong(),
                        thread.getIdLong(),
                        message.getIdLong(),
                        event.getUser().getIdLong(),
                        geoguesser);

                GAMES.add(game);
                createEventWaiter(game, thread).build();
            });
        });
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createEventWaiter(Game game, ThreadChannel thread) {
        return TurtyBot.EVENT_WAITER
                .builder(MessageReceivedEvent.class)
                .condition(msgEvent -> msgEvent.isFromGuild() &&
                        msgEvent.getGuild().getIdLong() == game.getGuildId() &&
                        msgEvent.getChannel().getIdLong() == game.getChannelId() &&
                        msgEvent.getAuthor().getIdLong() == game.getUserId() &&
                        game.isValidGuess(msgEvent.getMessage().getContentRaw()) &&
                        !game.hasGuessed(msgEvent.getMessage().getContentRaw()))
                .timeout(10, TimeUnit.MINUTES)
                .timeoutAction(() -> {
                    GAMES.remove(game);
                    thread.sendMessage("❌ **Game timed out!**").queue(ignored ->
                            thread.getManager().setArchived(true).setLocked(true).queue());
                })
                .failure(() -> {
                    GAMES.remove(game);
                    thread.sendMessage("❌ **Game failed!**").queue(ignored ->
                            thread.getManager().setArchived(true).setLocked(true).queue());
                })
                .success(msgEvent -> {
                    String guess = msgEvent.getMessage().getContentRaw();
                    if (game.guess(guess)) {
                        GAMES.remove(game);
                        thread.sendMessage("✅ **Correct!**").queue(ignored ->
                                thread.getManager().setArchived(true).setLocked(true).queue());
                    } else {
                        thread.sendMessage("❌ **Incorrect!**").queue();

                        if (game.hasLost()) {
                            GAMES.remove(game);
                            thread.sendMessage("❌ **Game over! The correct answer was: " + game.getGeoguesser().country() + "**").queue(ignored ->
                                    thread.getManager().setArchived(true).setLocked(true).queue());
                        } else {
                            thread.sendMessage("❌ **You have " + (10 - game.getGuesses().size()) + " guesses left!**").queue();

                            // we need to listen for another message so we need to create a new event waiter
                            createEventWaiter(game, thread).build();
                        }
                    }
                });
    }

    @Getter
    public static class Game {
        private static final RegionExcludeRequestData EXCLUDE_REQUEST_DATA =
                new RegionExcludeRequestData.Builder().excludeTerritories().build();

        private final long guildId, parentChannelId, channelId, messageId, userId;
        private final Geoguesser geoguesser;
        private final List<String> guesses = new ArrayList<>();
        private final List<String> possibleCountries = new ArrayList<>();

        public Game(long guildId, long parentChannelId, long channelId, long messageId, long userId, Geoguesser geoguesser) {
            this.guildId = guildId;
            this.parentChannelId = parentChannelId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
            this.geoguesser = geoguesser;

            Either<List<Region>, HttpStatus> matchingRegions = ApiHandler.getAllRegions(EXCLUDE_REQUEST_DATA);
            if (matchingRegions.isRight()) {
                Constants.LOGGER.error("Error while getting all regions! Response Code: " + matchingRegions.getRight());
                return;
            }

            List<Region> regions = matchingRegions.getLeft();
            for (Region region : regions) {
                if (!region.getName().equalsIgnoreCase(geoguesser.country())) {
                    this.possibleCountries.add(region.getName());
                }
            }
        }

        public boolean isCorrect(String guess) {
            return this.geoguesser.country().equalsIgnoreCase(guess.trim());
        }

        public boolean isValidGuess(String guess) {
            for (String possibleCountry : this.possibleCountries) {
                if (possibleCountry.equalsIgnoreCase(guess.trim())) {
                    return true;
                }
            }

            return isCorrect(guess);
        }

        public boolean hasGuessed(String guess) {
            for (String guessed : this.guesses) {
                if (guessed.equalsIgnoreCase(guess.trim())) {
                    return true;
                }
            }

            return false;
        }

        public boolean guess(String guess) {
            if (this.guesses.contains(guess))
                return false;

            this.guesses.add(guess);
            return isCorrect(guess);
        }

        public boolean hasLost() {
            return this.guesses.size() >= 10;
        }
    }
}
