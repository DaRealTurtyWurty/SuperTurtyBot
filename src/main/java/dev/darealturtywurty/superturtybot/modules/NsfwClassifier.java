package dev.darealturtywurty.superturtybot.modules;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

public record NsfwClassifier(OrtEnvironment environment, OrtSession session, String inputName,
                             Config config) implements AutoCloseable {
    private static final AtomicBoolean LOGGED_SHAPE_ERROR = new AtomicBoolean(false);

    public static NsfwClassifier create(Path modelPath, Config config) throws OrtException {
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        OrtSession session = environment.createSession(modelPath.toString(), options);
        String inputName = session.getInputNames().iterator().next();
        return new NsfwClassifier(environment, session, inputName, config);
    }

    public OptionalDouble predictScore(Path imagePath) {
        BufferedImage inputImage = loadAndResize(imagePath, config.imageSize());
        if (inputImage == null)
            return OptionalDouble.empty();

        float[] data = toFloatArray(inputImage, config);
        long[] shape = config.layout() == InputLayout.NHWC
                ? new long[]{1, config.imageSize(), config.imageSize(), 3}
                : new long[]{1, 3, config.imageSize(), config.imageSize()};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(data), shape);
             OrtSession.Result results = session.run(Map.of(inputName, tensor))) {
            Object output = results.get(0).getValue();
            Double score = extractScore(output);
            if (score == null)
                return OptionalDouble.empty();

            return OptionalDouble.of(score);
        } catch (OrtException exception) {
            if (LOGGED_SHAPE_ERROR.compareAndSet(false, true)) {
                Constants.LOGGER.error("Failed to run NSFW classifier on {}.", imagePath, exception);
            }
            return OptionalDouble.empty();
        }
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException exception) {
            Constants.LOGGER.warn("Failed to close NSFW model session.", exception);
        }
    }

    private static BufferedImage loadAndResize(Path path, int size) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null)
                return null;

            BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, size, size, null);
            graphics.dispose();
            return resized;
        } catch (IOException exception) {
            return null;
        }
    }

    private static float[] toFloatArray(BufferedImage image, Config config) {
        int size = config.imageSize();
        int pixelsCount = size * size;
        float[] data = new float[pixelsCount * 3];
        int[] pixels = image.getRGB(0, 0, size, size, null, 0, size);
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            float r = (rgb >> 16) & 0xFF;
            float g = (rgb >> 8) & 0xFF;
            float b = rgb & 0xFF;
            float[] ordered = config.channelOrder() == ChannelOrder.BGR
                    ? new float[]{b, g, r}
                    : new float[]{r, g, b};

            ordered[0] = ordered[0] - config.mean0();
            ordered[1] = ordered[1] - config.mean1();
            ordered[2] = ordered[2] - config.mean2();

            if (config.layout() == InputLayout.NHWC) {
                int base = i * 3;
                data[base] = ordered[0];
                data[base + 1] = ordered[1];
                data[base + 2] = ordered[2];
            } else {
                data[i] = ordered[0];
                data[i + pixelsCount] = ordered[1];
                data[i + (pixelsCount * 2)] = ordered[2];
            }
        }
        return data;
    }

    private static Double extractScore(Object value) {
        if (value instanceof float[][] output) {
            if (output.length == 0)
                return null;

            if (output[0].length == 1)
                return (double) output[0][0];

            if (output[0].length >= 2)
                return (double) output[0][1];
        } else if (value instanceof float[] output) {
            if (output.length == 1)
                return (double) output[0];
            if (output.length >= 2)
                return (double) output[1];
        }

        return null;
    }

    public enum InputLayout {
        NHWC,
        NCHW
    }

    public enum ChannelOrder {
        RGB,
        BGR
    }

    public record Config(int imageSize, InputLayout layout, ChannelOrder channelOrder,
                         float mean0, float mean1, float mean2) {
    }
}
