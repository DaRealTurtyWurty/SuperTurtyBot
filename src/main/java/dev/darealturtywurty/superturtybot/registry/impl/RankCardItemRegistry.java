package dev.darealturtywurty.superturtybot.registry.impl;

import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Rarity;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Type;
import dev.darealturtywurty.superturtybot.registry.Registerer;
import dev.darealturtywurty.superturtybot.registry.Registry;

public class RankCardItemRegistry {
    public static final Registry<RankCardItem> RANK_CARD_ITEMS = new Registry<>();
    
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
        public static final RankCardItem BEANS = RANK_CARD_ITEMS.register("beans",
            new RankCardItem("beans", Type.AVATAR_OUTLINE_IMAGE, Rarity.LEGENDARY));
        
        public static final RankCardItem JELLY_BEANS = RANK_CARD_ITEMS.register("jelly_beans",
            new RankCardItem("jelly_beans", Type.AVATAR_OUTLINE_IMAGE, Rarity.UNCOMMON));
        
        public static final RankCardItem FRUIT = RANK_CARD_ITEMS.register("fruit",
            new RankCardItem("fruit", Type.AVATAR_OUTLINE_IMAGE, Rarity.COMMON));
        
        public static final RankCardItem GRASS = RANK_CARD_ITEMS.register("grass",
            new RankCardItem("grass", Type.AVATAR_OUTLINE_IMAGE, Rarity.RARE));
        
        public static final RankCardItem ICE = RANK_CARD_ITEMS.register("ice",
            new RankCardItem("ice", Type.AVATAR_OUTLINE_IMAGE, Rarity.EPIC));
    }
    
    @Registerer
    public static final class Backgrounds {
        public static final RankCardItem RICK_ROLL = RANK_CARD_ITEMS.register("rick_roll",
            new RankCardItem("rick_astley", Type.BACKGROUND_IMAGE, Rarity.EPIC));
        
        public static final RankCardItem AMONG_US = RANK_CARD_ITEMS.register("among_us",
            new RankCardItem("among_us", Type.BACKGROUND_IMAGE, Rarity.COMMON));
        
        public static final RankCardItem BABY_TURTLES = RANK_CARD_ITEMS.register("baby_turtles",
            new RankCardItem("baby_turtles", Type.BACKGROUND_IMAGE, Rarity.LEGENDARY));
        
        public static final RankCardItem TURTLE = RANK_CARD_ITEMS.register("turtle",
            new RankCardItem("turtle", Type.BACKGROUND_IMAGE, Rarity.UNCOMMON));
        
        public static final RankCardItem CATS = RANK_CARD_ITEMS.register("cats",
            new RankCardItem("cats", Type.BACKGROUND_IMAGE, Rarity.RARE));
    }
    
    @Registerer
    public static final class EmptyXPBars {
        public static final RankCardItem LAVA = RANK_CARD_ITEMS.register("lava",
            new RankCardItem("lava", Type.XP_EMPTY_IMAGE, Rarity.UNCOMMON));
        
        public static final RankCardItem MINECRAFT = RANK_CARD_ITEMS.register("minecraft_cave",
            new RankCardItem("minecraft", Type.XP_EMPTY_IMAGE, Rarity.RARE));
        
        public static final RankCardItem DESERT = RANK_CARD_ITEMS.register("desert",
            new RankCardItem("desert", Type.XP_EMPTY_IMAGE, Rarity.COMMON));
        
        public static final RankCardItem PLAINS = RANK_CARD_ITEMS.register("plains",
            new RankCardItem("grass_plains", Type.XP_EMPTY_IMAGE, Rarity.EPIC));
        
        public static final RankCardItem LAKE = RANK_CARD_ITEMS.register("lake",
            new RankCardItem("lake", Type.XP_EMPTY_IMAGE, Rarity.LEGENDARY));
    }
    
    @Registerer
    public static final class FilledXPBars {
        public static final RankCardItem WATER = RANK_CARD_ITEMS.register("water",
            new RankCardItem("water", Type.XP_FILL_IMAGE, Rarity.UNCOMMON));
        
        public static final RankCardItem MINECRAFT = RANK_CARD_ITEMS.register("minecraft_surface",
            new RankCardItem("minecraft", Type.XP_FILL_IMAGE, Rarity.RARE));
        
        public static final RankCardItem ICE_CAVE = RANK_CARD_ITEMS.register("ice_cave",
            new RankCardItem("ice_cave", Type.XP_FILL_IMAGE, Rarity.COMMON));
        
        public static final RankCardItem MOUNTAIN = RANK_CARD_ITEMS.register("mountain",
            new RankCardItem("mountain", Type.XP_FILL_IMAGE, Rarity.EPIC));
        
        public static final RankCardItem MOUNTAIN_MEADOW = RANK_CARD_ITEMS.register("mountain_meadow",
            new RankCardItem("mountain_meadow", Type.XP_FILL_IMAGE, Rarity.LEGENDARY));
    }
    
    @Registerer
    public static final class Outlines {
        public static final RankCardItem SEAWEED = RANK_CARD_ITEMS.register("seaweed",
            new RankCardItem("seaweed", Type.OUTLINE_IMAGE, Rarity.RARE));
        
        public static final RankCardItem FOREST = RANK_CARD_ITEMS.register("forest",
            new RankCardItem("forest", Type.OUTLINE_IMAGE, Rarity.UNCOMMON));
    }
    
    @Registerer
    public static final class XPBarOutlines {
        public static final RankCardItem PEBBLES = RANK_CARD_ITEMS.register("pebbles",
            new RankCardItem("pebbles", Type.XP_OUTLINE_IMAGE, Rarity.COMMON));
        
        public static final RankCardItem AUTUMN_FOREST = RANK_CARD_ITEMS.register("autumn_forest",
            new RankCardItem("autumn_forest", Type.XP_OUTLINE_IMAGE, Rarity.RARE));
    }
}
