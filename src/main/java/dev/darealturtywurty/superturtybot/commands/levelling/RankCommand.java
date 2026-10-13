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
import dev.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;

@SuppressWarnings("DataFlowIssue") // TODO: Possibly hold the images in memory to avoid reading them every time and that
                                   // way this is irrelevant
public class RankCommand extends CoreCommand {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.#");
    private final Font usedFont;

    public RankCommand() {
        super(new Types(true, false, false, false));
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try (final InputStream stream = TurtyBot.loadResource("fonts/Code New Roman.otf")) {
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
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final Member member = event.getOption("member", event.getMember(),
            option -> event.getGuild().getMember(option.getAsUser()));

        if (member == null) {
            reply(event,
                "❌ You must supply a valid member of this server if you are going to provide the `member` option!",
                false, true);
            return;
        }

        if (member.getUser().isBot()) {
            reply(event, "❌ Bots cannot level up. Silly!");
            return;
        }

        if (!LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
            reply(event, "❌ Levels have been disabled for this server!", false, true);
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
            if (card == null) {
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
    private BufferedImage makeRankCard(
        final Member member,
        RankCard card,
        int level,
        int xp,
        int nextLevelXp,
        float xpPercent
    ) {
        try {
            BufferedImage avatar = BotUtils.resize(ImageIO.read(member.getEffectiveAvatar().download(512).join()), 512);
            return renderRankCard(member.getEffectiveName(), getRank(member), avatar, card,
                level, xp, nextLevelXp, xpPercent, this.usedFont);
        } catch (IOException exception) {
            Constants.LOGGER.error("Error getting rank card for {}", member.getEffectiveName(), exception);
            return null;
        }
    }

    static BufferedImage renderRankCard(
        String name,
        int rank,
        BufferedImage userAvatar,
        RankCard card,
        int level,
        int xp,
        int nextLevelXp,
        float xpPercent,
        Font font
    ) throws IOException {
        final int baseWidth = 2800;
        final int baseHeight = 845;
        final var rankCardBuffer = new BufferedImage(baseWidth, baseHeight,
            BufferedImage.TYPE_INT_ARGB);
        final var graphics = (Graphics2D) rankCardBuffer.getGraphics();
        graphics.addRenderingHints(
            new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        // RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);

        // Background: crop to fit, keeping the texture's proportions.
        BufferedImage background = readTexture(RankCardItem.Type.BACKGROUND_IMAGE, card.getBackgroundImage());
        if (background == null) {
            graphics.setColor(card.getBackgroundColor());
            graphics.fillRect(0, 0, baseWidth, baseHeight);
        } else {
            graphics.drawImage(RankCardTextures.cover(background, baseWidth, baseHeight), 0, 0, null);
        }

        BufferedImage outlineImg = readTexture(RankCardItem.Type.OUTLINE_IMAGE, card.getOutlineImage());
        if (outlineImg == null) {
            outlineImg = new BufferedImage(baseWidth, baseHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D layer = outlineImg.createGraphics();
            layer.setColor(card.getOutlineColor());
            layer.fillRect(0, 0, baseWidth, baseHeight);
            layer.dispose();
        }
        outlineImg = cutoutImageMiddle(outlineImg, baseWidth, baseHeight, 20);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
            Math.clamp(card.getOutlineOpacity(), 0f, 1f)));
        graphics.drawImage(outlineImg, 0, 0, null);
        graphics.setComposite(AlphaComposite.SrcOver);

        // Keep the card artwork visible while separating the text from busy backgrounds.
        graphics.setColor(new Color(10, 16, 24, 170));
        graphics.fillRoundRect(660, 70, 2000, 430, 40, 40);

        // Name
        graphics.setStroke(new BasicStroke(3));
        graphics.setColor(card.getNameTextColor());

        final float nameFontSize = name.length() <= 12
            ? 200f
            : name.length() <= 16 ? 150f : name.length() <= 20 ? 125f : name.length() <= 26 ? 100f : 90f;
        graphics.setFont(font.deriveFont(nameFontSize));
        graphics.drawString(name.length() > 26 ? name.substring(0, 26) + "..." : name, 700, 270);

        // Rank
        graphics.setColor(card.getRankTextColor());
        graphics.setFont(font.deriveFont(105f));

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
        graphics.setFont(font.deriveFont(75f));

        // XP
        final var currentXp = xp - LevellingManager.getXPForLevel(level);
        final var nextXp = nextLevelXp - LevellingManager.getXPForLevel(level);
        final var xpStr = currentXp + " / " + nextXp + " XP";

        graphics.setColor(card.getXpTextColor());
        drawRightAlignedString(graphics, xpStr, font.deriveFont(75f), 2600, 375);

        // Draw the textured border last so the empty/fill layers cannot obscure it.
        RankCardTextures.drawBar(graphics,
            readTexture(RankCardItem.Type.XP_EMPTY_IMAGE, card.getXpEmptyImage()),
            card.getXpEmptyColor(), 700, 550, 1900, 120, 1f, false);
        RankCardTextures.drawBar(graphics,
            readTexture(RankCardItem.Type.XP_FILL_IMAGE, card.getXpFillImage()),
            card.getXpFillColor(), 700, 550, 1900, 120, xpPercent / 100f, false);
        RankCardTextures.drawBar(graphics,
            readTexture(RankCardItem.Type.XP_OUTLINE_IMAGE, card.getXpOutlineImage()),
            card.getXpOutlineColor(), 700, 550, 1900, 120, 1f, true);

        graphics.setFont(font.deriveFont(90f));
        String percentText = DECIMAL_FORMAT.format(xpPercent) + "%";
        FontMetrics percentMetrics = graphics.getFontMetrics();
        graphics.setColor(new Color(10, 16, 24, 170));
        graphics.fillRoundRect(1480, 558, percentMetrics.stringWidth(percentText) + 40, 104, 24, 24);
        graphics.setColor(card.getPercentTextColor());
        int percentBaseline = 610 + (percentMetrics.getAscent() - percentMetrics.getDescent()) / 2;
        graphics.drawString(percentText, 1500, percentBaseline);

        // User Avatar

        BufferedImage avatarTexture = readTexture(RankCardItem.Type.AVATAR_OUTLINE_IMAGE,
            card.getAvatarOutlineImage());
        Shape ring = createRingShape(350, 300, userAvatar.getWidth() / 2f + 10, 10);
        Graphics2D avatarGraphics = (Graphics2D) graphics.create();
        avatarGraphics.clip(ring);
        if (avatarTexture == null) {
            avatarGraphics.setColor(card.getAvatarOutlineColor());
            avatarGraphics.fill(ring);
        } else {
            int diameter = userAvatar.getWidth() + 20;
            avatarGraphics.drawImage(RankCardTextures.cover(avatarTexture, diameter, diameter),
                350 - diameter / 2, 300 - diameter / 2, null);
        }
        avatarGraphics.dispose();
        drawAvatar(userAvatar, graphics);

        graphics.dispose();

        return rankCardBuffer;
    }

    static @Nullable BufferedImage readTexture(RankCardItem.Type type, String name) throws IOException {
        if (name == null || name.isBlank())
            return null;
        RankCardItem item = RankCardItemRegistry.resolveImage(type, name);
        if (item == null)
            return null;
        try (InputStream stream = TurtyBot.loadResource(item.getResourcePath())) {
            return stream == null ? null : ImageIO.read(stream);
        }
    }

    public static Shape createRingShape(double centerX, double centerY, double outerRadius, double thickness) {
        final Ellipse2D outer = new Ellipse2D.Double(centerX - outerRadius, centerY - outerRadius,
            outerRadius + outerRadius, outerRadius + outerRadius);
        final Ellipse2D inner = new Ellipse2D.Double(centerX - outerRadius + thickness,
            centerY - outerRadius + thickness, outerRadius + outerRadius - thickness - thickness,
            outerRadius + outerRadius - thickness - thickness);
        final var area = new Area(outer);
        area.subtract(new Area(inner));
        return area;
    }

    public static BufferedImage cutoutImageMiddle(
        final BufferedImage image,
        final int baseWidth,
        final int baseHeight,
        final int cornerRadius
    ) {
        final var output = new BufferedImage(baseWidth, baseHeight, BufferedImage.TYPE_INT_ARGB);

        final var g2 = output.createGraphics();
        final var area = new Area(new Rectangle2D.Double(0, 0, baseWidth, baseHeight));
        final var toSubtract = new Area(new RoundRectangle2D.Double(cornerRadius, cornerRadius,
            baseWidth - cornerRadius * 2, baseHeight - cornerRadius * 2, cornerRadius, cornerRadius));
        area.subtract(toSubtract);
        g2.clip(area);
        g2.drawImage(RankCardTextures.cover(image, baseWidth, baseHeight), 0, 0, null);
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

    public static void paintTextWithOutline(
        final Graphics g,
        final String text,
        final Font font,
        final Color outlineColor,
        final Color fillColor,
        final float outlineWidth
    ) {
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
