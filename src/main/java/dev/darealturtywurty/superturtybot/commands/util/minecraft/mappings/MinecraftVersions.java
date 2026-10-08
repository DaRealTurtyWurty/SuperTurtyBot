package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMeta;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMetaVersion;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version.VersionPackage;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MinecraftVersions {
    private static final Cache<String, List<PistonMetaVersion>> VERSIONS = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();
    private static final Cache<PistonMetaVersion, VersionPackage> PACKAGES = CacheBuilder.newBuilder()
        .maximumSize(50)
        .build();

    public static List<String> getPistonVersions() {
        return versions().stream().map(PistonMetaVersion::id).toList();
    }

    public static PistonMetaVersion getVersion(String name) {
        return versions().stream()
            .filter(version -> version.id().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    private static List<PistonMetaVersion> versions() {
        try {
            return VERSIONS.get("manifest", () -> {
                var json = MappingDownloads.json(PistonMeta.META_URL).getAsJsonObject();
                return List
                    .copyOf(Arrays.asList(Constants.GSON.fromJson(json.get("versions"), PistonMetaVersion[].class)));
            });
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to load Minecraft versions", exception.getCause());
        }
    }

    public static VersionPackage getVersionPackage(PistonMetaVersion version) {
        try {
            return PACKAGES.get(version, () -> VersionPackage.fromJson(
                MappingDownloads.json(version.url()).getAsJsonObject()));
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to load Minecraft version " + version.id(), exception.getCause());
        }
    }
}
