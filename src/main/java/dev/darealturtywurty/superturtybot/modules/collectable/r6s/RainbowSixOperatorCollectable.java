package dev.darealturtywurty.superturtybot.modules.collectable.r6s;

import dev.darealturtywurty.superturtybot.core.util.EmojiReader;
import dev.darealturtywurty.superturtybot.modules.collectable.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class RainbowSixOperatorCollectable extends Collectable {
    private final String richName;
    private final String question;
    private final Answer answer;
    private final CollectableRarity rarity;

    private RainbowSixOperatorCollectable(String name, String emoji, String question, Answer answer, CollectableRarity rarity) {
        super(name.toLowerCase(Locale.ROOT).replace(" ", "_"), emoji);
        this.richName = name;
        this.question = question;
        this.answer = answer;
        this.rarity = rarity;
    }

    @Override
    public CollectableGameCollector<?> getCollectionType() {
        return CollectableGameCollectorRegistry.RAINBOW_SIX_OPERATORS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "Unknown";
        private String emoji = "❓";
        private String question = "Respond with the name of the operator to collect them!";
        private final Answer.Builder<Builder> answer = new Answer.Builder<>();
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
            long emojiId = EmojiReader.getEmoji(name);
            if (emojiId == 0)
                throw new IllegalArgumentException("Emoji with name '" + name + "' does not exist in " + EmojiReader.getEmojisPath().getFileName() + "!");

            return emoji(name, emojiId);
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

        public Answer.Builder<Builder> answer() {
            return this.answer.start(this);
        }

        public Builder rarity(CollectableRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public RainbowSixOperatorCollectable build() {
            Answer answer = this.answer.build();
            if (answer.isEmpty())
                throw new IllegalArgumentException("Answer must be set!");

            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name must be set!");

            if (emoji == null || emoji.isBlank())
                throw new IllegalArgumentException("Emoji must be set!");

            if (question == null || question.isBlank())
                throw new IllegalArgumentException("Question must be set!");

            if (rarity == null)
                throw new IllegalArgumentException("Rarity must be set!");

            return new RainbowSixOperatorCollectable(name, emoji, question, answer, rarity);
        }
    }
}
