package dev.darealturtywurty.superturtybot.modules.collectable;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@Getter
@ToString
@EqualsAndHashCode
public abstract class Collectable implements Registerable {
    private final String emoji;
    private final Path imagePath;
    private String name;

    protected Collectable(String name, String emoji) {
        this(name, emoji, null);
    }

    protected Collectable(String name, Path imagePath) {
        this(name, null, imagePath);
    }

    private Collectable(String name, String emoji, Path imagePath) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name cannot be null or empty!");

        if ((emoji == null || emoji.isEmpty()) == (imagePath == null))
            throw new IllegalArgumentException("A collectable must have exactly one emoji or image!");

        this.name = name;
        this.emoji = emoji;
        this.imagePath = imagePath == null ? null : imagePath.toAbsolutePath().normalize();
    }

    public abstract String getRichName();

    public abstract String getQuestion();

    public abstract Answer getAnswer();

    public abstract CollectableRarity getRarity();

    public abstract CollectableGameCollector<?> getCollectionType();

    public abstract String getNote();

    @Override
    public Registerable setName(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name cannot be null or empty!");

        if (this.name != null)
            return this;

        this.name = name;
        return this;
    }
}
