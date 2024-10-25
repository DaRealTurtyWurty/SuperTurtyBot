package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Region;
import dev.darealturtywurty.superturtybot.core.api.request.RegionExcludeRequestData;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuessTheFlagCommand extends SubcommandCommand {
    private static final Map<Long, Game> GAMES = new HashMap<>();

    public GuessTheFlagCommand(CoreCommand parent) {
        super(parent, "flag", "Guess the flag of a region!");
        addOption(OptionType.BOOLEAN, "include-territories", "Whether to include territories in the flag guessing.", false);
        addOption(OptionType.BOOLEAN, "exclude-countries", "Whether to exclude countries in the flag guessing.", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ This command can only be used in a guild!", false, true);
            return;
        }

        if (event.getChannel() instanceof ThreadChannel) {
            reply(event, "❌ This command cannot be used in a thread!", false, true);
            return;
        }

        // check that the user does not already have a game running
        if (GAMES.values().stream().anyMatch(game -> game.getUserId() == event.getUser().getIdLong())) {
            reply(event, "❌ You already have a game running!", false, true);
            return;
        }

        event.deferReply().queue();

        boolean includeTerritories = event.getOption("include-territories", false, OptionMapping::getAsBoolean);
        boolean excludeCountries = event.getOption("exclude-countries", false, OptionMapping::getAsBoolean);
        if (!includeTerritories && excludeCountries) {
            event.getHook().sendMessage("❌ You cannot both include territories and exclude countries!").queue();
            return;
        }

        RegionExcludeRequestData.Builder builder = new RegionExcludeRequestData.Builder();
        if (excludeCountries) {
            builder.excludeCountries();
        }

        if (!includeTerritories) {
            builder.excludeTerritories();
        }

        RegionExcludeRequestData data = builder.build();

        Either<Pair<BufferedImage, Region>, HttpStatus> result = ApiHandler.getFlag(data);
        if (result.isRight()) {
            event.getHook().sendMessage("❌ An error occurred while fetching the flag!").queue();
            return;
        }

        Pair<BufferedImage, Region> pair = result.getLeft();
        var outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(pair.getLeft(), "png", outputStream);
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while writing the flag!").queue();
            Constants.LOGGER.error("An error occurred while writing the flag!", exception);
            return;
        }

        try (var upload = FileUpload.fromData(outputStream.toByteArray(), "flag.png")) {
            event.getHook().editOriginal("🚩 Guess the flag of this region!")
                    .setFiles(upload)
                    .queue(message -> message.createThreadChannel(event.getUser().getName() + "'s game").queue(thread -> {
                        Either<List<Region>, HttpStatus> allRegions = ApiHandler.getAllRegions(data);
                        if (allRegions.isRight()) {
                            Constants.LOGGER.error("An error occurred while trying to get all regions! Status code: {}",
                                    allRegions.getRight().getCode());
                            event.getHook().sendMessage("❌ An error occurred while trying to get all regions!").queue(ignored -> thread.delete().queue());
                            return;
                        }

                        var game = new Game(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                                thread.getIdLong(), message.getIdLong(), event.getUser().getIdLong(), allRegions.getLeft(), pair);
                        GAMES.put(message.getIdLong(), game);

                        message.editMessageComponents(
                                        ActionRow.of(Button.danger("guess-flag-" + message.getId(), Emoji.fromFormatted("❌"))))
                                .queue();

                        thread.sendMessage("✅ The game has started " + event.getUser().getAsMention() + "!").queue();

                        try {
                            outputStream.close();
                        } catch (IOException exception) {
                            Constants.LOGGER.error("An error occurred while closing the output stream!", exception);
                        }
                    }));
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while uploading the flag!").queue();
            Constants.LOGGER.error("An error occurred while uploading the flag!", exception);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;
        if (event.getButton().getId() == null) return;
        if (!event.getButton().getId().startsWith("guess-flag-")) return;

        long messageId = Long.parseLong(event.getButton().getId().replace("guess-flag-", ""));

        Game game = GAMES.get(messageId);
        if (game == null) return;

        if (game.getUserId() != event.getUser().getIdLong()) {
            event.deferEdit().setComponents(event.getMessage().getComponents()).queue();
            return;
        }

        GAMES.remove(messageId, game);

        ThreadChannel thread = event.getGuild().getThreadChannelById(game.getChannelId());
        if (thread == null) return;

        thread.sendMessageFormat("Game cancelled! The region was %s!", game.flag.getRight().getName())
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

        // check if the message is a valid region
        String region = event.getMessage().getContentRaw().trim();

        // check if the region is valid
        if (game.getAllRegions().stream().noneMatch(r -> r.getAliases().stream().anyMatch(region::equalsIgnoreCase) || r.getName().equalsIgnoreCase(region)))
            return;

        // check if the region has already been guessed
        if (game.getGuesses().stream().anyMatch(region::equalsIgnoreCase))
            return;

        // add the region to the game
        if (game.guess(region)) {
            FileUpload flag = null;
            try (var outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(game.getFlag().getLeft(), "png", outputStream);
                flag = FileUpload.fromData(outputStream.toByteArray(), "flag.png");
            } catch (IOException exception) {
                Constants.LOGGER.error("An error occurred while writing the flag!", exception);
            }

            if (flag == null) {
                event.getChannel().sendMessage("❌ An error occurred while writing the flag!").queue();
                return;
            }

            FileUpload finalFlag = flag;
            event.getChannel().sendMessageFormat("✅ Correct guess! The region was %s!", game.getFlag().getRight().getName())
                    .setFiles(flag)
                    .queue($ -> {
                        if (game.isOver()) {
                            event.getChannel().sendMessage("❌ You have run out of guesses!").queue();
                            return;
                        }

                        event.getChannel().sendMessage("🚩 Guess the flag of this region!")
                                .setFiles(finalFlag)
                                .queue(message -> message.editMessageComponents(
                                                ActionRow.of(Button.danger("guess-flag-" + message.getId(), Emoji.fromFormatted("❌"))))
                                        .queue());
                    });
        } else {
            event.getChannel().sendMessage("❌ Incorrect guess!").queue();
        }
    }

    @Getter
    private static class Game {
        private final List<Region> allRegions;
        private final long guildId, ownerChannelId, channelId, messageId, userId;
        private final int maxGuesses = 3;
        private final Pair<BufferedImage, Region> flag;
        private final List<String> guesses = new ArrayList<>();
        private int incorrectGuesses = 0;

        public Game(long guildId, long ownerChannelId, long channelId, long messageId, long userId, List<Region> allRegions, Pair<BufferedImage, Region> flag) {
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
            this.allRegions = allRegions;
            this.flag = flag;
        }

        public boolean guess(@NotNull String guess) {
            if (this.guesses.stream().anyMatch(guess::equalsIgnoreCase)) {
                return false;
            }

            for (Region region : this.allRegions) {
                if (region.getName().equalsIgnoreCase(guess)) {
                    this.guesses.add(region.getName());
                    return true;
                }

                if (region.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(guess))) {
                    this.guesses.add(region.getName());
                    return true;
                }
            }

            this.incorrectGuesses++;
            return false;
        }

        public boolean isOver() {
            return this.incorrectGuesses >= this.maxGuesses;
        }
    }
}
