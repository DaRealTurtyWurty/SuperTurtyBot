package io.github.darealturtywurty.superturtybot.commands.levelling;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.conversions.Bson;

import com.google.common.collect.Lists;
import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class LeaderboardCommand extends CoreCommand {
    private static final Color GOLD_COLOR = Color.decode("#ffd700");
    private static final Color SILVER_COLOR = Color.decode("#e7e7e7");
    private static final Color BRONZE_COLOR = Color.decode("#cd7f32");
    private static final char[] CHARS = { 'k', 'm', 'b', 't' };
    
    private final Font usedFont;
    
    public LeaderboardCommand() {
        super(new Types(true, false, false, false));
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            this.usedFont = Font
                .createFont(Font.TRUETYPE_FONT, TurtyBot.class.getResourceAsStream("/fonts/JetBrainsMono-Medium.ttf"))
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("You can only use this command inside of a server!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        event.deferReply().queue();
        
        final Bson filter = Filters.eq("guild", event.getGuild().getIdLong());
        List<Levelling> profiles = new ArrayList<>();
        Database.getDatabase().levelling.find(filter).forEach(profiles::add);
        if (profiles.isEmpty()) {
            event.getHook().sendMessage("❌ This server has no levels!").mentionRepliedUser(false).queue();
            return;
        }
        
        profiles = profiles.stream().sorted(Comparator.comparing(Levelling::getXp).reversed()).toList();

        final List<Levelling> top10 = Lists.partition(profiles, 10).get(0);
        try {
            final BufferedImage lb = constructLeaderboard(event.getGuild(), top10);
            final var bao = new ByteArrayOutputStream();
            ImageIO.write(lb, "png", bao);
            event.getHook().sendFile(bao.toByteArray(), "leaderboard.png").mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            event.getHook().sendMessage(
                "There has been an issue processing this leaderboard. The bot owner has been informed of this issue.")
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
        
        final BufferedImage guildIcon = RankCommand.resize(ImageIO.read(new URL(guild.getIconUrl())), 420);
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
                    + " | XP: " + numberFormat(xp, 0).replace(".0", "") + " | Level: "
                    + numberFormat(level, 0).replace(".0", ""),
                420, startY + metrics.getHeight() + (spacing + partHeight) * indexedRank);
        }
        
        graphics.dispose();
        return buffer;
    }
    
    private static BufferedImage getTemplate() throws IOException {
        return ImageIO.read(TurtyBot.class.getResourceAsStream("/levels/leaderboard.png"));
    }
    
    /**
     * Recursive implementation, invokes itself for each factor of a thousand, increasing the class on each invokation.
     *
     * @param  n         the number to format
     * @param  iteration in fact this is the class from the array c
     * @return           a String representing the number n formatted in a cool looking way.
     */
    // TODO: Utility class
    private static String numberFormat(final double n, final int iteration) {
        if (n < 1000)
            return String.valueOf(n);
        final double d = (long) n / 100 / 10D;
        final boolean isRound = d * 10 % 10 == 0;// true if the decimal part is equal to 0 (then it's trimmed
                                                 // anyway)
        return d < 1000 ? // this determines the class, i.e. 'k', 'm' etc
            (d > 99.9D || isRound && d > 9.99D ? // this decides whether to trim the decimals
                (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
            ) + "" + CHARS[iteration] : numberFormat(d, iteration + 1);
    }
}
