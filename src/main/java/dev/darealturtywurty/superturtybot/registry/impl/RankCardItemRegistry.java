package dev.darealturtywurty.superturtybot.registry.impl;

import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Rarity;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Type;
import dev.darealturtywurty.superturtybot.registry.Registerer;
import dev.darealturtywurty.superturtybot.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Map.Entry;

public class RankCardItemRegistry {
    public static final Registry<RankCardItem> RANK_CARD_ITEMS = new Registry<>();

    static {
        Object[] catalogues = {AvatarOutlines.COBALT_METAL, Backgrounds.CHERRY_PANELS, EmptyXPBars.WEATHERED_COPPER,
            FilledXPBars.COBALT_STEEL, Outlines.MOSS_FRAME, XPBarOutlines.PEBBLE_EDGE};
    }

    @Nullable
    public static RankCardItem resolveImage(Type type, String data) {
        if (data == null || data.isBlank())
            return null;
        RankCardItem item = RANK_CARD_ITEMS.get(data);
        return item != null && item.type == type ? item : null;
    }

    @Nullable
    public static RankCardItem locate(String name, String data, Type type, Rarity rarity) {
        return RANK_CARD_ITEMS.getRegistry().entrySet().stream().filter(entry -> {
            final RankCardItem item = entry.getValue();
            return entry.getKey().equals(name) && item.data.equalsIgnoreCase(data) && item.type == type
                && item.rarity == rarity;
        }).map(Entry::getValue).findFirst().orElse(null);
    }

    @Registerer
    public static final class AvatarOutlines {
        public static final RankCardItem COBALT_METAL = RANK_CARD_ITEMS.register("cobalt_metal",
            new RankCardItem("cobalt_metal", Type.AVATAR_OUTLINE_IMAGE, Rarity.UNCOMMON));

        public static final RankCardItem RED_LEATHER = RANK_CARD_ITEMS.register("red_leather",
            new RankCardItem("red_leather", Type.AVATAR_OUTLINE_IMAGE, Rarity.UNCOMMON));

        public static final RankCardItem CHERRY_WOOD = RANK_CARD_ITEMS.register("cherry_wood",
            new RankCardItem("cherry_wood", Type.AVATAR_OUTLINE_IMAGE, Rarity.COMMON));

        public static final RankCardItem FOREST_MOSS = RANK_CARD_ITEMS.register("forest_moss",
            new RankCardItem("forest_moss", Type.AVATAR_OUTLINE_IMAGE, Rarity.COMMON));

        public static final RankCardItem IVORY_MARBLE = RANK_CARD_ITEMS.register("ivory_marble",
            new RankCardItem("ivory_marble", Type.AVATAR_OUTLINE_IMAGE, Rarity.UNCOMMON));
    }

    @Registerer
    public static final class Backgrounds {
        public static final RankCardItem MINECRAFT_OVERWORLD = RANK_CARD_ITEMS.register("minecraft_overworld",
            new RankCardItem("minecraft_overworld", Type.BACKGROUND_IMAGE, Rarity.LEGENDARY));
        public static final RankCardItem FORTNITE_ISLAND = RANK_CARD_ITEMS.register("fortnite_island",
            new RankCardItem("fortnite_island", Type.BACKGROUND_IMAGE, Rarity.LEGENDARY));
        public static final RankCardItem ROCKET_LEAGUE_ARENA = RANK_CARD_ITEMS.register("rocket_league_arena",
            new RankCardItem("rocket_league_arena", Type.BACKGROUND_IMAGE, Rarity.EPIC));

        public static final RankCardItem BANANAS = RANK_CARD_ITEMS.register("bananas",
            new RankCardItem("bananas", Type.BACKGROUND_IMAGE, Rarity.RARE));

        public static final RankCardItem BAKED_BEANS = RANK_CARD_ITEMS.register("baked_beans",
            new RankCardItem("baked_beans", Type.BACKGROUND_IMAGE, Rarity.RARE));

        public static final RankCardItem GINGER_CAT = RANK_CARD_ITEMS.register("ginger_cat",
            new RankCardItem("ginger_cat", Type.BACKGROUND_IMAGE, Rarity.RARE));

        public static final RankCardItem CHERRY_PANELS = RANK_CARD_ITEMS.register("cherry_panels",
            new RankCardItem("cherry_panels", Type.BACKGROUND_IMAGE, Rarity.COMMON));

