package dev.darealturtywurty.superturtybot.modules.collectable.anime;

import dev.darealturtywurty.superturtybot.modules.collectable.Answer;
import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollector;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollectorRegistry;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public final class AnimeCharacterCollectable extends Collectable {
    private final String richName;
    private final String question;
    private final Answer answer;
    private final CollectableRarity rarity;
    private final String note;

    AnimeCharacterCollectable(String name, String richName, Path imagePath) {
        super(name, imagePath);
        this.richName = richName;
        this.question = "What is the name of this anime character?";
        this.answer = new Answer.Builder<>()
                .or(richName, name)
                .build();
        this.rarity = CollectableRarity.COMMON;
        this.note = null;
    }

    @Override
    public CollectableGameCollector<?> getCollectionType() {
        return CollectableGameCollectorRegistry.ANIME_CHARACTERS;
    }
}
