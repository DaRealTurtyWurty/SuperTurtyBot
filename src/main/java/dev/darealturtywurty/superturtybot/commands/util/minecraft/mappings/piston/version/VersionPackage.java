package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMetaVersion;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record VersionPackage(Arguments arguments, AssetIndex assetIndex, String assets, int complianceLevel,
                             Downloads downloads, String id, JavaVersion javaVersion, List<Library> libraries,
                             Logging logging, String mainClass, int minimumLauncherVersion, String releaseTime,
                             String time, String type) {
    public static VersionPackage fromJson(JsonObject json) {
        Arguments arguments;
        try {
            JsonObject argumentsJson = json.getAsJsonObject("arguments");
            JsonArray gameJson = argumentsJson.getAsJsonArray("game");
            JsonArray jvmJson = json.getAsJsonArray("jvm");
            CLIArguments gameArguments = CLIArguments.fromJsonArray(gameJson);
            CLIArguments jvmArguments = CLIArguments.fromJsonArray(jvmJson);
            arguments = new Arguments(gameArguments, jvmArguments);
        } catch (Exception exception) {
            arguments = new Arguments(new CLIArguments(List.of()), new CLIArguments(List.of()));
        }

        JsonObject assetIndexJson = json.getAsJsonObject("assetIndex");
        AssetIndex assetIndex = AssetIndex.fromJson(assetIndexJson);

        String assets = json.get("assets").getAsString();
        int complianceLevel = json.has("complianceLevel") ? json.get("complianceLevel").getAsInt() : -1;

        JsonObject downloadsJson = json.getAsJsonObject("downloads");
        Downloads downloads = Downloads.fromJson(downloadsJson);

        String id = json.get("id").getAsString();

        JsonObject javaVersionJson = json.getAsJsonObject("javaVersion");
        JavaVersion javaVersion = JavaVersion.fromJson(javaVersionJson);

        JsonArray librariesJson = json.getAsJsonArray("libraries");
        List<Library> libraries = Library.fromJsonArray(librariesJson);

        JsonObject loggingJson = json.getAsJsonObject("logging");
        Logging logging = Logging.fromJson(loggingJson);

        String mainClass = json.get("mainClass").getAsString();
        int minimumLauncherVersion = json.get("minimumLauncherVersion").getAsInt();
        String releaseTime = json.get("releaseTime").getAsString();
        String time = json.get("time").getAsString();
        String type = json.get("type").getAsString();

        return new VersionPackage(arguments, assetIndex, assets, complianceLevel, downloads, id, javaVersion,
                libraries, logging, mainClass, minimumLauncherVersion, releaseTime, time, type);
    }

    public static VersionPackage fromPath(Path path) {
        Path versionJsonPath = path.toAbsolutePath();
        if (Files.notExists(versionJsonPath))
            return null;

        VersionPackage versionPackage;
        try {
            String jsonStr = Files.readString(versionJsonPath);
            JsonObject json = Constants.GSON.fromJson(jsonStr, JsonObject.class);
            versionPackage = VersionPackage.fromJson(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to download client to path %s".formatted(path), exception);
        }

        return versionPackage;
    }

    private static final Map<PistonMetaVersion, Path> PATH_CACHE = new ConcurrentHashMap<>();

    public static void download(PistonMetaVersion metaVersion, Path outputFile) {
        if(PATH_CACHE.containsKey(metaVersion))
            return;

        String url = metaVersion.url();
        try (InputStream inputStream = new URI(url).toURL().openStream()) {
            Files.createDirectories(Files.isDirectory(outputFile) ? outputFile : outputFile.getParent());
            Files.write(outputFile, inputStream.readAllBytes());
        } catch (IOException | URISyntaxException exception) {
            throw new RuntimeException("Failed to download version!", exception);
        }
    }

    public static Path getOrDownload(PistonMetaVersion metaVersion) {
        return PATH_CACHE.computeIfAbsent(metaVersion, version -> {
            Path path = Path.of("versions", version.id() + ".json");
            if(Files.exists(path))
                return path;

            download(version, path);
            return path;
        });
    }

    public static VersionPackage load(PistonMetaVersion metaVersion, Path outputFile) {
        if (Files.notExists(outputFile)) {
            download(metaVersion, outputFile);
        }

        return fromPath(outputFile);
    }
}