package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PistonMeta {
    public static final String META_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private final List<PistonMetaVersion> versions = new ArrayList<>();

    public PistonMeta(Path metaPath) {
        try {
            String json = Files.readString(metaPath);
            System.out.println("Piston meta json: " + json);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            if (object.has("versions")) {
                JsonArray versions = object.getAsJsonArray("versions");
                for (JsonElement version : versions) {
                    this.versions.add(Constants.GSON.fromJson(version, PistonMetaVersion.class));
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load piston meta!", exception);
        }
    }

    public static void download(Path path) {
        try (InputStream stream = new URI(META_URL).toURL().openStream()) {
            String json = new String(stream.readAllBytes());
            Files.createDirectories(path.getParent());
            Files.writeString(path, json);

            System.out.println("Piston meta downloaded to: " + path);
        } catch (IOException | URISyntaxException exception) {
            throw new RuntimeException("Failed to download piston meta!", exception);
        }
    }

    public static PistonMeta load(Path path) {
        if (Files.notExists(path)) {
            download(path);
        }

        return new PistonMeta(path);
    }

    public PistonMetaVersion findVersion(String version) {
        for (PistonMetaVersion metaVersion : this.versions) {
            if (metaVersion.id().equals(version)) {
                return metaVersion;
            }
        }

        return null;
    }

    public List<PistonMetaVersion> versions() {
        return this.versions;
    }
}