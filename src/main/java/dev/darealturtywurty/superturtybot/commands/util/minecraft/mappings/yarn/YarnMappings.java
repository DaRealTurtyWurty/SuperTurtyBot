package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.yarn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingDownloads;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingFiles;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.intermediary.IntermediaryMappings;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public record YarnMappings(MemoryMappingTree tree, String build) {
    public static YarnMappings load(String version) throws IOException {
        var builds = MappingDownloads.json("https://meta.fabricmc.net/v2/versions/yarn/" + MappingFiles.encode(version))
            .getAsJsonArray();
        JsonObject latest = builds.asList().stream().map(JsonElement::getAsJsonObject)
            .max(Comparator.comparingInt(build -> build.get("build").getAsInt()))
            .orElseThrow(
                () -> new IllegalArgumentException("Yarn mappings are not available for Minecraft " + version + "."));
        String build = latest.get("version").getAsString();
        String encodedBuild = MappingFiles.encode(build);
        Path jar = MappingDownloads.cached("https://maven.fabricmc.net/net/fabricmc/yarn/"
            + encodedBuild + "/yarn-" + encodedBuild + "-v2.jar",
            Path.of("versions", version, "yarn-" + build + "-v2.jar"), null);
        var tree = IntermediaryMappings.load(version);
        MappingFiles.readTiny(jar, Map.of("named", "yarn")).accept(tree);
        return new YarnMappings(tree, build);
    }
}
