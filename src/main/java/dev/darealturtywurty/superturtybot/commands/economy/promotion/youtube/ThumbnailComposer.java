package dev.darealturtywurty.superturtybot.commands.economy.promotion.youtube;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ThumbnailComposer {
    private ThumbnailComposer() {
    }

    public static FileUpload createComparisonImageUpload(YoutubeVideo first, YoutubeVideo second) {
        BufferedImage image = createComparisonImage(first, second);
        var stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write image!", exception);
        }

        return FileUpload.fromData(stream.toByteArray(), "youtube_promotion.png");
    }

    private static BufferedImage createComparisonImage(YoutubeVideo first, YoutubeVideo second) {
        int thumbWidth = 320;
        int thumbHeight = 180;
        int padding = 16;
        int titleHeight = 48;
        int width = padding * 3 + thumbWidth * 2;
        int height = padding * 3 + titleHeight + thumbHeight;

        var canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(220, 20, 60));
        Font baseFont = new Font("Dialog", Font.BOLD, 14);
        graphics.setFont(baseFont);

        int rightX = padding * 2 + thumbWidth;
        int titleY = padding + 18;
        drawWrappedTitle(graphics, baseFont, "1) " + first.title(), padding, titleY, thumbWidth, titleHeight);
        drawWrappedTitle(graphics, baseFont, "2) " + second.title(), rightX, titleY, thumbWidth, titleHeight);

        BufferedImage leftThumb = fetchThumbnail(first.thumbnailUrl(), thumbWidth, thumbHeight);
        BufferedImage rightThumb = fetchThumbnail(second.thumbnailUrl(), thumbWidth, thumbHeight);
        int imageY = padding * 2 + titleHeight;
        graphics.drawImage(leftThumb, padding, imageY, thumbWidth, thumbHeight, null);
        graphics.drawImage(rightThumb, rightX, imageY, thumbWidth, thumbHeight, null);

        graphics.dispose();
        return canvas;
    }

    private static BufferedImage fetchThumbnail(String url, int width, int height) {
        if (url != null && !url.isBlank()) {
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        try (InputStream stream = body.byteStream()) {
                            BufferedImage image = ImageIO.read(stream);
                            if (image != null) {
                                var scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                                Graphics2D graphics = scaled.createGraphics();
                                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                graphics.drawImage(image, 0, 0, width, height, null);
                                graphics.dispose();
                                return scaled;
                            }
                        }
                    }
                }
            } catch (IOException exception) {
                Constants.LOGGER.warn("Failed to fetch YouTube thumbnail.", exception);
            }
        }

        var placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = placeholder.createGraphics();
        graphics.setColor(new Color(230, 230, 230));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.DARK_GRAY);
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics metrics = graphics.getFontMetrics();
        String text = "No Thumbnail";
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = (height + metrics.getAscent()) / 2;
        graphics.drawString(text, x, y);
        graphics.dispose();
        return placeholder;
    }

    private static void drawWrappedTitle(Graphics2D graphics, Font baseFont, String title, int x, int y, int width,
                                         int height) {
        Font font = baseFont;
        if (baseFont.canDisplayUpTo(title) != -1) {
            font = new Font("Dialog", baseFont.getStyle(), baseFont.getSize());
        }
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int lineHeight = metrics.getHeight();
        int maxLines = Math.max(1, height / lineHeight);
        List<String> lines = wrapText(title, metrics, width);
        int linesToDraw = Math.min(lines.size(), maxLines);
        for (int i = 0; i < linesToDraw; i++) {
            graphics.drawString(lines.get(i), x, y + i * lineHeight);
        }
    }

    private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        var current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (current.isEmpty()) {
                current.append(word);
                continue;
            }

            String candidate = current + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.append(" ").append(word);
            }
        }

        if (!current.isEmpty())
            lines.add(current.toString());
        return lines;
    }
}
