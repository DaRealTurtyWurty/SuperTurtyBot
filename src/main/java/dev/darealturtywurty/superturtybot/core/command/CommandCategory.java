package dev.darealturtywurty.superturtybot.core.command;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

public interface CommandCategory {
    CommandCategory CORE = create("Core", "🌍");
    CommandCategory UTILITY = create("Utility", "⚒️");
    CommandCategory FUN = create("Fun", "😂");
    CommandCategory MODERATION = create("Moderation", "⚖");
    CommandCategory NSFW = create("NSFW", "🥵", true);
    CommandCategory MUSIC = create("Music", "🎶");
    CommandCategory IMAGE = create("Image", "🖼️");
    CommandCategory LEVELLING = create("Levelling", "🔝");
    
    String getEmoji();
    
    String getName();
    
    default boolean isNSFW() {
        return false;
    }
    
    @Nullable
    static CommandCategory byName(String name) {
        return getCategories().stream().filter(category -> category.getName().equalsIgnoreCase(name)).findFirst()
            .orElse(null);
    }
    
    static CommandCategory create(String name, String emoji) {
        final var category = new CommandCategory() {
            @Override
            public String getEmoji() {
                return emoji;
            }
            
            @Override
            public String getName() {
                return name;
            }
        };
        
        CommandHook.CATEGORIES.add(category);
        return category;
    }
    
    static CommandCategory create(String name, String emoji, boolean isNSFW) {
        final var category = new CommandCategory() {
            @Override
            public String getEmoji() {
                return emoji;
            }
            
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public boolean isNSFW() {
                return isNSFW;
            }
        };
        
        CommandHook.CATEGORIES.add(category);
        return category;
    }
    
    static Set<CommandCategory> getCategories() {
        return Set.of(CommandHook.CATEGORIES.toArray(new CommandCategory[0]));
    }
}
