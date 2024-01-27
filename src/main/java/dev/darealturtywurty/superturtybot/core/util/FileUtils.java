package dev.darealturtywurty.superturtybot.core.util;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    public static List<URL> locateResourceFiles(String folder) {
        if(CommandHook.isDevMode()) {
            Path path = Path.of("src/main/resources/" + folder);
            return FileUtils.locateResourceFiles(path);
        } else {
            URL url = TurtyBot.getResource(folder);
            if (url == null)
                return List.of();

            var fileFolder = new File(url.getFile());
            if(!fileFolder.isDirectory())
                return List.of();

            File[] files = fileFolder.listFiles();
            if(files == null)
                return List.of();

            return Arrays.stream(files).map(file -> {
                try {
                    return file.toURI().toURL();
                } catch (final IOException exception) {
                    Constants.LOGGER.error("Failed to locate resource files!", exception);
                    return null;
                }
            }).toList();
        }
    }

    private static List<URL> locateResourceFiles(Path path) {
        List<URL> files;
        try (Stream<Path> paths = Files.list(path)) {
            files = paths.map(Path::toFile).map(file -> {
                try {
                    return file.toURI().toURL();
                } catch (final IOException exception) {
                    Constants.LOGGER.error("Failed to locate resource files!", exception);
                    return null;
                }
            }).toList();
        } catch (final IOException exception) {
            Constants.LOGGER.error("Failed to locate resource files!", exception);
            return List.of();
        }

        return files;
    }
}
