package dev.darealturtywurty.superturtybot.core.util.discord;

import net.dv8tion.jda.api.entities.MessageReaction;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

public final class BotUtils {
    public static boolean compareEmote(MessageReaction reaction0, MessageReaction reaction1) {
        return reaction0.getEmoji().getName().equals(reaction1.getEmoji().getName());
    }

    public static Color generateRandomColor() {
        final var red = ThreadLocalRandom.current().nextFloat();
        final var green = ThreadLocalRandom.current().nextFloat();
        final var blue = ThreadLocalRandom.current().nextFloat();
        return new Color(red, green, blue);
    }

    public static Color generateRandomPastelColor() {
        final var rand = ThreadLocalRandom.current();
        final var hue = rand.nextFloat();
        // Saturation between 0.1 and 0.3
        final var saturation = (rand.nextInt(2000) + 1000) / 10000f;
        final var luminance = 0.9f;
        return Color.getHSBColor(hue, saturation, luminance);
    }
    
    /**
     * Takes a BufferedImage and resizes it according to the provided targetSize
     *
     * @param  src        the source BufferedImage
     * @param  targetSize maximum height (if portrait) or width (if landscape)
     * @return            a resized version of the provided BufferedImage
     */
    public static BufferedImage resize(final BufferedImage src, final int targetSize) {
        if (targetSize <= 0)
            return src;
        int targetWidth = targetSize;
        int targetHeight = targetSize;
        final float ratio = (float) src.getHeight() / (float) src.getWidth();
        if (ratio <= 1) { // square or landscape-oriented image
            targetHeight = (int) Math.ceil(targetWidth * ratio);
        } else { // portrait image
            targetWidth = Math.round(targetHeight / ratio);
        }
        
        final BufferedImage retImg = new BufferedImage(targetWidth, targetHeight,
            src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = retImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return retImg;
    }

    public static @NotNull InputStream toInputStream(@NotNull BufferedImage image) {
        try {
            final var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