        public static final RankCardItem MIDNIGHT_SLATE = RANK_CARD_ITEMS.register("midnight_slate",
            new RankCardItem("midnight_slate", Type.BACKGROUND_IMAGE, Rarity.COMMON));

        public static final RankCardItem FOREST_FLOOR = RANK_CARD_ITEMS.register("forest_floor",
            new RankCardItem("forest_floor", Type.BACKGROUND_IMAGE, Rarity.COMMON));

        public static final RankCardItem WOVEN_DENIM = RANK_CARD_ITEMS.register("woven_denim",
            new RankCardItem("woven_denim", Type.BACKGROUND_IMAGE, Rarity.COMMON));

        public static final RankCardItem MARBLE_TILES = RANK_CARD_ITEMS.register("marble_tiles",
            new RankCardItem("marble_tiles", Type.BACKGROUND_IMAGE, Rarity.UNCOMMON));
    }

    @Registerer
    public static final class EmptyXPBars {
        public static final RankCardItem WEATHERED_COPPER = RANK_CARD_ITEMS.register("weathered_copper",
            new RankCardItem("weathered_copper", Type.XP_EMPTY_IMAGE, Rarity.COMMON));

        public static final RankCardItem DARK_OAK = RANK_CARD_ITEMS.register("dark_oak",
            new RankCardItem("dark_oak", Type.XP_EMPTY_IMAGE, Rarity.COMMON));

        public static final RankCardItem GOLDEN_SAND = RANK_CARD_ITEMS.register("golden_sand",
            new RankCardItem("golden_sand", Type.XP_EMPTY_IMAGE, Rarity.COMMON));

        public static final RankCardItem MOSSY_STONE = RANK_CARD_ITEMS.register("mossy_stone",
            new RankCardItem("mossy_stone", Type.XP_EMPTY_IMAGE, Rarity.COMMON));

        public static final RankCardItem RIVER_PEBBLES = RANK_CARD_ITEMS.register("river_pebbles",
            new RankCardItem("river_pebbles", Type.XP_EMPTY_IMAGE, Rarity.COMMON));
    }

    @Registerer
    public static final class FilledXPBars {
        public static final RankCardItem COBALT_STEEL = RANK_CARD_ITEMS.register("cobalt_steel",
            new RankCardItem("cobalt_steel", Type.XP_FILL_IMAGE, Rarity.UNCOMMON));

        public static final RankCardItem OAK_PLANKS = RANK_CARD_ITEMS.register("oak_planks",
            new RankCardItem("oak_planks", Type.XP_FILL_IMAGE, Rarity.COMMON));

        public static final RankCardItem PALE_MARBLE = RANK_CARD_ITEMS.register("pale_marble",
            new RankCardItem("pale_marble", Type.XP_FILL_IMAGE, Rarity.UNCOMMON));

        public static final RankCardItem RED_COPPER = RANK_CARD_ITEMS.register("red_copper",
            new RankCardItem("red_copper", Type.XP_FILL_IMAGE, Rarity.UNCOMMON));

        public static final RankCardItem POLISHED_CHERRY = RANK_CARD_ITEMS.register("polished_cherry",
            new RankCardItem("polished_cherry", Type.XP_FILL_IMAGE, Rarity.UNCOMMON));
    }

    @Registerer
    public static final class Outlines {
        public static final RankCardItem LEATHER_FRAME = RANK_CARD_ITEMS.register("leather_frame",
            new RankCardItem("leather_frame", Type.OUTLINE_IMAGE, Rarity.COMMON));
        public static final RankCardItem MOSS_FRAME = RANK_CARD_ITEMS.register("moss_frame",
            new RankCardItem("moss_frame", Type.OUTLINE_IMAGE, Rarity.COMMON));

        public static final RankCardItem WOOD_FRAME = RANK_CARD_ITEMS.register("wood_frame",
            new RankCardItem("wood_frame", Type.OUTLINE_IMAGE, Rarity.COMMON));
    }

    @Registerer
    public static final class XPBarOutlines {
        public static final RankCardItem PEBBLE_EDGE = RANK_CARD_ITEMS.register("pebble_edge",
            new RankCardItem("pebble_edge", Type.XP_OUTLINE_IMAGE, Rarity.COMMON));

        public static final RankCardItem MARBLE_EDGE = RANK_CARD_ITEMS.register("marble_edge",
            new RankCardItem("marble_edge", Type.XP_OUTLINE_IMAGE, Rarity.UNCOMMON));
    }
}
