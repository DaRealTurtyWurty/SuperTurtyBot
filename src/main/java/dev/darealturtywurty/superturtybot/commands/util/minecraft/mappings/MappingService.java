package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.intermediary.IntermediaryMappings;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mcp.McpMappings;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mcp.SrgMappings;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap.MojmapMappings;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.yarn.YarnMappings;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MappingService {
    public static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final Cache<Request, CompletableFuture<MappingData>> CACHE = CacheBuilder.newBuilder()
            .maximumSize(8)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    private MappingService() {
    }

    public static CompletableFuture<MappingData> get(String version, String side, MappingChannel from, MappingChannel to) {
        var request = new Request(version, side, Set.copyOf(EnumSet.of(from, to)));
        CompletableFuture<MappingData> future = CACHE.asMap().computeIfAbsent(request,
                key -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return load(key);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to download mappings", exception);
                    }
                }, EXECUTOR));
        future.whenComplete((result, error) -> {
            if (error != null) {
                CACHE.asMap().remove(request, future);
            }
        });
        return future;
    }

    private static MappingData load(Request request) throws IOException {
        if (!request.side().equals("client") && !request.side().equals("server"))
            throw new IllegalArgumentException("Choose client or server mappings.");

        var version = MinecraftVersions.getVersion(request.version());
        if (version == null)
            throw new IllegalArgumentException("Unknown Minecraft version. Choose a version from autocomplete.");

        var tree = new MemoryMappingTree(true);
        var builds = new EnumMap<MappingChannel, String>(MappingChannel.class);
        if (request.channels().contains(MappingChannel.YARN)) {
            var yarn = YarnMappings.load(version.id());
            yarn.tree().accept(tree);
            builds.put(MappingChannel.YARN, yarn.build());
        } else if (request.channels().contains(MappingChannel.INTERMEDIARY)) {
            IntermediaryMappings.load(version.id()).accept(tree);
        }

        if (request.channels().contains(MappingChannel.MCP)) {
            var mcp = McpMappings.load(version.id());
            mcp.tree().accept(tree);
            builds.put(MappingChannel.MCP, mcp.build());
        } else if (request.channels().contains(MappingChannel.SRG)) {
            SrgMappings.load(version.id()).accept(tree);
        }

        var sideMappings = MojmapMappings.loadIfAvailable(version, request.side());
        if (sideMappings == null && (request.channels().contains(MappingChannel.MOJMAP)
                || tree.getClasses().isEmpty() || request.side().equals("server"))) {
            throw new IllegalArgumentException("Mojmap " + request.side() + " mappings are not available for Minecraft " + version.id() + ".");
        }

        if (sideMappings != null) {
            sideMappings.accept(tree);
            retainSide(tree, sideMappings);
        }

        // Older releases have merged mappings but no Mojmap data to identify each side.
        return new MappingData(tree, version.id(), sideMappings == null ? "merged" : request.side(), builds);
    }

    private static void retainSide(MemoryMappingTree tree, MemoryMappingTree sideMappings) {
        for (var owner : List.copyOf(tree.getClasses())) {
            var sideClass = sideMappings.getClass(owner.getSrcName());
            if (sideClass == null) {
                tree.removeClass(owner.getSrcName());
                continue;
            }

            for (var method : List.copyOf(owner.getMethods())) {
                if (sideClass.getMethod(method.getSrcName(), method.getSrcDesc()) == null) {
                    owner.removeMethod(method.getSrcName(), method.getSrcDesc());
                }
            }

            for (var field : List.copyOf(owner.getFields())) {
                if (sideClass.getField(field.getSrcName(), field.getSrcDesc()) == null) {
                    owner.removeField(field.getSrcName(), field.getSrcDesc());
                }
            }
        }
    }

    private record Request(String version, String side, Set<MappingChannel> channels) {
    }
}
