package dev.darealturtywurty.superturtybot.core.api.request;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RandomWordRequestData {
    private final Optional<Integer> length, minLength, maxLength;
    private final Optional<String> startsWith;
    private final Optional<Integer> amount;

    private RandomWordRequestData(Integer length, Integer minLength, Integer maxLength, String startsWith, Integer amount) {
        this.length = Optional.ofNullable(length);
        this.minLength = Optional.ofNullable(minLength);
        this.maxLength = Optional.ofNullable(maxLength);
        this.startsWith = Optional.ofNullable(startsWith);
        this.amount = Optional.ofNullable(amount);
    }

    public Optional<Integer> getLength() {
        return length;
    }

    public Optional<Integer> getMinLength() {
        return minLength;
    }

    public Optional<Integer> getMaxLength() {
        return maxLength;
    }

    public Optional<String> getStartsWith() {
        return startsWith;
    }

    public Optional<Integer> getAmount() {
        return amount;
    }

    public static class Builder {
        private Integer length = null, minLength = null, maxLength = null;
        private String startsWith = null;
        private Integer amount = null;

        public Builder length(int length) {
            if(length < 0)
                throw new IllegalArgumentException("Length must be greater than 0!");

            this.length = length;
            return this;
        }

        public Builder length(int minLength, int maxLength) {
            if(minLength < 0)
                throw new IllegalArgumentException("Min length must be greater than 0!");

            if(maxLength < 0)
                throw new IllegalArgumentException("Max length must be greater than 0!");

            if(minLength > maxLength)
                throw new IllegalArgumentException("Min length must be less than max length!");

            this.minLength = minLength;
            this.maxLength = maxLength;
            return this;
        }

        public Builder minLength(int minLength) {
            if(minLength < 0)
                throw new IllegalArgumentException("Min length must be greater than 0!");

            this.minLength = minLength;
            return this;
        }

        public Builder maxLength(int maxLength) {
            if(maxLength < 1)
                throw new IllegalArgumentException("Max length must be greater than 0!");

            this.maxLength = maxLength;
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

        public RandomWordRequestData build() {
            return new RandomWordRequestData(length, minLength, maxLength, startsWith, amount);
        }
    }

    public static RandomWordRequestData blank() {
        return new RandomWordRequestData(null, null, null, null, null);
    }
}
