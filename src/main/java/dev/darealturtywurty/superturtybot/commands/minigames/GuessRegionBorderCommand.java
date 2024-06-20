package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Region;
import dev.darealturtywurty.superturtybot.core.api.request.RegionExcludeRequestData;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import lombok.Getter;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuessRegionBorderCommand extends SubcommandCommand {
    private static final Map<Long, Game> GAMES = new HashMap<>();

    public GuessRegionBorderCommand() {
        super("border", "Guess the region that has the border shown.");

        addOptions(List.of(
                new OptionData(OptionType.BOOLEAN, "exclude-islands", "Whether or not to exclude islands from the game.", false),
                new OptionData(OptionType.BOOLEAN, "exclude-mainland", "Whether or not to exclude mainland regions from the game.", false),
                new OptionData(OptionType.BOOLEAN, "exclude-countries", "Whether or not to exclude countries from the game.", false),
                new OptionData(OptionType.BOOLEAN, "include-territories", "Whether or not to include territories from the game.", false)
        ));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        if (event.getChannel() instanceof ThreadChannel) {
            reply(event, "❌ This command cannot be used in a thread!", false, true);
            return;
        }

        if (GAMES.values().stream().anyMatch(
                game -> game.guildId == event.getGuild().getIdLong() && game.userId == event.getUser().getIdLong())) {
            reply(event, "❌ You already have a game running in this server!", false, true);
            return;
        }

        boolean excludeIslands = event.getOption("exclude-islands", false, OptionMapping::getAsBoolean);
        boolean excludeMainland = event.getOption("exclude-mainland", false, OptionMapping::getAsBoolean);
        boolean excludeCountries = event.getOption("exclude-countries", false, OptionMapping::getAsBoolean);
        boolean includeTerritories = event.getOption("include-territories", false, OptionMapping::getAsBoolean);

        if (excludeIslands && excludeMainland && excludeCountries && !includeTerritories) {
            reply(event, "❌ You cannot exclude all types of regions!", false, true);
            return;
        }

        if (excludeIslands && excludeMainland) {
            reply(event, "❌ You cannot exclude both islands and mainland regions!", false, true);
            return;
        }

        if (excludeCountries && !includeTerritories) {
            reply(event, "❌ You cannot exclude countries and not include territories!", false, true);
            return;
        }

        event.deferReply().queue();

        RegionExcludeRequestData.Builder builder = new RegionExcludeRequestData.Builder();
        if (excludeIslands) {
            builder.excludeIslands();
        } else if (excludeMainland) {
            builder.excludeMainland();
        }

        if (excludeCountries) {
            builder.excludeCountries();
        }

        if (!includeTerritories) {
            builder.excludeTerritories();
        }

        Either<Pair<BufferedImage, Region>, HttpStatus> result = ApiHandler.getOutline(builder.build());
        if (result.isRight()) {
            reply(event, "❌ An error occurred while trying to get the image!", false, true);
            return;
        }

        Pair<BufferedImage, Region> region = result.getLeft();

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(region.getKey(), "png", baos);
        } catch (IOException exception) {
            reply(event, "❌ An error occurred while trying to send the image!", false, true);
            return;
        }

        var upload = FileUpload.fromData(baos.toByteArray(), "border.png");
        event.getHook().editOriginal("Guess the region that has the border shown!").setFiles(upload).queue(message ->
                message.createThreadChannel(event.getUser().getName() + "'s game").queue(thread -> {
                    Either<List<Region>, HttpStatus> matchingRegions = ApiHandler.getAllRegions(builder.build());
                    if (matchingRegions.isRight()) {
                        Constants.LOGGER.error("An error occurred while trying to get all regions! Status code: {}",
                                matchingRegions.getRight().getCode());
                        event.getHook().sendMessage("❌ An error occurred while trying to get all regions!").queue(ignored -> thread.delete().queue());
                        return;
                    }

                    final var game = new Game(region.getValue(), event.getGuild().getIdLong(),
                            event.getChannel().getIdLong(), thread.getIdLong(), message.getIdLong(),
                            event.getUser().getIdLong(), matchingRegions.getLeft());
                    GAMES.put(message.getIdLong(), game);

                    message.editMessageComponents(
                                    ActionRow.of(Button.danger("region-border-" + message.getId(), Emoji.fromFormatted("❌"))))
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
                }));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;

        if (event.getButton().getId() == null) return;

        if (!event.getButton().getId().startsWith("region-border-")) return;

        long messageId = Long.parseLong(event.getButton().getId().replace("region-border-", ""));

        Game game = GAMES.get(messageId);
        if (game == null) return;

        if (game.getUserId() != event.getUser().getIdLong()) {
            event.deferEdit().setComponents(event.getMessage().getComponents()).queue();
            return;
        }

        GAMES.remove(messageId, game);

        ThreadChannel thread = event.getGuild().getThreadChannelById(game.getChannelId());
        if (thread == null) return;

        thread.sendMessage(String.format("Game cancelled! The region was: %s", game.getRegion().getName())).setComponents()
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
        if (game.getPossibleRegions().stream().noneMatch(region1 -> region1.getName().equalsIgnoreCase(region))) return;

        // check if the region is correct
        if (game.guess(region)) {
            var thread = (ThreadChannel) event.getChannel();
            thread.sendMessage(String.format("Correct! The region was: %s", game.getRegion().getName()))
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
            thread.sendMessage(String.format("Game over! The region was: %s", game.getRegion().getName())).setComponents()
                    .queue($ -> thread.getManager().setArchived(true).setLocked(true).queue());

            GAMES.remove(game.getMessageId(), game);

            // remove components on original message
            TextChannel channel = event.getJDA().getTextChannelById(game.getOwnerChannelId());
            if (channel == null) return;

            channel.retrieveMessageById(game.getMessageId()).queue(message -> message.editMessageComponents().queue());
        }
    }

    @Getter
    public static class Game {
        private final Region region;
        private final long guildId, ownerChannelId, channelId, messageId, userId;
        private final List<Region> guesses = new ArrayList<>();
        private final List<Region> possibleRegions;

        public Game(Region region, long guildId, long ownerChannelId, long channelId, long messageId, long userId, List<Region> possibleRegions) {
            this.region = region;
            this.guildId = guildId;
            this.ownerChannelId = ownerChannelId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.userId = userId;
            this.possibleRegions = possibleRegions;
        }

        public boolean guess(String guess) {
            if (this.guesses.stream().anyMatch(region -> region.getName().equalsIgnoreCase(guess)))
                return false;

            for (Region region : possibleRegions) {
                if (region.getName().equalsIgnoreCase(guess)) {
                    this.guesses.add(region);

                    if (region.equals(this.region))
                        return true;
                }

                List<String> aliases = region.getAliases();
                if (aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(guess))) {
                    this.guesses.add(region);

                    if (region.equals(this.region))
                        return true;
                }
            }

            return false;
        }
    }
}
