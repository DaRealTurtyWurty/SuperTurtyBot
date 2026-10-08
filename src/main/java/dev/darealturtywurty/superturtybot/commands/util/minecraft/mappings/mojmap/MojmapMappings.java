package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingDownloads;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MinecraftVersions;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMetaVersion;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MojmapMappings {
    private MojmapMappings() {
    }

    public static @Nullable MemoryMappingTree loadIfAvailable(PistonMetaVersion version, String side)
        throws IOException {
        var versionPackage = MinecraftVersions.getVersionPackage(version);
        var download = switch (side) {
            case "client" -> versionPackage.downloads().clientMappings();
            case "server" -> versionPackage.downloads().serverMappings();
            default -> throw new IllegalArgumentException("Choose client or server mappings.");
        };
        if (download == null)
            return null;

        Path path = MappingDownloads.cached(download.url(),
            Path.of("versions", version.id(), "mojmap-" + side + ".txt"), download.sha1());
        var tree = new MemoryMappingTree(true);
        try (var reader = Files.newBufferedReader(path)) {
            ProGuardFileReader.read(reader, "mojmap", "official", new MappingSourceNsSwitch(tree, "official"));
        }

        return tree;
    }
}
