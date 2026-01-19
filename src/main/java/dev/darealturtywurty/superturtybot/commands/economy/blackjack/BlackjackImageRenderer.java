package dev.darealturtywurty.superturtybot.commands.economy.blackjack;

import dev.darealturtywurty.superturtybot.TurtyBot;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlackjackImageRenderer {
    private static final int PADDING = 24;
    private static final int GAP = 16;
    private static final int ROW_GAP = 28;
    private static final int LABEL_GAP = 10;
    private static final Color TABLE_TOP = new Color(0x0E6B2E);
    private static final Color TABLE_BOTTOM = new Color(0x0B4F24);
    private static final Color TEXT_COLOR = new Color(0xF5F1E8);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 110);
    private static final Font LABEL_FONT = new Font("Serif", Font.BOLD, 78);
    private static final int CARD_CORNER_RADIUS = 32;

    private static final Map<String, BufferedImage> CARD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> BACK_CACHE = new ConcurrentHashMap<>();

    private BlackjackImageRenderer() {
    }

    public static FileUpload createUpload(BlackjackCommand.Game game, boolean revealDealer) throws IOException {
        BufferedImage image = createImage(game, revealDealer);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return FileUpload.fromData(baos.toByteArray(), "blackjack.png");
    }

    public static BufferedImage createImage(BlackjackCommand.Game game, boolean revealDealer) {
        List<BlackjackCommand.Card> dealerCards = game.getDealerHand().getCards();
        List<BlackjackCommand.Card> playerCards = game.getPlayerHand().getCards();
        BufferedImage sample = resolveSampleImage(dealerCards, playerCards);
        int cardWidth = sample.getWidth();
        int cardHeight = sample.getHeight();

        int maxCards = Math.max(1, Math.max(dealerCards.size(), playerCards.size()));
        int handWidth = maxCards * cardWidth + (maxCards - 1) * GAP;
        int width = handWidth + PADDING * 2;

        Graphics2D measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        measure.setFont(LABEL_FONT);
        FontMetrics labelMetrics = measure.getFontMetrics();
        int labelHeight = labelMetrics.getAscent();
        measure.dispose();

        int dealerLabelY = PADDING + labelHeight;
        int dealerCardsY = dealerLabelY + LABEL_GAP;
        int playerLabelY = dealerCardsY + cardHeight + ROW_GAP + labelHeight;
        int playerCardsY = playerLabelY + LABEL_GAP;
        int height = playerCardsY + cardHeight + PADDING;

        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configureGraphics(graphics);

        graphics.setPaint(new GradientPaint(0, 0, TABLE_TOP, 0, height, TABLE_BOTTOM));
        graphics.fillRect(0, 0, width, height);

        String dealerLabel = "Dealer (Value: " + (revealDealer ? game.getDealerHand().bestValue() : "?") + ")";
        String playerLabel = "Player (Value: " + game.getPlayerHand().bestValue() + ")";
        drawLabel(graphics, dealerLabel, PADDING, dealerLabelY);
        drawLabel(graphics, playerLabel, PADDING, playerLabelY);

        int dealerStartX = centerHandX(width, dealerCards.size(), cardWidth);
        int playerStartX = centerHandX(width, playerCards.size(), cardWidth);
        drawHand(graphics, dealerCards, dealerStartX, dealerCardsY, cardWidth, cardHeight, !revealDealer);
        drawHand(graphics, playerCards, playerStartX, playerCardsY, cardWidth, cardHeight, false);

        graphics.dispose();
        return image;
    }

    private static BufferedImage resolveSampleImage(List<BlackjackCommand.Card> dealerCards,
                                                    List<BlackjackCommand.Card> playerCards) {
        if (!dealerCards.isEmpty())
            return getCardImage(dealerCards.getFirst());

        if (!playerCards.isEmpty())
            return getCardImage(playerCards.getFirst());

        return createCardBack(140, 190);
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void drawLabel(Graphics2D graphics, String text, int x, int y) {
        graphics.setFont(LABEL_FONT);
        graphics.setColor(SHADOW_COLOR);
        graphics.drawString(text, x + 2, y + 2);
        graphics.setColor(TEXT_COLOR);
        graphics.drawString(text, x, y);
    }

    private static int centerHandX(int totalWidth, int cardCount, int cardWidth) {
        if (cardCount <= 0)
            return (totalWidth - cardWidth) / 2;

        int handWidth = cardCount * cardWidth + (cardCount - 1) * GAP;
        return (totalWidth - handWidth) / 2;
    }

    private static void drawHand(Graphics2D graphics, List<BlackjackCommand.Card> cards, int startX, int y,
                                 int cardWidth, int cardHeight, boolean hideHoleCard) {
        int x = startX;
        for (int i = 0; i < cards.size(); i++) {
            BufferedImage cardImage = (hideHoleCard && i == 1)
                    ? getCardBack(cardWidth, cardHeight)
                    : getCardImage(cards.get(i));
            drawCardShadow(graphics, x, y, cardWidth, cardHeight);
            graphics.drawImage(cardImage, x, y, null);
            x += cardWidth + GAP;
        }
    }

    private static void drawCardShadow(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setColor(new Color(0, 0, 0, 80));
        graphics.fillRoundRect(x + 5, y + 6, width - 2, height - 2, 16, 16);
    }

    private static BufferedImage getCardImage(BlackjackCommand.Card card) {
        String key = card.getAssetName();
        return CARD_CACHE.computeIfAbsent(key, BlackjackImageRenderer::loadCardImage);
    }

    private static BufferedImage loadCardImage(String assetName) {
        try (InputStream stream = TurtyBot.loadResource("cards/" + assetName + ".png")) {
            if (stream == null)
                throw new IllegalStateException("Could not load card image: " + assetName);

            BufferedImage image = ImageIO.read(stream);
            if (image == null)
                throw new IllegalStateException("Card image is unreadable: " + assetName);

            return roundCardCorners(image);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read card image: " + assetName, exception);
        }
    }

    private static BufferedImage getCardBack(int width, int height) {
        String key = width + "x" + height;
        return BACK_CACHE.computeIfAbsent(key, ignored -> roundCardCorners(createCardBack(width, height)));
    }

    private static BufferedImage createCardBack(int width, int height) {
        var back = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = back.createGraphics();
        configureGraphics(graphics);

        graphics.setColor(new Color(0x1B4B8F));
        graphics.fillRoundRect(0, 0, width - 1, height - 1, 18, 18);

        graphics.setColor(new Color(0x163B73));
        graphics.fillRoundRect(6, 6, width - 12, height - 12, 14, 14);

        graphics.setStroke(new BasicStroke(3f));
        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.drawRoundRect(4, 4, width - 9, height - 9, 16, 16);

        graphics.setColor(new Color(255, 255, 255, 120));
        for (int i = -height; i < width; i += 14) {
            graphics.drawLine(i, 0, i + height, height);
        }

        graphics.dispose();
        return back;
    }

    private static BufferedImage roundCardCorners(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        var rounded = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = rounded.createGraphics();
        configureGraphics(graphics);
        graphics.setClip(new RoundRectangle2D.Float(0, 0, width, height, CARD_CORNER_RADIUS * 2F, CARD_CORNER_RADIUS * 2F));
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rounded;
    }
}
