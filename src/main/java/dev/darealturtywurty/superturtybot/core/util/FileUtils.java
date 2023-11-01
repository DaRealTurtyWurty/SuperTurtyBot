package dev.darealturtywurty.superturtybot.core.util;

import com.google.common.collect.Streams;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    public static List<URL> locateResourceFiles(String folder) {
        if(CommandHook.isDevMode()) {
            Path path = Path.of("src/main/resources/" + folder);
            return FileUtils.locateResourceFiles(path);
        } else {
            try {
                return Streams.stream(TurtyBot.class.getClassLoader().getResources(folder).asIterator())
                        .toList();
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to locate resource files!", exception);
                return List.of();
            }
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
