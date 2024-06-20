package dev.darealturtywurty.superturtybot.commands.nsfw;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GuessSexPositionCommand extends CoreCommand {
    private static final List<SexPositionGame> GAMES = new ArrayList<>();
    private static final Map<Pair<Integer, Integer>, String> TILE_MAP = Map.of(Pair.of(0, 0), "Top Left", Pair.of(1, 0),
            "Top Middle", Pair.of(2, 0), "Top Right", Pair.of(0, 1), "Middle Left", Pair.of(1, 1), "Middle",
            Pair.of(2, 1), "Middle Right", Pair.of(0, 2), "Bottom Left", Pair.of(1, 2), "Bottom Middle", Pair.of(2, 2),
            "Bottom Right");


    public GuessSexPositionCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.NSFW;
    }

    @Override
    public String getDescription() {
        return "Displays a black image that hides a sex position. You can remove tiles 1 by 1 to guess the position.";
    }

    @Override
    public String getName() {
        return "guesssexposition";
    }

    @Override
    public String getRichName() {
        return "Guess Sex Position";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!NSFWCommand.isValidChannel(event.getChannel())) {
            event.deferReply(true).setContent("❌ This command can only be used in NSFW channels!").queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild != null) {
            GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong()))
                    .first();
            if (config == null) {
                event.deferReply(true).setContent("❌ This server has not been configured yet!").queue();
                return;
            }

            List<Long> enabledChannels = GuildData.getChannels(config.getNsfwChannels());
            if (enabledChannels.isEmpty()) {
                event.deferReply(true).setContent("❌ This server has no NSFW channels configured!").queue();
                return;
            }

            if (!enabledChannels.contains(event.getChannel().getIdLong())) {
                event.deferReply(true).setContent("❌ This channel is not configured as an NSFW channel!").queue();
                return;
            }
        }

        SexPosition position = SexPosition.values()[ThreadLocalRandom.current().nextInt(SexPosition.values().length)];
        String url = getRandomImage(position.chooseRandomUrl());
        if (url == null) {
            event.deferReply(true).setContent("❌ Failed to get a random image!").queue();
            return;
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new URI(url).toURL());
        } catch (IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to load image!", exception);
            reply(event, "❌ An error occurred while trying to load the image!");
            return;
        }

        if (image == null) {
            Constants.LOGGER.error("Image is null!");
            reply(event, "❌ An error occurred while trying to load the image!");
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        var newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                newImage.setRGB(x, y, 0);
            }
        }

        var boas = new ByteArrayOutputStream();
        try {
            ImageIO.write(newImage, "jpg", boas);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write image!", exception);
            reply(event, "❌ An error occurred while trying to load the image!");
            return;
        }

        var upload = FileUpload.fromData(boas.toByteArray(), "sex_position.jpg");
        event.deferReply().setContent("Reveal tiles one-by-one and guess the sex position when you are ready 😜!")
                .setFiles(upload).setComponents().queue(hook -> {
                    hook.retrieveOriginal().queue(message ->
                            message.reply("Which tile do you want to reveal?").queue(msg -> {
                                GAMES.add(new SexPositionGame(position, url, guild == null ? 0 : guild.getIdLong(),
                                        event.getChannel().getIdLong(), message.getIdLong(), msg.getIdLong(),
                                        event.getUser().getIdLong()));

                                var tileSelectMenu = StringSelectMenu.create(
                                        "position_tile-" + message.getId() + "-" + msg.getId() + "-" + event.getUser().getId());
                                tileSelectMenu.setPlaceholder("Select a tile to reveal");
                                tileSelectMenu.setRequiredRange(1, 1);
                                TILE_MAP.forEach(
                                        (pair, name) -> tileSelectMenu.addOption(name, pair.getLeft() + "-" + pair.getRight()));

                                msg.editMessageComponents(ActionRow.of(tileSelectMenu.build())).queue();

                                var positionSelectMenu = StringSelectMenu.create(
                                        "position_select_menu-" + message.getId() + "-" + msg.getId() + "-" + event.getUser()
                                                .getId());
                                positionSelectMenu.setPlaceholder("Guess which sex position");
                                positionSelectMenu.setRequiredRange(1, 1);
                                for (SexPosition sexPosition : SexPosition.values()) {
                                    positionSelectMenu.addOption(sexPosition.getName(),
                                            sexPosition.name().toLowerCase(Locale.ROOT));
                                }

                                message.editMessageComponents(ActionRow.of(positionSelectMenu.build())).queue();
                            }));

                    try {
                        upload.close();
                    } catch (IOException exception) {
                        Constants.LOGGER.error("Failed to close upload!", exception);
                    }
                });
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;

        if (!NSFWCommand.isValidChannel(event.getChannel())) return;

        String id = event.getComponentId();
        String[] split = id.split("-");
        if (split.length != 4) return;

        String type = split[0];
        long messageId = Long.parseLong(split[1]);
        long replyId = Long.parseLong(split[2]);
        long userId = Long.parseLong(split[3]);

        if (event.getUser().getIdLong() != userId) return;

        if (type.equals("position_tile") && replyId == event.getMessageIdLong()) {
            SexPositionGame game = GAMES.stream()
                    .filter(g -> g.guildId == event.getGuild().getIdLong() && g.channelId == event.getChannel()
                            .getIdLong() && g.getMessageId() == messageId).findFirst().orElse(null);
            if (game == null) return;

            if (game.isFullyRevealed()) {
                event.reply("You have already revealed all tiles!").setEphemeral(true).queue();
                return;
            }

            String value = event.getValues().getFirst();
            String[] splitValue = value.split("-");
            int x = Integer.parseInt(splitValue[0]);
            int y = Integer.parseInt(splitValue[1]);
            if (game.isRevealed(x, y)) {
                event.reply("You have already revealed this tile!").setEphemeral(true).queue();
                return;
            }

            game.reveal(x, y);

            BufferedImage image;
            try {
                image = ImageIO.read(new URI(game.getUrl()).toURL());
            } catch (IOException | URISyntaxException exception) {
                Constants.LOGGER.error("Failed to load image!", exception);
                event.reply("❌ An error occurred while trying to load the image!").setEphemeral(true).queue();
                return;
            }

            if (image == null) {
                Constants.LOGGER.error("Image is null!");
                event.reply("❌ An error occurred while trying to load the image!").setEphemeral(true).queue();
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            var newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int tileWidth = width / 3;
            int tileHeight = height / 3;

            for (int tileX = 0; tileX < 3; tileX++) {
                for (int tileY = 0; tileY < 3; tileY++) {
                    for (int x1 = tileX * tileWidth; x1 < (tileX + 1) * tileWidth; x1++) {
                        for (int y1 = tileY * tileHeight; y1 < (tileY + 1) * tileHeight; y1++) {
                            newImage.setRGB(x1, y1, game.isRevealed(tileX, tileY) ? image.getRGB(x1, y1) : 0);
                        }
                    }
                }
            }

            var boas = new ByteArrayOutputStream();
            try {
                ImageIO.write(newImage, "jpg", boas);
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to write image!", exception);
                event.reply("❌ An error occurred while trying to load the image!").setEphemeral(true).queue();
                return;
            }

            var upload = FileUpload.fromData(boas.toByteArray(), "sex_position.jpg");

            // remove option from menu
            var tileSelectMenu = StringSelectMenu.create("position_tile-" + messageId + "-" + replyId + "-" + userId);
            tileSelectMenu.setPlaceholder("Select a tile to reveal");
            tileSelectMenu.setRequiredRange(1, 1);
            TILE_MAP.forEach((pair, name) -> {
                if (!game.isRevealed(pair.getLeft(), pair.getRight())) {
                    tileSelectMenu.addOption(name, pair.getLeft() + "-" + pair.getRight());
                }
            });

            if (game.isFullyRevealed()) {
                event.editComponents().queue();
            } else {
                event.editSelectMenu(tileSelectMenu.build()).queue();
            }

            event.getChannel().asGuildMessageChannel().editMessageAttachmentsById(messageId, upload).queue();
        } else if (type.equals("position_select_menu") && messageId == event.getMessageIdLong()) {
            SexPositionGame game = GAMES.stream()
                    .filter(g -> g.guildId == event.getGuild().getIdLong() && g.channelId == event.getChannel()
                            .getIdLong() && g.getMessageId() == messageId).findFirst().orElse(null);
            if (game == null) return;

            String value = event.getValues().getFirst();
            SexPosition sexPosition = SexPosition.valueOf(value.toUpperCase(Locale.ROOT));
            if (sexPosition != game.getPosition()) {
                event.reply(
                        "❌ " + sexPosition.getName() + " was not the right position! The correct position was " + game.getPosition()
                                .getName() + "!").queue();
            } else if (LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
                int xpEarned = ThreadLocalRandom.current()
                        .nextInt((9 - game.getRevealedCount()) * 5, (9 - game.getRevealedCount()) * 20);
                event.reply("✅ You were correct! The correct position was " + game.getPosition()
                        .getName() + "! You " + "earned " + xpEarned + " XP!").queue();
                LevellingManager.INSTANCE.addXP(event.getGuild(), event.getUser(), xpEarned);
            }

            GAMES.remove(game);

            // fully reveal the image
            BufferedImage image;
            try {
                image = ImageIO.read(new URI(game.getUrl()).toURL());
            } catch (IOException | URISyntaxException exception) {
                Constants.LOGGER.error("Failed to load image!", exception);
                event.reply("❌ An error occurred while trying to load the image!").setEphemeral(true).queue();
                return;
            }

            if (image == null) {
                Constants.LOGGER.error("Image is null!");
                event.reply("❌ An error occurred while trying to load the image!").setEphemeral(true).queue();
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            var newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    newImage.setRGB(x, y, image.getRGB(x, y));
                }
            }

            var boas = new ByteArrayOutputStream();
            try {
                ImageIO.write(newImage, "jpg", boas);
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to write image!", exception);
                event.reply("❌ An error occurred while trying to load the image!").setEphemeral(true).queue();
                return;
            }

            var upload = FileUpload.fromData(boas.toByteArray(), "sex_position.jpg");
            event.getMessage().editMessageComponents().setFiles(upload).queue();
            event.getChannel().asGuildMessageChannel().deleteMessageById(replyId).queue();
        }
    }

    private static String getRandomImage(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements elements = document.select("div.container");
            Elements images = elements.select("div.qsxgsmgmari");
            Elements links = images.select("a");
            Elements img = links.select("img");
            List<String> urls = img.eachAttr("src");
            return urls.get(ThreadLocalRandom.current().nextInt(urls.size()));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to get random image!", exception);
            return null;
        }
    }

    @Getter
    public enum SexPosition {
        SIXTYNINE("69", "https://mysexpics.com/69/"), MISSIONARY("Missionary",
                "https://www.mysexpics.com/missionary/"), DOGGY_STYLE("Doggy Style",
                "https://mysexpics.com/doggystyle/"), ANAL("Anal", "https://mysexpics.com/anal/",
                "https://mysexpics.com/painful-anal/", "https://mysexpics.com/lesbian-anal/",
                "https://mysexpics.com/big-ass-anal/"), BLOWJOB("Blowjob", "https://mysexpics.com/blowjob/"), STANDING(
                "Standing", "https://mysexpics.com/standing/"), COWGIRL("Cowgirl",
                "https://mysexpics.com/cowgirl/"), MASTURBATION("Masturbation",
                "https://mysexpics.com/masturbation/"), ORGY("Orgy", "https://mysexpics.com/gangbang/",
                "https://mysexpics.com/orgy/", "https://mysexpics.com/group-sex/"), FISTING("Fisting",
                "https://mysexpics.com/fisting/"), HANDJOB("Handjob",
                "https://mysexpics.com/handjob/"), DOUBLE_PENETRATION("Double Penetration",
                "https://mysexpics.com/double-penetration/"), PUSSY_LICKING("Pussy Licking",
                "https://mysexpics.com/pussy-licking/");


        private final String name;
        private final String[] urls;

        SexPosition(String name, String... urls) {
            this.name = name;
            this.urls = urls;
        }

        public String chooseRandomUrl() {
            return this.urls[(int) (Math.random() * this.urls.length)];
        }
    }

    @Getter
    public static final class SexPositionGame {
        private final SexPosition position;
        private final String url;
        private final boolean[][] revealed;
        private final long guildId;
        private final long channelId;
        private final long messageId;
        private final long positionSelectionMessageId;
        private final long userId;

        public SexPositionGame(SexPosition position, String url, long guildId, long channelId, long messageId, long positionSelectionMessageId, long userId) {
            this.position = position;
            this.url = url;
            this.revealed = new boolean[3][3];
            this.guildId = guildId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.positionSelectionMessageId = positionSelectionMessageId;
            this.userId = userId;
        }

        public boolean isRevealed(int x, int y) {
            return this.revealed[x][y];
        }

        public void reveal(int x, int y) {
            this.revealed[x][y] = true;
        }

        public boolean isFullyRevealed() {
            for (boolean[] row : this.revealed) {
                for (boolean revealed : row) {
                    if (!revealed) {
                        return false;
                    }
                }
            }

            return true;
        }

        public int getRevealedCount() {
            int count = 0;
            for (boolean[] row : this.revealed) {
                for (boolean revealed : row) {
                    if (revealed) {
                        count++;
                    }
                }
            }

            return count;
        }
    }
}
