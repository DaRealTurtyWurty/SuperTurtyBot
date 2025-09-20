package dev.darealturtywurty.superturtybot.database.pojos.collections;

import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollector;
import lombok.Data;
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

    public Collectables getCollectables(CollectableGameCollector<?> type) {
        return collectables.stream().filter(collectables -> collectables.getType().equals(type.getName())).findFirst().orElseGet(() -> {
            var collectables = new Collectables(type.getName());
            this.collectables.add(collectables);
            return collectables;
        });
    }

    public void collect(CollectableGameCollector<?> type, Collectable collectable) {
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
}
