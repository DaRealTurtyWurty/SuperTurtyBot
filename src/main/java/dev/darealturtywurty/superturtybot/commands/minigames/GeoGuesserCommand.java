package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Geoguesser;
import dev.darealturtywurty.superturtybot.core.api.pojo.Region;
import dev.darealturtywurty.superturtybot.core.api.request.RegionExcludeRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.Either;
import dev.darealturtywurty.superturtybot.core.util.EventWaiter;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GeoGuesserCommand extends CoreCommand {
    private static final Map<Long, List<Game>> GAMES = new HashMap<>();

    public GeoGuesserCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Starts a game of GeoGuesser (a game where you have to guess what country you are in, in the world)";
    }

    @Override
    public String getName() {
        return "geoguesser";
    }

    @Override
    public String getRichName() {
        return "GeoGuesser";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (!event.isFromGuild() || guild == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        User user = event.getUser();
        if (hasGameRunning(guild.getIdLong(), user.getIdLong())) {
            reply(event, "❌ You already have a game of GeoGuesser running!", false, true);
            RATELIMITS.put(user.getIdLong(), Pair.of(getName(), 0L));
            return;
        }

        event.deferReply().queue();

        if (guild.getMember(user) == null) {
            guild.loadMembers().onSuccess(members -> {
                if (members.stream().anyMatch(member -> member.getIdLong() == user.getIdLong())) {
                    RATELIMITS.put(user.getIdLong(), Pair.of(getName(), 0L));
                    event.getHook()
                            .sendMessage("❌ Something went wrong loading your member data, please try again!")
                            .mentionRepliedUser(false)
                            .queue();
                } else {
                    event.getHook()
                            .sendMessage("❌ This command can only be used in a server!")
                            .mentionRepliedUser(false)
                            .queue();
                }
            });

            return;
        }

        Either<Geoguesser, HttpStatus> response = ApiHandler.geoguesser();
        if (response.isRight()) {
            event.getHook()
                    .sendMessage("❌ There was an error while trying to start a game of GeoGuesser!")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Failed to get GeoGuesser! Status: {}", response.getRight());
            RATELIMITS.put(user.getIdLong(), Pair.of(getName(), 0L));
            return;
        }

        Geoguesser geoguesser = response.getLeft();
        BufferedImage image = geoguesser.image();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            event.getHook()
                    .sendMessage("❌ There was an error while trying to start a game of GeoGuesser!")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Failed to write GeoGuesser image!", exception);
            RATELIMITS.put(user.getIdLong(), Pair.of(getName(), 0L));
            return;
        }

        byte[] bytes = baos.toByteArray();
        try (FileUpload upload = FileUpload.fromData(bytes, "geogueeser.png")) {
            event.getHook()
                    .sendMessage("🌎 You have started a game of GeoGuesser! Guess what country you are in! (You have 10 minutes)")
                    .addFiles(upload)
                    .mentionRepliedUser(false)
                    .queue(message -> message.createThreadChannel(user.getName() + "'s GeoGuesser Game")
                            .queue(thread -> {
                                List<Game> games = GAMES.getOrDefault(guild.getIdLong(), new ArrayList<>());
                                var game = new Game(
                                        guild.getIdLong(),
                                        event.getChannel().getIdLong(),
                                        thread.getIdLong(),
                                        user.getIdLong(),
                                        geoguesser);

                                games.add(game);
                                GAMES.put(guild.getIdLong(), games);

                                constructWaiter(user, game).build();
                            }));
        } catch (IOException exception) {
            event.getHook()
                    .sendMessage("❌ There was an error while trying to start a game of GeoGuesser!")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Failed to upload GeoGuesser image!", exception);
            RATELIMITS.put(user.getIdLong(), Pair.of(getName(), 0L));
        }
    }

    private static EventWaiter.Builder<MessageReceivedEvent> constructWaiter(User user, Game game) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(msgEvent ->
                        msgEvent.getChannel().getIdLong() == game.getThreadChannelId()
                                && msgEvent.getAuthor().getIdLong() == user.getIdLong())
                .timeout(10, TimeUnit.MINUTES)
                .success(msgEvent -> {
                    if (onGameResponse(msgEvent, game)) {
                        constructWaiter(user, game).build();
                    }
                });
    }

    private static boolean onGameResponse(MessageReceivedEvent event, Game game) {
        ThreadChannel thread = event.getChannel().asThreadChannel();

        String content = event.getMessage().getContentRaw();
        Game.GuessResult result = game.guess(content);

        switch (result) {
            case CORRECT -> event.getMessage()
                    .reply("🎉 You guessed correctly! You were in **" + game.getGeoguesser().country() + "**!")
                    .mentionRepliedUser(false)
                    .queue(message -> endGame(thread, game));
            case INCORRECT -> {
                event.getMessage()
                        .reply("❌ You guessed incorrectly! You have **" + (5 - game.getGuesses()) + "** guesses left!")
                        .mentionRepliedUser(false)
                        .queue();

                if (game.getGuesses() >= 5) {
                    event.getMessage()
                            .reply("❌ You have run out of guesses! You were in **" + game.getGeoguesser().country() + "**!")
                            .mentionRepliedUser(false)
                            .queue(message -> endGame(thread, game));
                    return false;
                }
            }
            case INVALID -> {
                if (content.equalsIgnoreCase("end")) {
                    event.getMessage()
                            .reply("❌ You have ended your game of GeoGuesser! You were in **" + game.getGeoguesser().country() + "**!")
                            .mentionRepliedUser(false)
                            .queue(message -> endGame(thread, game));
                    return false;
                }
            }
        }

        return true;
    }

    private static void endGame(ThreadChannel thread, Game game) {
        thread.getManager().setArchived(true).setLocked(true).queue();

        List<Game> games = GAMES.getOrDefault(game.getGuildId(), new ArrayList<>());
        games.remove(game);
        GAMES.put(game.getGuildId(), games);
    }

    private static boolean hasGameRunning(long guild, long user) {
        return GAMES.containsKey(guild) && GAMES.get(guild).stream().anyMatch(game -> game.getUserId() == user);
    }

    @Getter
    public static class Game {
        private static final List<String> COUNTRIES = new ArrayList<>();

        static {
            Either<List<Region>, HttpStatus> response = ApiHandler.getAllRegions(new RegionExcludeRequestData.Builder().excludeTerritories().build());
            if (response.isRight()) {
                Constants.LOGGER.error("Failed to load regions for GeoGuesser!");
            } else {
                response.getLeft().stream().map(Region::getName).forEach(COUNTRIES::add);
            }
        }

        private final long guildId;
        private final long parentChannelId;
        private final long threadChannelId;
        private final long userId;

        private final Geoguesser geoguesser;

        private int guesses = 0;

        public Game(long guildId, long parentChannelId, long threadChannelId, long userId, Geoguesser geoguesser) {
            this.guildId = guildId;
            this.parentChannelId = parentChannelId;
            this.threadChannelId = threadChannelId;
            this.userId = userId;

            this.geoguesser = geoguesser;
        }

        public GuessResult guess(String country) {
            if (isCountry(country)) {
                guesses++;

                if (country.equalsIgnoreCase(geoguesser.country()))
                    return GuessResult.CORRECT;

                return GuessResult.INCORRECT;
            }

            return GuessResult.INVALID;
        }

        private static boolean isCountry(String str) {
            for (String country : Game.COUNTRIES) {
                if (country.equalsIgnoreCase(str))
                    return true;
            }

            return false;
        }

        public enum GuessResult {
            CORRECT,
            INCORRECT,
            INVALID
        }
    }
}
