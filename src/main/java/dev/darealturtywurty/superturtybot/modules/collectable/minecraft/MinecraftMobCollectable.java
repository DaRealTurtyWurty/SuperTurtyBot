package dev.darealturtywurty.superturtybot.modules.collectable.minecraft;

import dev.darealturtywurty.superturtybot.core.util.EmojiReader;
import dev.darealturtywurty.superturtybot.modules.collectable.Answer;
import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class MinecraftMobCollectable extends Collectable {
    private final String richName;
    private final String question;
    private final Answer answer;
    private final MobCategory category;
    private final CollectableRarity rarity;

    private MinecraftMobCollectable(String name, String emoji, String question, Answer answer, MobCategory category, CollectableRarity rarity) {
        super(name.toLowerCase(Locale.ROOT).replace(" ", "_"), emoji);
        this.richName = name;
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.rarity = rarity;
    }

    public static class Builder {
        private String name = "Unknown";
        private String emoji = "❓";
        private String question = "Respond with the name of the mob to collect it!";
        private final Answer.Builder answer = new Answer.Builder();
        private MobCategory category = MobCategory.MISC;
        private CollectableRarity rarity = CollectableRarity.COMMON;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder emoji(String name, long id) {
            this.emoji = "<:" + name + ":" + id + ">";
            return this;
        }

        public Builder emoji(String name) {
            return emoji(name, EmojiReader.getEmoji(name));
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder answer(String answer) {
            this.answer.segment(answer, false, true);
            return this;
        }

        public Builder answer(double answer) {
            this.answer.numberSegment(answer);
            return this;
        }

        public Builder answerExact(String answer, boolean caseSensitive) {
            this.answer.segment(answer, caseSensitive, false);
            return this;
        }

        public Builder answerExact(String answer) {
            return answerExact(answer, false);
        }

        public Builder answerYesOrNo(boolean yes) {
            answerExact(yes ? "yes" : "no", false);
            return this;
        }

        public Builder answerYes() {
            return answerYesOrNo(true);
        }

        public Builder answerNo() {
            return answerYesOrNo(false);
        }

        public Builder answerTrueOrFalse(boolean bool) {
            answerExact(bool ? "true" : "false", false);
            return this;
        }

        public Builder answerTrue() {
            return answerTrueOrFalse(true);
        }

        public Builder answerFalse() {
            return answerTrueOrFalse(false);
        }

        public Answer.Builder answer() {
            return this.answer.start(this);
        }

        public Builder category(MobCategory category) {
            this.category = category;
            return this;
        }

        public Builder rarity(CollectableRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public MinecraftMobCollectable build() {
            Answer answer = this.answer.build();
            if(answer.isEmpty())
                throw new IllegalArgumentException("Answer must be set!");

            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name must be set!");

            if (emoji == null || emoji.isBlank())
                throw new IllegalArgumentException("Emoji must be set!");

            if (question == null || question.isBlank())
                throw new IllegalArgumentException("Question must be set!");

            if (category == null)
                throw new IllegalArgumentException("Category must be set!");

            if (rarity == null)
                throw new IllegalArgumentException("Rarity must be set!");

            return new MinecraftMobCollectable(name, emoji, question, answer, category, rarity);
        }
    }
}
