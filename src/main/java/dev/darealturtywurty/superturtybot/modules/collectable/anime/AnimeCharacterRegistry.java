package dev.darealturtywurty.superturtybot.modules.collectable.anime;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.registry.Registry;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AnimeCharacterRegistry {
    public static final Path IMAGE_DIRECTORY = Path.of("data", "anime_characters").toAbsolutePath().normalize();
    public static final Registry<AnimeCharacterCollectable> CHARACTER_REGISTRY = new Registry<>();

    private static final AtomicBoolean LOADED = new AtomicBoolean();

    private AnimeCharacterRegistry() {
    }

    public static void load() {
        if (!LOADED.compareAndSet(false, true))
            return;

        if (!Files.isDirectory(IMAGE_DIRECTORY, LinkOption.NOFOLLOW_LINKS)) {
            Constants.LOGGER.warn("Anime character image directory does not exist: {}", IMAGE_DIRECTORY);
            return;
        }

        Set<String> registeredNames = new HashSet<>();
        try (DirectoryStream<Path> images = Files.newDirectoryStream(IMAGE_DIRECTORY)) {
            for (Path image : images) {
                if (!Files.isRegularFile(image, LinkOption.NOFOLLOW_LINKS) || !isSupportedImage(image))
                    continue;

                String fileName = image.getFileName().toString();
                String stem = fileName.substring(0, fileName.lastIndexOf('.'));
                String name = stem.toLowerCase(Locale.ROOT);
                if (name.isBlank() || !registeredNames.add(name))
                    continue;

                String richName = stem.replace('_', ' ').trim();
                CHARACTER_REGISTRY.register(name, new AnimeCharacterCollectable(name, richName, image));
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to load anime character collectables from {}", IMAGE_DIRECTORY, exception);
        }

        Constants.LOGGER.info("Loaded Anime Character Collectables: {}", CHARACTER_REGISTRY.size());
    }

    private static boolean isSupportedImage(Path image) {
        String fileName = image.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".jpg") || fileName.endsWith(".png");
    }
}
