package dev.darealturtywurty.superturtybot.core.api.request;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WordRequest {
    private final Optional<Integer> length;
    private final Optional<String> startsWith;
    private final Optional<Integer> amount;

    private WordRequest(Integer length, String startsWith, Integer amount) {
        this.length = Optional.ofNullable(length);
        this.startsWith = Optional.ofNullable(startsWith);
        this.amount = Optional.ofNullable(amount);
    }

    public Optional<Integer> getLength() {
        return length;
    }

    public Optional<String> getStartsWith() {
        return startsWith;
    }

    public Optional<Integer> getAmount() {
        return amount;
    }

    public static class Builder {
        private Integer length = null;
        private String startsWith = null;
        private Integer amount = null;

        public Builder length(int length) {
            if(length < 0)
                throw new IllegalArgumentException("Length must be greater than 0!");

            this.length = length;
            return this;
        }

        public Builder startsWith(@NotNull String startsWith) {
            if (startsWith.isBlank())
                throw new IllegalArgumentException("Starts with cannot be blank!");

            if(!startsWith.matches("[a-zA-Z]+"))
                throw new IllegalArgumentException("Starts with must only contain letters!");

            this.startsWith = startsWith;
            return this;
        }

        public Builder amount(int amount) {
            if(amount < 0)
                throw new IllegalArgumentException("Amount must be greater than 0!");

            this.amount = amount;
            return this;
        }

        public WordRequest build() {
            return new WordRequest(length, startsWith, amount);
        }
    }

    public static WordRequest blank() {
        return new WordRequest(null, null, null);
    }
}
