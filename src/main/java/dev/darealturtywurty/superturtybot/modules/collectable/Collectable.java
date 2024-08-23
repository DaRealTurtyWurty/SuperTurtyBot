package dev.darealturtywurty.superturtybot.modules.collectable;

import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobCollector;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public abstract class Collectable implements Registerable {
    private String name;
    private final String emoji;

    @Override
    public Registerable setName(String name) {
        if(name == null || name.isEmpty())
            throw new IllegalArgumentException("Name cannot be null or empty!");

        if(this.name != null)
            return this;

        this.name = name;
        return this;
    }
}
