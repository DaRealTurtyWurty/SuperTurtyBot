package dev.darealturtywurty.superturtybot.commands.levelling;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public final class RankCardTextures {
    private RankCardTextures() {
    }

    public static BufferedImage cover(BufferedImage source, int width, int height) {
        var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = Math.max((double) width / source.getWidth(), (double) height / source.getHeight());
        int scaledWidth = (int) Math.ceil(source.getWidth() * scale);
        int scaledHeight = (int) Math.ceil(source.getHeight() * scale);
        graphics.drawImage(source, (width - scaledWidth) / 2, (height - scaledHeight) / 2,
            scaledWidth, scaledHeight, null);
        graphics.dispose();
        return result;
    }

    public static void drawBar(
        Graphics2D graphics,
        BufferedImage texture,
        Color color,
        int x,
        int y,
        int width,
        int height,
        float fraction,
        boolean outline
    ) {
        Graphics2D layer = (Graphics2D) graphics.create();
        Shape shape = new RoundRectangle2D.Float(x, y, width, height, 30, 30);
        if (outline) {
            var ring = new Area(shape);
            ring.subtract(new Area(new RoundRectangle2D.Float(x + 7, y + 7, width - 14, height - 14, 20, 20)));
            shape = ring;
        }
        layer.clip(shape);
        layer.clipRect(x, y, Math.round(width * Math.clamp(fraction, 0f, 1f)), height);
        if (texture == null) {
            layer.setColor(color);
            layer.fillRect(x, y, width, height);
        } else {
            layer.drawImage(cover(texture, width, height), x, y, null);
        }
        layer.dispose();
    }

    public static BufferedImage preview(BufferedImage source, RankCardItem.Type type) {
        var result = new BufferedImage(140, 85, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (type) {
            case BACKGROUND_IMAGE -> graphics.drawImage(cover(source, 140, 85), 0, 0, null);
            case OUTLINE_IMAGE -> {
                Area frame = new Area(new RoundRectangle2D.Float(0, 0, 140, 85, 12, 12));
                frame.subtract(new Area(new RoundRectangle2D.Float(6, 6, 128, 73, 8, 8)));
                graphics.clip(frame);
                graphics.drawImage(cover(source, 140, 85), 0, 0, null);
            }
            case XP_EMPTY_IMAGE, XP_FILL_IMAGE, XP_OUTLINE_IMAGE ->
                drawBar(graphics, source, Color.WHITE, 0, 25, 140, 35, 1f,
                    type == RankCardItem.Type.XP_OUTLINE_IMAGE);
            case AVATAR_OUTLINE_IMAGE -> {
                var ring = new Area(new Ellipse2D.Float(32, 4, 77, 77));
                ring.subtract(new Area(new Ellipse2D.Float(42, 14, 57, 57)));
                graphics.clip(ring);
                graphics.drawImage(cover(source, 77, 77), 32, 4, null);
            }
        }
        graphics.dispose();
        return result;
    }
}
