package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mcp;

import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.format.srg.SrgFileReader;
import net.fabricmc.mappingio.format.srg.TsrgFileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipFile;

public final class SrgMappings {
    private SrgMappings() {
    }

    public static MemoryMappingTree load(String version) throws IOException {
        var builds = ForgeMaven.versions("mcp_config");
        String build = builds.contains(version)
            ? version
            : builds.stream()
                .filter(candidate -> candidate.startsWith(version + "-"))
                .max(Comparator.naturalOrder())
                .orElse(null);
        boolean legacy = build == null;
        if (legacy && !ForgeMaven.versions("mcp").contains(version))
            throw new IllegalArgumentException("SRG mappings are not available for Minecraft " + version + ".");

        Path archive = legacy
            ? ForgeMaven.download("mcp", version, "-srg", Path.of("versions", version))
            : ForgeMaven.download("mcp_config", build, "", Path.of("versions", version));
        var tree = new MemoryMappingTree(true);
        try (var zip = new ZipFile(archive.toFile())) {
            var entry = zip.getEntry(legacy ? "joined.srg" : "config/joined.tsrg");
            if (entry == null)
                throw new IOException("Missing SRG mappings in " + archive);

            try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                if (legacy) {
                    SrgFileReader.read(reader, "official", "srg", tree);
                } else {
                    TsrgFileReader.read(reader, "official", "srg",
                        new MappingNsRenamer(tree, Map.of("obf", "official")));
                }
            }
        }

        if (!"official".equals(tree.getSrcNamespace()) || tree.getNamespaceId("srg") < 0)
            throw new IOException("Unsupported SRG namespace layout for Minecraft " + version);

        return tree;
    }
}
