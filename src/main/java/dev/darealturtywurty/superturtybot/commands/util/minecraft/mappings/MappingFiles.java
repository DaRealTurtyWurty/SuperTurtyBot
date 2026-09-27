package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipFile;

public final class MappingFiles {
    private MappingFiles() {
    }

    public static MemoryMappingTree readTiny(Path path, Map<String, String> namespaces) throws IOException {
        var tree = new MemoryMappingTree(true);
        try (var zip = new ZipFile(path.toFile())) {
            var entry = zip.getEntry("mappings/mappings.tiny");
            if (entry == null)
                throw new IOException("Missing mappings/mappings.tiny in " + path);

            try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                MappingReader.read(reader, new MappingNsRenamer(tree, namespaces));
            }
        }

        return tree;
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
