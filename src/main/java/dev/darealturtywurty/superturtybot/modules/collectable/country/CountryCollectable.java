package dev.darealturtywurty.superturtybot.modules.collectable.country;

import dev.darealturtywurty.superturtybot.modules.collectable.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class CountryCollectable extends Collectable {
    private final String richName;
    private final String question;
    private final Answer answer;
    private final CollectableRarity rarity;
    private final String note;

    private CountryCollectable(String name, String emoji, String question, Answer answer, CollectableRarity rarity, String note) {
        super(name.toLowerCase(Locale.ROOT).replace(" ", "_"), emoji);
        this.richName = name;
        this.question = question;
        this.answer = answer;
        this.rarity = rarity;
        this.note = note;
    }

    @Override
    public CollectableGameCollector<?> getCollectionType() {
        return CollectableGameCollectorRegistry.COUNTRIES;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "Unknown";
        private String emoji = "❓";
        private String question = "Respond with the name of the country to collect them!";
        private final Answer.Builder<Builder> answer = new Answer.Builder<>();
        private CollectableRarity rarity = CollectableRarity.COMMON;
        private String note = "";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder emoji(String name, long id) {
            this.emoji = "<:" + name + ":" + id + ">";
            return this;
        }

        public Builder emoji(String name) {
            this.emoji = ":" + name + ":";
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder answer(String answer) {
            addNormalizedAnswerVariants(false, true, answer);
            return this;
        }

        public Builder answerAny(String... answers) {
            addNormalizedAnswerVariants(false, true, answers);
            return this;
        }

        public Builder answer(double answer) {
            this.answer.numberSegment(answer);
            return this;
        }

        public Builder answerExact(String answer, boolean caseSensitive) {
            addNormalizedAnswerVariants(caseSensitive, false, answer);
            return this;
        }

        public Builder answerExactAny(String... answers) {
            addNormalizedAnswerVariants(false, false, answers);
            return this;
        }

        public Builder answerExact(String answer) {
            return answerExact(answer, false);
        }

        public Answer.Builder<Builder> answer() {
            return this.answer.start(this);
        }

        public Builder rarity(CollectableRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public CountryCollectable build() {
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

            if(note != null && note.isBlank())
                note = null;

            return new CountryCollectable(name, emoji, question, answer, rarity, note);
        }

        private void addNormalizedAnswerVariants(boolean caseSensitive, boolean contains, String... answers) {
            List<String> variants = new ArrayList<>();
            for (String answer : answers) {
                if (answer == null || answer.isBlank()) {
                    continue;
                }

                String trimmed = answer.trim();
                addVariant(variants, trimmed);

                String normalized = normalizeAnswer(trimmed);
                addVariant(variants, normalized);

                String noSpaces = normalized.replace(" ", "");
                addVariant(variants, noSpaces);
            }

            if (variants.isEmpty()) {
                throw new IllegalArgumentException("Answer must be set!");
            }

            if (variants.size() == 1) {
                this.answer.segment(variants.getFirst(), caseSensitive, contains);
                return;
            }

            this.answer.or(variants.toArray(String[]::new));
        }

        private void addVariant(List<String> variants, String variant) {
            if (variant == null || variant.isBlank() || variants.contains(variant)) {
                return;
            }

            variants.add(variant);
        }

        private String normalizeAnswer(String answer) {
            String normalized = Normalizer.normalize(answer, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .replace('’', '\'')
                    .replace('`', '\'')
                    .replace('‘', '\'')
                    .replace('“', '"')
                    .replace('”', '"')
                    .replaceAll("[^\\p{Alnum}\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return normalized.isEmpty() ? answer : normalized;
        }
    }
}
