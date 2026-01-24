package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import dev.darealturtywurty.superturtybot.TurtyBot;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BattleshipsImageRenderer {
    private static final int PADDING = 24;
    private static final int BOARD_GAP = 48;
    private static final int LABEL_GAP = 12;
    private static final int AXIS_GAP = 8;
    private static final int CELL_SIZE = 64;
    private static final int GRID_LINE_WIDTH = 3;
    private static final int SHIP_INSET = 6;
    private static final Color SEA_COLOR = new Color(0x1E90FF);
    private static final Color GRID_COLOR = new Color(0xFFFFFF);
    private static final Color TEXT_COLOR = new Color(0xFFFFFF);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 110);
    private static final Color HIT_COLOR = new Color(0xE84A4A);
    private static final Color MISS_COLOR = new Color(0xF5F5F5);
    private static final Color SCANNED_SHIP_COLOR = new Color(0xF4A63A);
    private static final Font LABEL_FONT = new Font("Serif", Font.BOLD, 78);
    private static final Font AXIS_FONT = new Font("Serif", Font.BOLD, 32);

    private static final Map<String, BufferedImage> SHIP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> MARKER_CACHE = new ConcurrentHashMap<>();

    private BattleshipsImageRenderer() {
    }

    public static FileUpload createUploadBothPlayers(BattleshipsCommand.Game game) throws IOException {
        return createUpload(game, new String[0], game.getPlayer1().getUserId(), game.getPlayer2().getUserId());
    }

    public static FileUpload createUpload(BattleshipsCommand.Game game) throws IOException {
        return createUpload(game, new String[0]);
    }

    public static FileUpload createUpload(BattleshipsCommand.Game game, long viewerId) throws IOException {
        return createUpload(game, new String[0], viewerId);
    }

    public static FileUpload createUpload(BattleshipsCommand.Game game, long... viewerIds) throws IOException {
        return createUpload(game, new String[0], viewerIds);
    }

    public static FileUpload createUpload(BattleshipsCommand.Game game, String[] playerNames, long... viewerIds) throws IOException {
        BufferedImage image = createImage(game, playerNames, viewerIds, -1L, Collections.emptyList(), null);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return FileUpload.fromData(baos.toByteArray(), "battleships.png");
    }

    public static FileUpload createUploadWithHighlights(BattleshipsCommand.Game game, String[] playerNames, long viewerId,
                                                        List<Point> highlights, Color highlightColor) throws IOException {
        BufferedImage image = createImage(game, playerNames, new long[]{viewerId}, viewerId, highlights, highlightColor);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return FileUpload.fromData(baos.toByteArray(), "battleships.png");
    }

    private static BufferedImage createImage(BattleshipsCommand.Game game, String[] playerNames, long[] viewerIds,
                                             long highlightOwnerId,
                                             List<Point> highlights, Color highlightColor) {
        boolean showPlayer1Ships = hasViewer(viewerIds, game.getPlayer1().getUserId());
        boolean showPlayer2Ships = hasViewer(viewerIds, game.getPlayer2().getUserId());

        Graphics2D measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        measure.setFont(LABEL_FONT);
        FontMetrics labelMetrics = measure.getFontMetrics();
        int labelHeight = labelMetrics.getAscent();
        measure.setFont(AXIS_FONT);
        FontMetrics axisMetrics = measure.getFontMetrics();
        int axisHeight = axisMetrics.getAscent();
        int axisWidth = axisMetrics.stringWidth("10");
        measure.dispose();

        int gridSize = BattleshipsCommand.BOARD_SIZE * CELL_SIZE;
        int leftAxisWidth = axisWidth + AXIS_GAP;
        int topAxisHeight = axisHeight + AXIS_GAP;
        int boardWidth = leftAxisWidth + gridSize;
        int boardHeight = labelHeight + LABEL_GAP + topAxisHeight + gridSize;

        int width = PADDING * 2 + boardWidth * 2 + BOARD_GAP;
        int height = PADDING * 2 + boardHeight;

        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configureGraphics(graphics);

        graphics.setColor(new Color(0x0B3A66));
        graphics.fillRect(0, 0, width, height);

        int board1X = PADDING;
        int board2X = board1X + boardWidth + BOARD_GAP;
        int boardY = PADDING;

        String player1Label = resolveLabel(playerNames, 0, "Player 1");
        String player2Label = resolveLabel(playerNames, 1, "Player 2");

        drawBoard(graphics, game, game.getPlayer1().getUserId(), player1Label, board1X, boardY, leftAxisWidth, topAxisHeight,
                labelHeight, showPlayer1Ships, highlightOwnerId == game.getPlayer1().getUserId() ? highlights : null, highlightColor);
        drawBoard(graphics, game, game.getPlayer2().getUserId(), player2Label, board2X, boardY, leftAxisWidth, topAxisHeight,
                labelHeight, showPlayer2Ships, highlightOwnerId == game.getPlayer2().getUserId() ? highlights : null, highlightColor);

        graphics.dispose();
        return image;
    }

    private static String resolveLabel(String[] playerNames, int index, String fallback) {
        if (playerNames != null && playerNames.length > index) {
            String value = playerNames[index];
            if (value != null && !value.isBlank())
                return value;
        }
        return fallback;
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static boolean hasViewer(long[] viewerIds, long playerId) {
        if (viewerIds == null || viewerIds.length == 0)
            return false;

        for (long viewerId : viewerIds) {
            if (viewerId == playerId)
                return true;
        }

        return false;
    }

    private static void drawBoard(Graphics2D graphics, BattleshipsCommand.Game game, long ownerId, String label,
                                  int boardX, int boardY, int leftAxisWidth, int topAxisHeight, int labelHeight,
                                  boolean showShips, List<Point> highlights, Color highlightColor) {
        int gridSize = BattleshipsCommand.BOARD_SIZE * CELL_SIZE;
        int labelY = boardY + labelHeight;

        drawLabel(graphics, label, boardX + leftAxisWidth, labelY);

        int axisLabelY = labelY + LABEL_GAP + topAxisHeight;
        int gridX = boardX + leftAxisWidth;
        int gridY = axisLabelY;

        drawAxisLabels(graphics, boardX, labelY + LABEL_GAP, leftAxisWidth, gridX, gridY);
        drawSea(graphics, gridX, gridY, gridSize);

        BattleshipsCommand.Battleship[] ships = game.getShips(ownerId);
        boolean[][] occupied = buildOccupiedGrid(ships);

        if (highlights != null && !highlights.isEmpty()) {
            drawHighlights(graphics, highlights, highlightColor == null ? new Color(255, 120, 120, 160) : highlightColor,
                    gridX, gridY);
        }

        drawShips(graphics, ships, gridX, gridY, !showShips);
        drawGrid(graphics, gridX, gridY, gridSize);

        long attackerId = game.getOpponentId(ownerId);
        drawMarkers(graphics, game, attackerId, occupied, gridX, gridY);
    }

    private static void drawHighlights(Graphics2D graphics, List<Point> highlights, Color color,
                                       int gridX, int gridY) {
        graphics.setColor(color);
        for (Point point : highlights) {
            if (point == null)
                continue;

            int x = point.x;
            int y = point.y;
            if (x < 0 || x >= BattleshipsCommand.BOARD_SIZE || y < 0 || y >= BattleshipsCommand.BOARD_SIZE)
                continue;

            int drawX = gridX + x * CELL_SIZE + 3;
            int drawY = gridY + y * CELL_SIZE + 3;
            int size = CELL_SIZE - 6;
            graphics.fillRoundRect(drawX, drawY, size, size, 12, 12);
        }
    }

    private static void drawLabel(Graphics2D graphics, String text, int x, int y) {
        graphics.setFont(LABEL_FONT);
        graphics.setColor(SHADOW_COLOR);
        graphics.drawString(text, x + 2, y + 2);
        graphics.setColor(TEXT_COLOR);
        graphics.drawString(text, x, y);
    }

    private static void drawAxisLabels(Graphics2D graphics, int boardX, int axisTopY, int leftAxisWidth,
                                       int gridX, int gridY) {
        graphics.setFont(AXIS_FONT);
        graphics.setColor(TEXT_COLOR);
        FontMetrics metrics = graphics.getFontMetrics();

        for (int col = 0; col < BattleshipsCommand.BOARD_SIZE; col++) {
            char letter = (char) ('A' + col);
            int cellX = gridX + col * CELL_SIZE;
            int textWidth = metrics.charWidth(letter);
            int textX = cellX + (CELL_SIZE - textWidth) / 2;
            int textY = axisTopY + metrics.getAscent();
            graphics.drawString(String.valueOf(letter), textX, textY);
        }

        for (int row = 0; row < BattleshipsCommand.BOARD_SIZE; row++) {
            String number = String.valueOf(row + 1);
            int cellY = gridY + row * CELL_SIZE;
            int textWidth = metrics.stringWidth(number);
            int textX = boardX + leftAxisWidth - AXIS_GAP - textWidth;
            int textY = cellY + (CELL_SIZE + metrics.getAscent()) / 2 - 4;
            graphics.drawString(number, textX, textY);
        }
    }

    private static void drawSea(Graphics2D graphics, int x, int y, int size) {
        graphics.setColor(SEA_COLOR);
        graphics.fillRect(x, y, size, size);
    }

    private static void drawGrid(Graphics2D graphics, int x, int y, int size) {
        graphics.setColor(GRID_COLOR);
        graphics.setStroke(new BasicStroke(GRID_LINE_WIDTH));
        for (int i = 0; i <= BattleshipsCommand.BOARD_SIZE; i++) {
            int offset = i * CELL_SIZE;
            graphics.drawLine(x + offset, y, x + offset, y + size);
            graphics.drawLine(x, y + offset, x + size, y + offset);
        }
    }

    private static void drawShips(Graphics2D graphics, BattleshipsCommand.Battleship[] ships, int gridX, int gridY,
                                  boolean onlySunk) {
        for (BattleshipsCommand.Battleship ship : ships) {
            if (ship == null)
                continue;
            if (onlySunk && !ship.isSunk())
                continue;

            int length = ship.getType().getSize();
            int width = ship.getOrientation() == BattleshipsCommand.Orientation.HORIZONTAL
                    ? length * CELL_SIZE
                    : CELL_SIZE;
            int height = ship.getOrientation() == BattleshipsCommand.Orientation.HORIZONTAL
                    ? CELL_SIZE
                    : length * CELL_SIZE;

            int drawX = gridX + ship.getX() * CELL_SIZE + SHIP_INSET;
            int drawY = gridY + ship.getY() * CELL_SIZE + SHIP_INSET;
            int drawWidth = width - SHIP_INSET * 2;
            int drawHeight = height - SHIP_INSET * 2;

            BufferedImage sprite = getShipSprite(ship.getType(), ship.getOrientation(), drawWidth, drawHeight);
            graphics.drawImage(sprite, drawX, drawY, null);

            if (ship.isSunk()) {
                graphics.setColor(new Color(0, 0, 0, 120));
                graphics.setStroke(new BasicStroke(6f));
                graphics.drawLine(drawX, drawY, drawX + drawWidth, drawY + drawHeight);
                graphics.drawLine(drawX + drawWidth, drawY, drawX, drawY + drawHeight);
            }
        }
    }

    private static boolean[][] buildOccupiedGrid(BattleshipsCommand.Battleship[] ships) {
        boolean[][] occupied = new boolean[BattleshipsCommand.BOARD_SIZE][BattleshipsCommand.BOARD_SIZE];
        for (BattleshipsCommand.Battleship ship : ships) {
            if (ship == null)
                continue;

            int[][] positions = BattleshipsCommand.Battleship.getPositions(
                    ship.getType(), ship.getOrientation(), ship.getX(), ship.getY());
            for (int[] position : positions) {
                occupied[position[0]][position[1]] = true;
            }
        }

        return occupied;
    }

    private static void drawMarkers(Graphics2D graphics, BattleshipsCommand.Game game, long attackerId,
                                    boolean[][] occupied, int gridX, int gridY) {
        int markerSize = (int) (CELL_SIZE * 0.55f);
        for (int x = 0; x < BattleshipsCommand.BOARD_SIZE; x++) {
            for (int y = 0; y < BattleshipsCommand.BOARD_SIZE; y++) {
                int drawX = gridX + x * CELL_SIZE + (CELL_SIZE - markerSize) / 2;
                int drawY = gridY + y * CELL_SIZE + (CELL_SIZE - markerSize) / 2;

                if (game.wasHit(attackerId, x, y)) {
                    boolean hit = occupied[x][y];
                    BufferedImage marker = getMarkerSprite(hit ? "hit" : "miss", markerSize);
                    if (marker != null) {
                        graphics.drawImage(marker, drawX, drawY, null);
                    } else {
                        graphics.setColor(hit ? HIT_COLOR : MISS_COLOR);
                        graphics.fillOval(drawX, drawY, markerSize, markerSize);
                        graphics.setColor(new Color(0, 0, 0, 90));
                        graphics.setStroke(new BasicStroke(3f));
                        graphics.drawOval(drawX, drawY, markerSize, markerSize);
                    }
                } else if (game.hasScannedPosition(attackerId, x, y)) {
                    graphics.setColor(occupied[x][y] ? SCANNED_SHIP_COLOR : MISS_COLOR);
                    graphics.fillOval(drawX, drawY, markerSize, markerSize);
                    graphics.setColor(new Color(0, 0, 0, 90));
                    graphics.setStroke(new BasicStroke(3f));
                    graphics.drawOval(drawX, drawY, markerSize, markerSize);
                }
            }
        }
    }

    private static BufferedImage getShipSprite(BattleshipsCommand.ShipType type,
                                               BattleshipsCommand.Orientation orientation,
                                               int width, int height) {
        String key = type.name() + "_" + orientation.name() + "_" + width + "x" + height;
        return SHIP_CACHE.computeIfAbsent(key, ignored -> loadShipSprite(type, orientation, width, height));
    }

    private static BufferedImage loadShipSprite(BattleshipsCommand.ShipType type,
                                                BattleshipsCommand.Orientation orientation,
                                                int width, int height) {
        String assetName = "battleships/ships/" + type.name().toLowerCase(Locale.ROOT)
                + "_" + orientation.name().toLowerCase(Locale.ROOT) + ".png";
        try (InputStream stream = TurtyBot.loadResource(assetName)) {
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                if (image != null)
                    return scaleImage(image, width, height);
            }
        } catch (IOException ignored) {
        }

        return createShipPlaceholder(type, width, height);
    }

    private static BufferedImage getMarkerSprite(String name, int size) {
        String key = name + "_" + size;
        return MARKER_CACHE.computeIfAbsent(key, ignored -> loadMarkerSprite(name, size));
    }

    private static BufferedImage loadMarkerSprite(String name, int size) {
        String assetName = "battleships/markers/" + name + ".png";
        try (InputStream stream = TurtyBot.loadResource(assetName)) {
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                if (image != null)
                    return scaleImage(image, size, size);
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
        var scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        configureGraphics(graphics);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static BufferedImage createShipPlaceholder(BattleshipsCommand.ShipType type, int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        configureGraphics(graphics);

        graphics.setColor(getShipColor(type));
        graphics.fillRoundRect(0, 0, width, height, 18, 18);
        graphics.setColor(new Color(0, 0, 0, 120));
        graphics.setStroke(new BasicStroke(3f));
        graphics.drawRoundRect(1, 1, width - 3, height - 3, 18, 18);

        graphics.setFont(AXIS_FONT);
        String label = type.name().substring(0, 1);
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = (width - metrics.stringWidth(label)) / 2;
        int textY = (height + metrics.getAscent()) / 2 - 4;
        graphics.setColor(new Color(255, 255, 255, 200));
        graphics.drawString(label, textX, textY);

        graphics.dispose();
        return image;
    }

    private static Color getShipColor(BattleshipsCommand.ShipType type) {
        return switch (type) {
            case CARRIER -> new Color(0x2E5D78);
            case BATTLESHIP -> new Color(0x3F4E5E);
            case DESTROYER -> new Color(0x4A6A7B);
            case SUBMARINE -> new Color(0x2F6B7F);
            case PATROL_BOAT -> new Color(0x5A6E7F);
        };
    }
}
