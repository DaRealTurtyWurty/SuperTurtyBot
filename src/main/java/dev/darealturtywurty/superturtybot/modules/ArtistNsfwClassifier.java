package dev.darealturtywurty.superturtybot.modules;

import ai.onnxruntime.OrtException;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalDouble;

public record ArtistNsfwClassifier(NsfwClassifier classifier) implements AutoCloseable {
    private static final int IMAGE_SIZE = 224;
    private static final float MEAN_B = 104f;
    private static final float MEAN_G = 117f;
    private static final float MEAN_R = 123f;
    private static final String MODEL_PATH_PROPERTY = "artistNsfwModelPath";
    private static volatile Path datasetRoot = Path.of("data/RealAIImages");

    public static Optional<ArtistNsfwClassifier> create() {
        Path modelPath = resolveModelPath();
        if (Files.notExists(modelPath)) {
            Constants.LOGGER.warn("Artist NSFW model not found at {}.", modelPath);
            return Optional.empty();
        }

        try {
            var config = new NsfwClassifier.Config(
                    IMAGE_SIZE,
                    NsfwClassifier.InputLayout.NHWC,
                    NsfwClassifier.ChannelOrder.BGR,
                    MEAN_B,
                    MEAN_G,
                    MEAN_R
            );
            NsfwClassifier classifier = NsfwClassifier.create(modelPath, config);
            return Optional.of(new ArtistNsfwClassifier(classifier));
        } catch (OrtException exception) {
            Constants.LOGGER.error("Failed to initialize artist NSFW classifier.", exception);
            return Optional.empty();
        }
    }

    public OptionalDouble predictScore(Path imagePath) {
        return classifier.predictScore(imagePath);
    }

    @Override
    public void close() {
        classifier.close();
    }

    public static void setDatasetRoot(Path root) {
        if (root == null)
            return;

        datasetRoot = root;
    }

    private static Path resolveModelPath() {
        String override = System.getProperty(MODEL_PATH_PROPERTY);
        if (override != null && !override.isBlank())
            return Path.of(override);

        Path defaultModelPath = datasetRoot.resolve("open_nsfw.onnx");
        if (Files.exists(defaultModelPath))
            return defaultModelPath;

        return datasetRoot.resolve("nsfw_model.onnx");
    }
}
