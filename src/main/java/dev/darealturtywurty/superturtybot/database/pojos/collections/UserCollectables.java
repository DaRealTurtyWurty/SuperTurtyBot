package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class UserCollectables {
    private long user;
    private List<Collectables> collectables = new ArrayList<>();

    public UserCollectables(long user) {
        this.user = user;
    }

    public Collectables getCollectables(CollectionType type) {
        return collectables.stream().filter(collectables -> collectables.getType().equals(type.getName())).findFirst().orElseGet(() -> {
            var collectables = new Collectables(type.getName());
            this.collectables.add(collectables);
            return collectables;
        });
    }

    public boolean hasCollectable(CollectionType type, Collectable collectable) {
        return getCollectables(type).hasCollectable(collectable);
    }

    public void collect(CollectionType type, Collectable collectable) {
        getCollectables(type).collect(collectable);
    }

    @NoArgsConstructor
    @Data
    public static class Collectables {
        private String type;
        private List<String> collectables = new ArrayList<>();

        public Collectables(String type) {
            this.type = type;
        }

        public void collect(Collectable collectable) {
            this.collectables.add(collectable.getName());
        }

        public boolean hasCollectable(Collectable collectable) {
            return this.collectables.contains(collectable.getName());
        }
    }

    @Getter
    public enum CollectionType {
        MINECRAFT_MOBS("minecraft_mobs");

        private final String name;

        CollectionType(String name) {
            this.name = name;
        }
    }
}
