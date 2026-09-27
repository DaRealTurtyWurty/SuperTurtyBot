package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mcp;

import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.apache.commons.csv.CSVFormat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public record McpMappings(MemoryMappingTree tree, String build) {
    public static McpMappings load(String version) throws IOException {
        String mappingVersion = switch (version) {
            case "1.12.1", "1.12.2" -> "1.12";
            case "1.11.2" -> "1.11";
            default -> version;
        };
        String suffix = "-" + mappingVersion;
        String build = ForgeMaven.versions("mcp_stable").stream()
                .filter(candidate -> candidate.endsWith(suffix))
                .max(Comparator.comparingInt(candidate -> Integer.parseInt(candidate.substring(0, candidate.indexOf('-')))))
                .orElse(null);
        String artifact = "mcp_stable";
        if (build == null) {
            artifact = "mcp_snapshot";
            build = ForgeMaven.versions(artifact).stream()
                    .filter(candidate -> candidate.endsWith(suffix))
                    .max(Comparator.naturalOrder())
                    .orElseThrow(() -> new IllegalArgumentException("MCP names are not available for Minecraft " + version
                            + ". Try SRG or Mojmap instead."));
        }

        Path archive = ForgeMaven.download(artifact, build, "", Path.of("versions", version));
        var tree = SrgMappings.load(version);
        try (var zip = new ZipFile(archive.toFile())) {
            applyNames(tree, readNames(zip, "fields.csv"), readNames(zip, "methods.csv"));
        }

        return new McpMappings(tree, artifact + ":" + build);
    }

    public static void applyNames(MemoryMappingTree tree, Map<String, String> fields, Map<String, String> methods) {
        int srg = tree.getNamespaceId("srg");
        var namespaces = new ArrayList<>(tree.getDstNamespaces());
        namespaces.add("mcp");
        tree.setDstNamespaces(namespaces);
        int mcp = tree.getNamespaceId("mcp");
        for (var owner : tree.getClasses()) {
            owner.setDstName(owner.getName(srg), mcp);
            for (var field : owner.getFields()) {
                String name = field.getName(srg);
                field.setDstName(fields.getOrDefault(name, name), mcp);
            }

            for (var method : owner.getMethods()) {
                String name = method.getName(srg);
                method.setDstName(methods.getOrDefault(name, name), mcp);
            }
        }
    }

    private static Map<String, String> readNames(ZipFile zip, String name) throws IOException {
        var entry = zip.getEntry(name);
        if (entry == null)
            throw new IOException("Missing " + name + " in " + zip.getName());

        var names = new HashMap<String, String>();
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8);
             var records = format.parse(reader)) {
            for (var record : records) {
                names.put(record.get("searge"), record.get("name"));
            }
        }

        return names;
    }
}
