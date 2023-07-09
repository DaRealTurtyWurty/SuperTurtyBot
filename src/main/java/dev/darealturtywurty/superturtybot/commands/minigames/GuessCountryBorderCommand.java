package dev.darealturtywurty.superturtybot.commands.minigames;

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
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GuessCountryBorderCommand extends CoreCommand {
    private static final Map<String, BufferedImage> BORDERS = new HashMap<>();
    private static final Map<Long, Game> GAMES = new HashMap<>();

    public GuessCountryBorderCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Guess the country that has the border shown.";
    }

    @Override
    public String getName() {
        return "guesscountryborder";
    }

    @Override
    public String getRichName() {
        return "Guess The Country's Border";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "This command can only be used in a server!", false, true);
            return;
        }

        if (GAMES.values().stream().anyMatch(
                game -> game.guildId == event.getGuild().getIdLong() && game.userId == event.getUser().getIdLong())) {
            reply(event, "You already have a game running in this server!", false, true);
            return;
        }

        event.deferReply().queue();

        final var country = BORDERS.entrySet().stream().skip((int) (Math.random() * BORDERS.size())).findFirst()
                .orElseThrow();

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(country.getValue(), "png", baos);
        } catch (IOException exception) {
            reply(event, "An error occurred while trying to send the image!", false, true);
            return;
        }

        var upload = FileUpload.fromData(baos.toByteArray(), "border.png");
        event.getHook().editOriginal("Guess the country that has the border shown!").setFiles(upload).queue(message -> {
            message.createThreadChannel(event.getUser().getName() + "'s game").queue(thread -> {
                final var game = new Game(country.getKey(), event.getGuild().getIdLong(),
                        event.getChannel().getIdLong(), thread.getIdLong(), message.getIdLong(),
                        event.getUser().getIdLong());
                GAMES.put(message.getIdLong(), game);

                message.editMessageComponents(
                                ActionRow.of(Button.danger("country-border-" + message.getId(), Emoji.fromFormatted("❌"))))
                        .queue();

                thread.sendMessage("Game started! " + event.getUser().getAsMention()).queue();

                try {
                    baos.close();
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

        if (!event.getButton().getId().startsWith("country-border-")) return;

        long messageId = Long.parseLong(event.getButton().getId().replace("country-border-", ""));

        Game game = GAMES.get(messageId);
        if (game == null) return;

        if (game.getUserId() != event.getUser().getIdLong()) {
            event.deferEdit().setComponents(event.getMessage().getComponents()).queue();
            return;
        }

        GAMES.remove(messageId, game);

        ThreadChannel thread = event.getGuild().getThreadChannelById(game.getChannelId());
        if (thread == null) return;

        thread.sendMessage(String.format("Game cancelled! The country was: %s", game.getCountry())).setComponents()
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

        // check if the message is a valid country
        String country = event.getMessage().getContentRaw().trim();
        if (BORDERS.keySet().stream().noneMatch(c -> c.equalsIgnoreCase(country))) return;

        // check if the country is correct
        if (game.guess(country)) {
            var thread = (ThreadChannel) event.getChannel();
            thread.sendMessage(String.format("Correct! The country was: %s", game.getCountry()))
                    .queue($ -> thread.getManager().setArchived(true).setLocked(true).queue());

            GAMES.remove(game.getMessageId(), game);

            // remove components on original message
            TextChannel channel = event.getJDA().getTextChannelById(game.getOwnerChannelId());
            if (channel == null) return;

            channel.retrieveMessageById(game.getMessageId()).queue(message -> message.editMessageComponents().queue());

            return;
        } else {
            event.getChannel()
                    .sendMessage(String.format("Incorrect! You have %d guesses left.", 9 - game.getGuesses().size()))
                    .queue();
        }

        if (game.getGuesses().size() >= 9) {
            var thread = (ThreadChannel) event.getChannel();
            thread.sendMessage(String.format("Game over! The country was: %s", game.getCountry())).setComponents()
                    .queue($ -> thread.getManager().setArchived(true).setLocked(true).queue());

            GAMES.remove(game.getMessageId(), game);

            // remove components on original message
            event.getChannel().retrieveMessageById(game.getMessageId())
                    .queue(message -> message.editMessageComponents().queue(ignored -> {}, ignored -> {}), ignored -> {});
        }
    }

    public static class Game {
        private final String country;
        private final long guildId, ownerChannelId, channelId, messageId, userId;
        private final List<String> guesses = new ArrayList<>();

        public Game(String country, long guildId, long ownerChannelId, long channelId, long messageId, long userId) {
            this.country = country;
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
        }

        public String getCountry() {
            return this.country;
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

        public boolean guess(String guess) {
            if (this.guesses.contains(guess)) return false;

            this.guesses.add(guess);

            guess = guess.trim();
            return guess.equalsIgnoreCase(this.country) || guess.equalsIgnoreCase(this.country.replace(" ", ""));
        }
    }

    public static class BorderFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toString().endsWith(".png")) {
                final BufferedImage image = ImageIO.read(file.toFile());
                final String name = file.getFileName().toString().replace(".png", "").replace("_", " ");
                BORDERS.put(name, image);
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
