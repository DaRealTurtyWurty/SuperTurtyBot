package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Territory;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.Either;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class GuessCombinedFlagsCommand extends CoreCommand {
    private static final Map<Long, Game> GAMES = new HashMap<>();

    public GuessCombinedFlagsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Guess the territories that make up the combined flag!";
    }

    @Override
    public String getName() {
        return "guesscombinedflags";
    }

    @Override
    public String getRichName() {
        return "Guess The Combined Flags";
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.INTEGER, "number", "The number of flags to combine", false).setRequiredRange(2, 16));
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "This command can only be used in a guild!", false, true);
            return;
        }

        // check that the user does not already have a game running
        if (GAMES.values().stream().anyMatch(game -> game.getUserId() == event.getUser().getIdLong())) {
            reply(event, "❌ You already have a game running!", false, true);
            return;
        }

        event.deferReply().queue();

        int numberOfTerritories = event.getOption("number", ThreadLocalRandom.current().nextInt(2, 17), OptionMapping::getAsInt);
        final Map<String, BufferedImage> territories = new HashMap<>(numberOfTerritories);
        for (int index = 0; index < numberOfTerritories; index++) {
            Either<Pair<BufferedImage, Territory>, HttpStatus> result = ApiHandler.getFlag();
            if (result.isLeft()) {
                Pair<BufferedImage, Territory> pair = result.getLeft();
                if (territories.containsKey(pair.getRight().getName())) {
                    index--;
                    continue;
                }

                territories.put(pair.getRight().getName(), pair.getLeft());
            } else {
                reply(event, "❌ An error occurred while trying to get a flag!", false, true);
                return;
            }
        }

        // get the largest with and height using stream
        int width = territories.values().stream().mapToInt(BufferedImage::getWidth).max().orElse(0);
        int height = territories.values().stream().mapToInt(BufferedImage::getHeight).max().orElse(0);

        ByteArrayOutputStream boas = createImage(territories.values().stream().toList(), width, height);
        var upload = FileUpload.fromData(boas.toByteArray(), "combined_flags.png");

        String toSend = String.format("Guess the territories that make up the combined flag! (There are %d territories)",
                numberOfTerritories);
        event.getHook().editOriginal(toSend).setFiles(upload).queue(message -> {
            message.createThreadChannel(event.getUser().getName() + "'s game").queue(thread -> {
                var game = new Game(territories, event.getGuild().getIdLong(),
                        event.getChannel().getIdLong(), thread.getIdLong(), message.getIdLong(),
                        event.getUser().getIdLong());
                GAMES.put(message.getIdLong(), game);

                message.editMessageComponents(
                                ActionRow.of(Button.danger("combined-flags-" + message.getId(), Emoji.fromFormatted("❌"))))
                        .queue();

                thread.sendMessage("Game started! " + event.getUser().getAsMention()).queue();

                try {
                    boas.close();
                    upload.close();
                } catch (IOException exception) {
                    Constants.LOGGER.error(
                            "An error occurred while trying to close the ByteArrayOutputStream or FileUpload!",
                            exception);
                }
            });
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;
        if (event.getButton().getId() == null) return;
        if (!event.getButton().getId().startsWith("combined-flags-")) return;

        long messageId = Long.parseLong(event.getButton().getId().replace("combined-flags-", ""));

        Game game = GAMES.get(messageId);
        if (game == null) return;

        if (game.getUserId() != event.getUser().getIdLong()) {
            event.deferEdit().setComponents(event.getMessage().getComponents()).queue();
            return;
        }

        GAMES.remove(messageId, game);

        ThreadChannel thread = event.getGuild().getThreadChannelById(game.getChannelId());
        if (thread == null) return;

        String territoriesStr = String.join(", ", game.getTerritories().keySet());
        thread.sendMessage(String.format("Game cancelled! The territories were: %s", territoriesStr)).setComponents()
                .queue($ -> thread.getManager().setArchived(true).setLocked(true).queue());

        event.editComponents().queue();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        // check if the user has a game running
        Game game = GAMES.values().stream()
                .filter(g -> g.getUserId() == event.getAuthor().getIdLong() && g.getGuildId() == event.getGuild()
                        .getIdLong() && g.getChannelId() == event.getChannel().getIdLong()).findFirst().orElse(null);
        if (game == null) return;

        // check if the message is a valid territory
        String territory = event.getMessage().getContentRaw().trim();
        if (game.getTerritories().keySet().stream().noneMatch(t -> t.equalsIgnoreCase(territory))) return;

        // check if the territory has already been guessed
        if (game.getGuesses().stream().anyMatch(t -> t.equalsIgnoreCase(territory))) return;

        // add the territory to the game
        if (game.chooseTerritory(territory)) {
            event.getMessage().reply("✅ Correct guess!").queue();

            // check if the game has ended
            if (game.hasWon()) {
                // remove the game from the map
                GAMES.remove(game.getMessageId(), game);

                event.getChannel()
                        .sendMessage("✅ You win! The territories were: " + String.join(", ", game.getTerritories().keySet()))
                        .queue($ -> ((ThreadChannel) event.getChannel()).getManager().setArchived(true).setLocked(true).queue());

                // remove the button
                TextChannel channel = event.getJDA().getTextChannelById(game.getOwnerChannelId());
                if (channel == null) return;

                channel.retrieveMessageById(game.getMessageId()).queue(message -> message.editMessageComponents().queue());
            } else {
                List<BufferedImage> images = new ArrayList<>();
                for (String t : game.getTerritories().keySet()) {
                    if (game.getGuesses().stream().noneMatch(te -> te.equalsIgnoreCase(t))) {
                        images.add(game.getTerritories().get(t));
                    }
                }

                int width = images.stream().mapToInt(BufferedImage::getWidth).max().orElse(0);
                int height = images.stream().mapToInt(BufferedImage::getHeight).max().orElse(0);

                ByteArrayOutputStream baos = createImage(images, width, height);
                var upload = FileUpload.fromData(baos.toByteArray(), "combined_flags.png");

                String toSend = String.format(
                        "Guess the territories that make up the combined flag! (There are %d territories remaining)",
                        images.size());

                event.getChannel().sendMessage(toSend).setFiles(upload).queue($ -> {
                    try {
                        baos.close();
                        upload.close();
                    } catch (IOException exception) {
                        Constants.LOGGER.error(
                                "An error occurred while trying to close the ByteArrayOutputStream or FileUpload!",
                                exception);
                    }
                });
            }
        } else {
            event.getMessage().reply("❌ Was not a correct guess!").queue();

            if (game.getIncorrectGuesses() >= 9) {
                GAMES.remove(game.getMessageId());

                event.getChannel().sendMessage(
                                "The game has ended! The territories were: " + String.join(", ", game.getTerritories().keySet()))
                        .queue($ -> ((ThreadChannel) event.getChannel()).getManager().setArchived(true).setLocked(true)
                                .queue());

                // remove the button
                TextChannel channel = event.getJDA().getTextChannelById(game.getOwnerChannelId());
                if (channel == null) return;

                channel.retrieveMessageById(game.getMessageId()).queue(message -> message.editMessageComponents().queue());
                return;
            }

            if (game.getIncorrectGuesses() % 3 == 0) {
                event.getChannel().sendMessage(
                        "The territories that have been guessed are: " + String.join(", ", game.getGuesses())).queue();
            }
        }
    }

    private static ByteArrayOutputStream createImage(List<BufferedImage> images, int width, int height) {
        int numberOfTerritories = images.size();

        // create a new image with the largest width and height
        var combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // draw the images onto the combined image with transparency
        Graphics2D graphics = combined.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw the background
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, combined.getWidth(), combined.getHeight());

        Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f / numberOfTerritories);
        graphics.setComposite(composite);

        for (var image : images) {
            graphics.drawImage(image, 0, 0, null);
        }

        graphics.dispose();

        var boas = new ByteArrayOutputStream();
        try {
            ImageIO.write(combined, "png", boas);
            boas.flush();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "An error occurred while trying to write the combined image to a " + "ByteArrayOutputStream!",
                    exception);
        }

        return boas;
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    private static class Game {
        private final Map<String, BufferedImage> territories;
        private final long guildId, ownerChannelId, channelId, messageId, userId;
        private final List<String> guesses = new ArrayList<>();
        private int incorrectGuesses = 0;

        public Game(Map<String, BufferedImage> territories, long guildId, long ownerChannelId, long channelId, long messageId, long userId) {
            this.territories = territories;
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
        }

        public Map<String, BufferedImage> getTerritories() {
            return this.territories;
        }

        public long getGuildId() {
            return this.guildId;
        }

        public long getOwnerChannelId() {
            return this.ownerChannelId;
        }

        public long getChannelId() {
            return this.channelId;
        }

        public long getMessageId() {
            return this.messageId;
        }

        public long getUserId() {
            return this.userId;
        }

        public List<String> getGuesses() {
            return this.guesses;
        }

        public int getIncorrectGuesses() {
            return this.incorrectGuesses;
        }

        public boolean chooseTerritory(String territory) {
            if (this.guesses.contains(territory)) {
                return false;
            }

            this.guesses.add(territory);

            for (var c : this.territories.keySet()) {
                if (c.equalsIgnoreCase(territory)) {
                    return true;
                }
            }

            this.incorrectGuesses++;
            return false;
        }

        public boolean hasWon() {
            List<String> territories = this.territories.keySet().stream().map(String::toLowerCase).map(String::trim).toList();
            List<String> guesses = this.guesses.stream().map(String::toLowerCase).map(String::trim).toList();

            return new HashSet<>(guesses).containsAll(territories);
        }
    }
}
