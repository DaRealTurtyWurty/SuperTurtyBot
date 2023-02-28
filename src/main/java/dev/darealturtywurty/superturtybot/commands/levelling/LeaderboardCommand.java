package dev.darealturtywurty.superturtybot.commands.levelling;

import com.google.common.collect.Lists;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.BotUtils;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.conversions.Bson;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardCommand extends CoreCommand {
    private static final Color GOLD_COLOR = Color.decode("#ffd700");
    private static final Color SILVER_COLOR = Color.decode("#e7e7e7");
    private static final Color BRONZE_COLOR = Color.decode("#cd7f32");

    private final Font usedFont;

    public LeaderboardCommand() {
        super(new Types(true, false, false, false));
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            this.usedFont = Font
                    .createFont(Font.TRUETYPE_FONT,
                            TurtyBot.class.getResourceAsStream("/fonts/JetBrainsMono-Medium.ttf"))
                    .deriveFont(72f);
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException("Unable to load font", exception);
        }

        graphicsEnv.registerFont(this.usedFont);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.LEVELLING;
    }

    @Override
    public String getDescription() {
        return "Gets the levelling leaderboard for this server";
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getRichName() {
        return "Leaderboard";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("You can only use this command inside of a server!")
                 .mentionRepliedUser(false).queue();
            return;
        }

        event.deferReply().queue();

        final Bson filter = Filters.eq("guild", event.getGuild().getIdLong());
        final List<Levelling> profiles = new ArrayList<>();
        Database.getDatabase().levelling.find(filter).forEach(profiles::add);
        if (profiles.isEmpty()) {
            event.getHook().sendMessage("❌ This server has no levels!").mentionRepliedUser(false).queue();
            return;
        }

        final List<Levelling> sorted = profiles.stream().sorted(Comparator.comparing(Levelling::getXp).reversed())
                                               .filter(profile -> event.getGuild()
                                                                       .getMemberById(profile.getUser()) != null)
                                               .toList();

        final List<Levelling> top10 = Lists.partition(sorted, 10).get(0);
        try {
            final BufferedImage lb = constructLeaderboard(event.getGuild(), top10);
            final var bao = new ByteArrayOutputStream();
            ImageIO.write(lb, "png", bao);
            event.getHook().sendFiles(FileUpload.fromData(bao.toByteArray(), "leaderboard.png"))
                 .mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            event.getHook().sendMessage(
                         "There has been an issue processing this leaderboard. The bot owner has been informed of " +
                                 "this issue.")
                 .mentionRepliedUser(false).queue();
            Constants.LOGGER.error(ExceptionUtils.getStackTrace(exception));
        }
    }

    private BufferedImage constructLeaderboard(Guild guild, List<Levelling> profiles) throws IOException {
        final BufferedImage template = getTemplate();

        final var buffer = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = buffer.createGraphics();
        graphics.setFont(this.usedFont);
        final FontMetrics metrics = graphics.getFontMetrics();

        graphics.drawImage(template, 0, 0, template.getWidth(), template.getHeight(), null);

        final BufferedImage guildIcon = BotUtils.resize(ImageIO.read(new URL(guild.getIconUrl())), 420);
        graphics.drawImage(guildIcon, 125, 125, guildIcon.getWidth(), guildIcon.getHeight(), null);

        final String guildName = guild.getName();
        final int guildLength = metrics.stringWidth(guildName);
        graphics.drawString(guildName, 600, 300);
        graphics.setStroke(new BasicStroke(10));
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawLine(600, 300 + metrics.getHeight() / 2 - 20, 600 + guildLength,
                300 + metrics.getHeight() / 2 - 20);

        final int startX = 80, startY = 568, partHeight = 140, spacing = 40;
        for (int indexedRank = 0; indexedRank < 10; indexedRank++) {
            if (indexedRank >= profiles.size()) {
                break;
            }

            final Levelling profile = profiles.get(indexedRank);
            final long id = profile.getUser();
            final int level = profile.getLevel();
            final int xp = profile.getXp();
            final int rank = indexedRank + 1;

            final User user = guild.getJDA().getUserById(id);
            String avatarURL, username, discriminator;
            if (user == null) {
                avatarURL = "https://discord.com/assets/1f0bfc0865d324c2587920a7d80c609b.png";
                username = "Unknown";
                discriminator = "0000";
            } else {
                avatarURL = user.getEffectiveAvatarUrl();
                username = user.getName();
                discriminator = user.getDiscriminator();
            }

            final BufferedImage avatarImage = ImageIO.read(new URL(avatarURL));
            graphics.drawImage(avatarImage, startX, startY + (spacing + partHeight) * indexedRank, partHeight,
                    partHeight, null);

            switch (rank) {
                case 1:
                    graphics.setColor(GOLD_COLOR);
                    break;
                case 2:
                    graphics.setColor(SILVER_COLOR);
                    break;
                case 3:
                    graphics.setColor(BRONZE_COLOR);
                    break;
                default:
                    graphics.setColor(Color.LIGHT_GRAY);
                    break;
            }

            graphics.drawString("#" + rank, 240, startY + metrics.getHeight() + (spacing + partHeight) * indexedRank);

            graphics.setColor(Color.WHITE);
            graphics.drawString(
                    (username.length() > 15 ? username.substring(0, 15) + "..." : username) + "#" + discriminator
                            + " | XP: " + StringUtils.numberFormat(xp, 0).replace(".0", "") + " | Level: "
                            + StringUtils.numberFormat(level, 0).replace(".0", ""),
                    420, startY + metrics.getHeight() + (spacing + partHeight) * indexedRank);
        }

        graphics.dispose();
        return buffer;
    }

    private static BufferedImage getTemplate() throws IOException {
        return ImageIO.read(TurtyBot.class.getResourceAsStream("/levels/leaderboard.png"));
    }
}
