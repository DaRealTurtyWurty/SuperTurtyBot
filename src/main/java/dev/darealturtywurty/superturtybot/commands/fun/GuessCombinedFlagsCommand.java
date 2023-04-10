package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GuessCombinedFlagsCommand extends CoreCommand {
    private static final Map<String, BufferedImage> FLAGS = new HashMap<>();
    private static final Map<Long, Game> GAMES = new HashMap<>();

    static {
        try {
            Files.walkFileTree(Path.of("src/main/resources/flags/"), new FlagFileVisitor());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load flags!", exception);
        }
    }

    public GuessCombinedFlagsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Guess the countries that make up the combined flag!";
    }

    @Override
    public String getName() {
        return "guesscombinedflags";
    }

    @Override
    public String getRichName() {
        return "Guess Combined Flags";
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

        int numberOfCountries = ThreadLocalRandom.current().nextInt(2, 6);
        final List<Map.Entry<String, BufferedImage>> countries = new ArrayList<>(numberOfCountries);
        for (int $ = 0; $ < numberOfCountries; $++) {
            countries.add(FLAGS.entrySet().stream().filter(entry -> !countries.contains(entry))
                    .skip(ThreadLocalRandom.current().nextInt(0, FLAGS.size())).findFirst().orElseThrow());
        }

        // get the largest with and height using stream
        int width = countries.stream().mapToInt(entry -> entry.getValue().getWidth()).max().orElse(0);
        int height = countries.stream().mapToInt(entry -> entry.getValue().getHeight()).max().orElse(0);

        ByteArrayOutputStream boas = createImage(countries.stream().map(Map.Entry::getValue).toList(), width, height);
        var upload = FileUpload.fromData(boas.toByteArray(), "combined_flags.png");

        String toSend = String.format("Guess the countries that make up the combined flag! (There are %d countries)",
                numberOfCountries);
        event.getHook().editOriginal(toSend).setFiles(upload).queue(message -> {
            message.createThreadChannel(event.getUser().getName() + "'s game").queue(thread -> {
                var game = new Game(countries.stream().map(Map.Entry::getKey).toList(), event.getGuild().getIdLong(),
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

        String countriesStr = String.join(", ", game.getCountries());
        thread.sendMessage(String.format("Game cancelled! The countries were: %s", countriesStr)).setComponents()
                .queue($ -> thread.getManager().setArchived(true).setLocked(true).queue());

        event.editComponents().queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        // check if the user has a game running
        Game game = GAMES.values().stream()
                .filter(g -> g.getUserId() == event.getAuthor().getIdLong() && g.getGuildId() == event.getGuild()
                        .getIdLong() && g.getChannelId() == event.getChannel().getIdLong()).findFirst().orElse(null);
        if (game == null) return;

        // check if the message is a valid country
        String country = event.getMessage().getContentRaw().trim();
        if (FLAGS.keySet().stream().noneMatch(c -> c.equalsIgnoreCase(country))) return;

        // check if the country has already been guessed
        if (game.getGuesses().stream().anyMatch(c -> c.equalsIgnoreCase(country))) return;

        // add the country to the game
        if (game.chooseCountry(country)) {
            event.getMessage().reply("✅ Correct guess!").queue();

            // check if the game has ended
            if (game.hasWon()) {
                // remove the game from the map
                GAMES.remove(game.getMessageId(), game);

                event.getChannel()
                        .sendMessage("✅ You win! The countries were: " + String.join(", ", game.getCountries()))
                        .queue($ -> ((ThreadChannel) event.getChannel()).getManager().setArchived(true).setLocked(true).queue());

                // remove the button
                TextChannel channel = event.getJDA().getTextChannelById(game.getOwnerChannelId());
                if (channel == null) return;

                channel.retrieveMessageById(game.getMessageId()).queue(message -> message.editMessageComponents().queue());
            } else {
                List<BufferedImage> images = new ArrayList<>();
                for (String c : game.getCountries()) {
                    if (game.getGuesses().stream().noneMatch(co -> co.equalsIgnoreCase(c))) {
                        images.add(FLAGS.get(c));
                    }
                }

                int width = images.stream().mapToInt(BufferedImage::getWidth).max().orElse(0);
                int height = images.stream().mapToInt(BufferedImage::getHeight).max().orElse(0);

                ByteArrayOutputStream baos = createImage(images, width, height);
                var upload = FileUpload.fromData(baos.toByteArray(), "combined_flags.png");

                String toSend = String.format(
                        "Guess the countries that make up the combined flag! (There are %d countries remaining)",
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

            if (game.getGuesses().size() == 9) {
                GAMES.remove(game.getMessageId());

                event.getChannel().sendMessage(
                                "The game has ended! The countries were: " + String.join(", ", game.getCountries()))
                        .queue($ -> ((ThreadChannel) event.getChannel()).getManager().setArchived(true).setLocked(true)
                                .queue());

                // remove the button
                TextChannel channel = event.getJDA().getTextChannelById(game.getOwnerChannelId());
                if (channel == null) return;

                channel.retrieveMessageById(game.getMessageId()).queue(message -> message.editMessageComponents().queue());
                return;
            }

            if (game.getGuesses().size() % 3 == 0) {
                event.getChannel().sendMessage(
                        "The countries that have been guessed are: " + String.join(", ", game.getGuesses())).queue();
            }
        }
    }

    private static ByteArrayOutputStream createImage(List<BufferedImage> images, int width, int height) {
        int numberOfCountries = images.size();

        // create a new image with the largest width and height
        var combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // draw the images onto the combined image with transparency
        Graphics2D graphics = combined.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw the background
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, combined.getWidth(), combined.getHeight());

        Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f / numberOfCountries);
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

    private static class Game {
        private final List<String> countries;
        private final long guildId, ownerChannelId, channelId, messageId, userId;
        private final List<String> guesses = new ArrayList<>();

        public Game(List<String> countries, long guildId, long ownerChannelId, long channelId, long messageId, long userId) {
            this.countries = countries;
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
        }

        public List<String> getCountries() {
            return this.countries;
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

        public boolean chooseCountry(String country) {
            if (this.guesses.contains(country)) {
                return false;
            }

            this.guesses.add(country);

            for (var c : this.countries) {
                if (c.equalsIgnoreCase(country)) {
                    return true;
                }
            }

            return false;
        }

        public boolean hasWon() {
            List<String> countries = this.countries.stream().map(String::toLowerCase).map(String::trim).toList();
            List<String> guesses = this.guesses.stream().map(String::toLowerCase).map(String::trim).toList();

            return new HashSet<>(guesses).containsAll(countries);
        }
    }

    private static class FlagFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(".png")) {
                final BufferedImage image = ImageIO.read(file.toFile());
                final String name = file.getFileName().toString().replace(".png", "").replace("_", " ");
                FLAGS.put(name, image);
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
