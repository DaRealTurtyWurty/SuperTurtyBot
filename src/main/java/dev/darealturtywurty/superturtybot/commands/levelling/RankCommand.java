package dev.darealturtywurty.superturtybot.commands.levelling;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.BotUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.RankCard;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.LineMetrics;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DataFlowIssue") // TODO: Possibly hold the images in memory to avoid reading them every time and that way this is irrelevant
public class RankCommand extends CoreCommand {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.#");
    private final Font usedFont;

    public RankCommand() {
        super(new Types(true, false, false, false));
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try(final InputStream stream = TurtyBot.loadResource("fonts/Code New Roman.otf")) {
            if (stream == null)
                throw new IllegalStateException("Unable to load font");

            this.usedFont = Font
                .createFont(Font.TRUETYPE_FONT, stream)
                .deriveFont(12f);
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException("Unable to load font", exception);
        }

        graphicsEnv.registerFont(this.usedFont);
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "member", "The member to gain rank information about", false));
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.LEVELLING;
    }

    @Override
    public String getDescription() {
        return "Gets the rank of yourself or the provided member";
    }
    
    @Override
    public String getHowToUse() {
        return "/rank\n/rank [user]";
    }

    @Override
    public String getName() {
        return "rank";
    }

    public int getRank(Member member) {
        if (member.getUser().isBot())
            return 0;

        final Bson filter = Filters.eq("guild", member.getGuild().getIdLong());
        List<Levelling> profiles = Database.getDatabase().levelling.find(filter).into(new ArrayList<>());
        profiles = profiles.stream()
                .filter(profile -> member.getGuild().getMemberById(profile.getUser()) != null)
                .sorted(Comparator.comparing(Levelling::getXp).reversed())
                .toList();

        final Optional<Levelling> found = profiles.stream().filter(profile -> profile.getUser() == member.getIdLong())
            .findFirst();
        if (found.isEmpty()) {
            final var profile = new Levelling(member.getGuild().getIdLong(), member.getIdLong());
            Database.getDatabase().levelling.insertOne(profile);
            return profiles.size() + 1;
        }

        return profiles.indexOf(found.get());
    }

    @Override
    public String getRichName() {
        return "Rank";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "You must be in a server to use this command!", false, true);
            return;
        }

        final Member member = event.getOption("member", event.getMember(),
            option -> event.getGuild().getMember(option.getAsUser()));

        if (member == null) {
            reply(event, "You must supply a valid member of this server if you are going to provide the `member` option!", false, true);
            return;
        }
        
        if (member.getUser().isBot()) {
            reply(event, "❌ Bots cannot level up. Silly!");
            return;
        }

        if (!LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
            reply(event, "Levels have been disabled for this server!", false, true);
            return;
        }

        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", member.getIdLong()));
        Levelling profile = Database.getDatabase().levelling.find(filter).first();
        if (profile == null) {
            profile = new Levelling(event.getGuild().getIdLong(), member.getIdLong());
            Database.getDatabase().levelling.insertOne(profile);
        }

        final int level = profile.getLevel();
        final int xp = profile.getXp();
        final int forLevel = LevellingManager.getXPForLevel(level + 1);
        final int prevXpRequire = LevellingManager.getXPForLevel(level);
        float percentage = (float) (xp - prevXpRequire) / (forLevel - prevXpRequire) * 100f;
        if (percentage < 0) {
            percentage = 0;
        }

        event.deferReply().mentionRepliedUser(false).queue();
        try {
            final BufferedImage card = makeRankCard(member, profile.getRankCard(), level, xp, forLevel, percentage);
            if(card == null) {
                event.getHook().editOriginal("❌ There has been an issue getting your rank card!").queue();
                return;
            }

            final var baos = new ByteArrayOutputStream();
            ImageIO.write(card, "png", baos);
            event.getHook().sendFiles(FileUpload.fromData(baos.toByteArray(), member.getId() + ".png")).queue();
        } catch (final IOException exception) {
            Constants.LOGGER.error("Error getting rank card for {}", member.getEffectiveName(), exception);
            event.getHook().editOriginal("❌ There has been an issue getting your rank card!").queue();
        }
    }

    @Nullable
    private BufferedImage makeRankCard(final Member member, RankCard card, int level, int xp, int nextLevelXp,
        float xpPercent) {
        try {
            final BufferedImage base = ImageIO
                .read(TurtyBot.class.getResourceAsStream("/levels/background/default.png"));
            final BufferedImage outline = ImageIO
                .read(TurtyBot.class.getResourceAsStream("/levels/outline/default.png"));
            final var rankCardBuffer = new BufferedImage(base.getWidth(), base.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
            final var graphics = (Graphics2D) rankCardBuffer.getGraphics();
            graphics.addRenderingHints(
                new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            // RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);

            // Background
            BufferedImage background;
            if (!card.getBackgroundImage().isBlank()) {
                background = ImageIO.read(
                    TurtyBot.class.getResourceAsStream("/levels/background/" + card.getBackgroundImage() + ".png"));
            } else {
                final var bgBuf = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
                final var bgGraphics = bgBuf.createGraphics();
                bgGraphics.setColor(card.getBackgroundColor());
                bgGraphics.fillRect(0, 0, base.getWidth(), base.getHeight());
                bgGraphics.dispose();
                background = bgBuf;
            }

            final var bgBuffer = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            final var bgGraphics = bgBuffer.createGraphics();
            bgGraphics.setClip(new Rectangle2D.Float(0, 0, base.getWidth(), base.getHeight()));
            bgGraphics.drawImage(background, 0, 0, base.getWidth(), base.getHeight(), null);
            bgGraphics.dispose();
            graphics.drawImage(bgBuffer, 0, 0, base.getWidth(), base.getHeight(), null);

            // Outline
            BufferedImage outlineImg;
            if (!card.getOutlineImage().isBlank()) {
                outlineImg = ImageIO
                    .read(TurtyBot.class.getResourceAsStream("/levels/outline/" + card.getOutlineImage() + ".png"));
            } else {
                final var outBuf = new BufferedImage(outline.getWidth(), outline.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
                final var outGraphics = outBuf.createGraphics();
                outGraphics.setColor(card.getOutlineColor());
                outGraphics.fillRect(0, 0, outline.getWidth(), outline.getHeight());
                outGraphics.dispose();
                outlineImg = outBuf;
            }

            outlineImg = cutoutImageMiddle(outlineImg, base.getWidth(), base.getHeight(), 20);

            final var outlineAlpha = card.getOutlineOpacity();
            final var alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, outlineAlpha);
            graphics.setComposite(alphaComp);
            graphics.drawImage(outlineImg, 0, 0, base.getWidth(), base.getHeight(), null);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

            // Name
            graphics.setStroke(new BasicStroke(3));
            graphics.setColor(card.getNameTextColor());

            final String name = member.getEffectiveName();
            final float nameFontSize = name.length() <= 12 ? 200f : name.length() <= 16 ? 150f : name.length() <= 20 ? 125f : name.length() <= 26 ? 100f : 90f;
            graphics.setFont(this.usedFont.deriveFont(nameFontSize));
            graphics.drawString(name.length() > 26 ? name.substring(0, 26) + "..." : name, 700, 270);

            // Rank
            graphics.setColor(card.getRankTextColor());
            graphics.setFont(this.usedFont.deriveFont(105f));

            int rank = getRank(member);

            var xModifier = 0;
            if (rank >= 10) {
                xModifier += 15;
            }

            if (rank >= 100) {
                xModifier += 15;
            }

            if (rank >= 1000) {
                xModifier += 15;
            }

            if (rank >= 10000) {
                xModifier += 15;
            }

            if (rank < 0) {
                rank = 0;
            }

            graphics.drawString("Rank #" + (rank + 1), 2200 - xModifier, 250);

            final var fontMetrics = graphics.getFontMetrics();
            final int textWidth = fontMetrics.stringWidth("Rank #" + (rank + 1));
            graphics.drawLine(700, 300 + fontMetrics.getDescent(), 2200 - xModifier + textWidth,
                300 + fontMetrics.getDescent());

            // Level
            final var levelStr = String.valueOf(level);
            graphics.setColor(card.getLevelTextColor());
            graphics.drawString("Level " + levelStr, 700, 450);
            graphics.setFont(this.usedFont.deriveFont(75f));

            // XP
            final var currentXp = xp - LevellingManager.getXPForLevel(level);
            final var nextXp = nextLevelXp - LevellingManager.getXPForLevel(level);
            final var xpStr = currentXp + " / " + nextXp + " XP";

            graphics.setColor(card.getXpTextColor());
            drawRightAlignedString(graphics, xpStr, this.usedFont.deriveFont(75f), 2600, 375);

            // XP Bar
            if (!card.getXpOutlineImage().isBlank()) {
                final BufferedImage xpBarOutline = makeRoundedCorner(
                    ImageIO.read(
                        TurtyBot.class.getResourceAsStream("/levels/xpOutline/" + card.getXpOutlineImage() + ".png")),
                    20);
                graphics.drawImage(xpBarOutline, 700, 550, 1900, 120, null);
            } else {
                graphics.setColor(card.getXpOutlineColor());
                graphics.drawRoundRect(700, 550, 1900, 120, 30, 30);
            }

            if (!card.getXpEmptyImage().isBlank()) {
                final BufferedImage xpBarEmpty = makeRoundedCorner(ImageIO.read(
                    TurtyBot.class.getResourceAsStream("/levels/xpEmpty/" + card.getXpEmptyImage() + ".png")), 20);
                graphics.drawImage(xpBarEmpty, 700, 550, 1900, 120, null);
            } else {
                graphics.setColor(card.getXpEmptyColor());
                graphics.fillRoundRect(700, 550, 1900, 120, 30, 30);
            }

            if (!card.getXpFillImage().isBlank()) {
                final BufferedImage xpBarFill = makeRoundedCorner(ImageIO
                    .read(TurtyBot.class.getResourceAsStream("/levels/xpFill/" + card.getXpFillImage() + ".png")), 20);
                graphics.drawImage(xpBarFill, 700, 550, (int) (1900 * (xpPercent * 0.01f)), 120, null);
            } else {
                graphics.setColor(card.getXpFillColor());
                graphics.fillRoundRect(700, 550, (int) (1900 * (xpPercent * 0.01f)), 120, 30, 30);
            }

            graphics.setColor(card.getPercentTextColor());
            graphics.setFont(this.usedFont.deriveFont(90f));

            graphics.drawString(DECIMAL_FORMAT.format(xpPercent) + "%", 1500, 640);

            // User Avatar
            BufferedImage userAvatar = ImageIO.read(member.getEffectiveAvatar().download(512).join());
            userAvatar = BotUtils.resize(userAvatar, 512);

            if (!card.getAvatarOutlineImage().isBlank()) {
                final var avatarOut = new BufferedImage(userAvatar.getWidth(), userAvatar.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
                final var avatarOutGraphics = avatarOut.createGraphics();
                avatarOutGraphics.setColor(card.getBackgroundColor());
                avatarOutGraphics.setClip(new Ellipse2D.Float(0, 0, userAvatar.getWidth(), userAvatar.getHeight()));
                avatarOutGraphics.drawImage(
                    ImageIO.read(TurtyBot.class
                        .getResourceAsStream("/levels/avatarOutline/" + card.getAvatarOutlineImage() + ".png")),
                    0, 0, userAvatar.getWidth(), userAvatar.getHeight(), null);
                avatarOutGraphics.dispose();
                graphics.drawImage(avatarOut, 350, 300, userAvatar.getWidth() + 30, userAvatar.getHeight() + 30, null);
                drawAvatar(userAvatar, graphics);
            } else {
                drawAvatar(userAvatar, graphics);
                graphics.setColor(card.getAvatarOutlineColor());
                final Shape shape = createRingShape(350, 300, userAvatar.getWidth() / 2f, 10);
                graphics.fill(shape);
                graphics.draw(shape);
            }

            graphics.dispose();

            return rankCardBuffer;
        } catch (final IOException exception) {
            Constants.LOGGER.error("Error getting rank card for {}", member.getEffectiveName(), exception);
        }

        return null;
    }

    public static Shape createRingShape(double centerX, double centerY, double outerRadius, double thickness) {
        final Ellipse2D outer = new Ellipse2D.Double(centerX - outerRadius, centerY - outerRadius,
            outerRadius + outerRadius, outerRadius + outerRadius);
        final Ellipse2D inner = new Ellipse2D.Double(centerX - outerRadius + thickness,
            centerY - outerRadius + thickness, outerRadius + outerRadius - thickness - thickness,
            outerRadius + outerRadius - thickness - thickness);
        final Area area = new Area(outer);
        area.subtract(new Area(inner));
        return area;
    }

    public static BufferedImage cutoutImageMiddle(final BufferedImage image, final int baseWidth, final int baseHeight,
        final int cornerRadius) {
        final var output = new BufferedImage(baseWidth, baseHeight, BufferedImage.TYPE_INT_ARGB);

        final var g2 = output.createGraphics();
        final var area = new Area(new Rectangle2D.Double(0, 0, baseWidth, baseHeight));
        final var toSubtract = new Area(new RoundRectangle2D.Double(cornerRadius, cornerRadius,
            baseWidth - cornerRadius * 2, baseHeight - cornerRadius * 2, cornerRadius, cornerRadius));
        area.subtract(toSubtract);
        g2.setPaint(new TexturePaint(image, new Rectangle2D.Double(0, 0, baseWidth, baseHeight)));
        g2.fill(area);
        g2.dispose();
        return output;
    }

    public static float drawRightAlignedString(Graphics2D g, String string, Font font, float x, float y) {
        final float startX = x - getTextWidth(g, string, font);
        return drawString(g, string, font, startX, y);
    }

    public static float drawString(Graphics2D g, String string, Font font, float x, float y) {
        g.setFont(font);
        y += font.getLineMetrics(string, g.getFontRenderContext()).getAscent();
        g.drawString(string, x, y);
        return getTextHeight(g, string, font);
    }

    public static float getTextHeight(Graphics2D g, String string, Font font) {
        final LineMetrics lm = font.getLineMetrics(string, g.getFontRenderContext());
        return lm.getHeight();
    }

    public static float getTextWidth(Graphics2D g, String string, Font font) {
        return g.getFontMetrics(font).stringWidth(string);
    }

    public static BufferedImage makeRoundedCorner(final BufferedImage image, final float cornerRadius) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final var output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final var g2 = output.createGraphics();

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return output;
    }

    public static void paintTextWithOutline(final Graphics g, final String text, final Font font,
        final Color outlineColor, final Color fillColor, final float outlineWidth) {
        final var outlineStroke = new BasicStroke(outlineWidth);

        if (g instanceof final Graphics2D g2) {
            // remember original settings
            final var originalColor = g2.getColor();
            final var originalStroke = g2.getStroke();
            final var originalHints = g2.getRenderingHints();

            // create a glyph vector from your text
            final var glyphVector = font.createGlyphVector(g2.getFontRenderContext(), text);
            // get the shape object
            final var textShape = glyphVector.getOutline();

            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2.setColor(outlineColor);
            g2.setStroke(outlineStroke);
            g2.draw(textShape); // draw outline

            g2.setColor(fillColor);
            g2.fill(textShape); // fill the shape

            // reset to original settings after painting
            g2.setColor(originalColor);
            g2.setStroke(originalStroke);
            g2.setRenderingHints(originalHints);
        }
    }
    
    private static void drawAvatar(final BufferedImage userAvatar, final Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(4));
        final var circleBuffer = new BufferedImage(userAvatar.getWidth(), userAvatar.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        final var avatarGraphics = circleBuffer.createGraphics();
        avatarGraphics.setClip(new Ellipse2D.Float(0, 0, userAvatar.getWidth() - 10, userAvatar.getHeight() - 10));
        avatarGraphics.drawImage(userAvatar, 0, 0, userAvatar.getWidth(), userAvatar.getHeight(), null);
        avatarGraphics.dispose();
        graphics.drawImage(circleBuffer, 100, 50, null);
    }
}
