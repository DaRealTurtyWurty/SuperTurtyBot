package dev.darealturtywurty.superturtybot.core.util.object;

import dev.darealturtywurty.superturtybot.core.util.FileUtils;
import lombok.Getter;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class PetPetGifCreator {
    private static final int DELAY = 20;
    private static final List<BufferedImage> PET_PET_FRAMES;

    static {
        PET_PET_FRAMES = FileUtils.locateResourceFiles("petpet").stream().map(file -> {
            try {
                return ImageIO.read(file);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read image!", exception);
            }
        }).toList();
    }

    private final Path outputPath;
    private final BufferedImage inputImage;
    private final GifSequenceWriter writer;
    private final CompletableFuture<Path> future = new CompletableFuture<>();

    public PetPetGifCreator(Path outputPath, BufferedImage inputImage) {
        this.outputPath = outputPath;
        this.inputImage = inputImage;

        try {
            this.writer = new GifSequenceWriter(new FileImageOutputStream(outputPath.toFile()), BufferedImage.TYPE_INT_ARGB, 50, true);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create GifSequenceWriter!", exception);
        }
    }

    public CompletableFuture<Path> start() {
        new Thread(() -> {
            for (int index = 0; index < PET_PET_FRAMES.size() * 2; index++) {
                BufferedImage frame = PET_PET_FRAMES.get(index % PET_PET_FRAMES.size());

                BufferedImage image = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                int j = index < PET_PET_FRAMES.size() ? index : PET_PET_FRAMES.size() - (index % PET_PET_FRAMES.size()) - 1;
                float widthMultiplier = 0.8f + j * 0.02f;
                float heightMultiplier = 0.8f - j * 0.05f;
                float offsetX = (1 - widthMultiplier) * 0.5f + 0.1f;
                float offsetY = (1 - heightMultiplier) - 0.08f;

                int width = (int) (inputImage.getWidth() * widthMultiplier);
                int height = (int) (inputImage.getHeight() * heightMultiplier);
                int x = (int) (inputImage.getWidth() * offsetX);
                int y = (int) (inputImage.getHeight() * offsetY);

                Graphics2D graphics = image.createGraphics();
                graphics.drawImage(inputImage, x, y, width, height, null);
                graphics.drawImage(frame, 0, 0, inputImage.getWidth(), inputImage.getHeight(), null);

                try {
                    this.writer.writeToSequence(image);
                } catch (IOException exception) {
                    throw new IllegalStateException("Could not write image!", exception);
                }

                graphics.dispose();
            }

            try {
                this.writer.close();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not close writer!", exception);
            }

            this.future.complete(this.outputPath);
        }).start();

        return this.future;
    }
}
