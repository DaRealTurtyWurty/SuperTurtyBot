package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.intermediary;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingDownloads;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingFiles;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class IntermediaryMappings {
    private IntermediaryMappings() {
    }

    public static MemoryMappingTree load(String version) throws IOException {
        String encodedVersion = MappingFiles.encode(version);
        var versions = MappingDownloads.json("https://meta.fabricmc.net/v2/versions/intermediary/" + encodedVersion)
            .getAsJsonArray();
        if (versions.isEmpty())
            throw new IllegalArgumentException(
                "Intermediary mappings are not available for Minecraft " + version + ".");

        Path jar = MappingDownloads.cached("https://maven.fabricmc.net/net/fabricmc/intermediary/"
            + encodedVersion + "/intermediary-" + encodedVersion + "-v2.jar",
            Path.of("versions", version, "intermediary-v2.jar"), null);
        var tree = MappingFiles.readTiny(jar, Map.of());
        if (!"official".equals(tree.getSrcNamespace()))
            throw new IllegalArgumentException(
                "This Minecraft version uses an unsupported Intermediary namespace layout.");

        return tree;
    }
}
