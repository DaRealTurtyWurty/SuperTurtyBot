package dev.darealturtywurty.superturtybot.modules.collectable;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public abstract class Collectable implements Registerable {
    private final String emoji;
    private String name;

    protected Collectable(String name, String emoji) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Name cannot be null or empty!");

        if (emoji == null || emoji.isEmpty())
            throw new IllegalArgumentException("Emoji cannot be null or empty!");

        this.name = name;
        this.emoji = emoji;
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
