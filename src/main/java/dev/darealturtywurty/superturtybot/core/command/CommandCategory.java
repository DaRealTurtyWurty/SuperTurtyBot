package dev.darealturtywurty.superturtybot.core.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface CommandCategory {
    CommandCategory CORE = create("Core", "🌍");
    CommandCategory UTILITY = create("Utility", "⚒️");
    CommandCategory FUN = create("Fun", "😂");
    CommandCategory MODERATION = create("Moderation", "⚖");
    CommandCategory NSFW = create("NSFW", "🥵", true);
    CommandCategory LEVELLING = create("Levelling", "🔝");
    CommandCategory MINIGAMES = create("Minigames", "🎮");
    CommandCategory ECONOMY = create("Economy", "💰");
    
    String getEmoji();
    
    @NotNull String getName();
    
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
            public @NotNull String getName() {
                return name;
            }
        };
        
        CommandHook.getCategories().add(category);
        return category;
    }
    
    static CommandCategory create(String name, String emoji, boolean isNSFW) {
        final var category = new CommandCategory() {
            @Override
            public String getEmoji() {
                return emoji;
            }
            
            @Override
            public @NotNull String getName() {
                return name;
            }
            
            @Override
            public boolean isNSFW() {
                return isNSFW;
            }
        };
        
        CommandHook.getCategories().add(category);
        return category;
    }
    
    static Set<CommandCategory> getCategories() {
        return Set.of(CommandHook.getCategories().toArray(new CommandCategory[0]));
    }
}
