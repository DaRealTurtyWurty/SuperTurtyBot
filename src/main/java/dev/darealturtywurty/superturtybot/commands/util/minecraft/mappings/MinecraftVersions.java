package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMeta;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMetaVersion;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version.VersionPackage;
import dev.darealturtywurty.superturtybot.core.util.DefaultLoaderCache;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MinecraftVersions {
    private static final DefaultLoaderCache<PistonMeta> PISTON_META_CACHE = new DefaultLoaderCache<>(() -> {
        Path path = Path.of("./piston_meta.json");
        PistonMeta.download(path);
        return PistonMeta.load(path);
    });

    private static final Cache<PistonMetaVersion, VersionPackage> VERSION_PACKAGE_CACHE =
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .build();

    public static List<String> getPistonVersions() {
        PistonMeta pistonMeta = PISTON_META_CACHE.get();
        if (pistonMeta == null) {
            return List.of();
        }

        List<String> choices = new ArrayList<>();
        for (PistonMetaVersion version : pistonMeta.versions()) {
            VersionPackage versionPackage = VERSION_PACKAGE_CACHE.getIfPresent(version);
            if (versionPackage == null) {
                Path path = VersionPackage.getOrDownload(version);
                versionPackage = VersionPackage.fromPath(path);
                if (versionPackage == null) {
                    continue;
                }

                VERSION_PACKAGE_CACHE.put(version, versionPackage);
            }

            choices.add(versionPackage.id());
        }

        return choices;
    }

    public static PistonMetaVersion getVersion(String versionString) {
        PistonMeta pistonMeta = PISTON_META_CACHE.get();
        if (pistonMeta == null)
            return null;

        for (PistonMetaVersion version : pistonMeta.versions()) {
            VersionPackage versionPackage = VERSION_PACKAGE_CACHE.getIfPresent(version);
            if (versionPackage == null) {
                Path path = VersionPackage.getOrDownload(version);
                versionPackage = VersionPackage.fromPath(path);
                if (versionPackage == null) {
                    continue;
                }

                VERSION_PACKAGE_CACHE.put(version, versionPackage);
            }

            if (versionPackage.id().equalsIgnoreCase(versionString)) {
                return version;
            }
        }

        return null;
    }

    public static VersionPackage getVersionPackage(PistonMetaVersion version) {
        VersionPackage versionPackage = VERSION_PACKAGE_CACHE.getIfPresent(version);
        if (versionPackage == null) {
            Path path = VersionPackage.getOrDownload(version);
            versionPackage = VersionPackage.fromPath(path);
            if (versionPackage == null) {
                return null;
            }

            VERSION_PACKAGE_CACHE.put(version, versionPackage);
        }

        return versionPackage;
    }
}
