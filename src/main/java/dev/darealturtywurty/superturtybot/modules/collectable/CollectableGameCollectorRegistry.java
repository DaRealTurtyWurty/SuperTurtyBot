package dev.darealturtywurty.superturtybot.modules.collectable;

import dev.darealturtywurty.superturtybot.modules.collectable.country.CountryCollectable;
import dev.darealturtywurty.superturtybot.modules.collectable.country.CountryCollectableRegistry;
import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobCollectable;
import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobRegistry;
import dev.darealturtywurty.superturtybot.modules.collectable.r6s.RainbowSixOperatorCollectable;
import dev.darealturtywurty.superturtybot.modules.collectable.r6s.RainbowSixOperatorRegistry;
import dev.darealturtywurty.superturtybot.registry.Registry;

public class CollectableGameCollectorRegistry {
    public static final Registry<CollectableGameCollector<? extends Collectable>> COLLECTOR_REGISTRY = new Registry<>();

    @SuppressWarnings("unchecked")
    public static <T extends Collectable> CollectableGameCollector<T> register(String name, CollectableGameCollector<T> collector) {
        return (CollectableGameCollector<T>) COLLECTOR_REGISTRY.register(name, collector);
    }

    public static final CollectableGameCollector<MinecraftMobCollectable> MINECRAFT_MOBS = register("minecraft_mobs", new CollectableGameCollector<>(MinecraftMobRegistry.MOB_REGISTRY, "minecraft_mobs", "Minecraft Mobs"));

    public static final CollectableGameCollector<RainbowSixOperatorCollectable> RAINBOW_SIX_OPERATORS = register("r6_operators", new CollectableGameCollector<>(RainbowSixOperatorRegistry.RAINBOW_SIX_OPERATOR_REGISTRY, "r6_operators", "Rainbow Six Operators"));

    public static final CollectableGameCollector<CountryCollectable> COUNTRIES = register("countries", new CollectableGameCollector<>(CountryCollectableRegistry.COUNTRY_REGISTRY, "countries", "Countries"));
}
