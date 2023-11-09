package dev.darealturtywurty.superturtybot.core.api.request;

import lombok.Getter;

import java.util.Optional;

@Getter
public class ImageRotateRequestData {
    private final Optional<Integer> angle;
    private final String url;

    private ImageRotateRequestData(String url, Integer angle) {
        this.url = url;
        this.angle = Optional.ofNullable(angle);
    }

    public static class Builder {
        private Integer angle;
        private final String url;

        public Builder(String url) {
            if (url == null)
                throw new IllegalArgumentException("URL cannot be null!");

            if (url.isBlank())
                throw new IllegalArgumentException("URL cannot be blank!");

            this.url = url;
        }

        public Builder angle(int angle) {
            if (angle % 90 != 0)
                throw new IllegalArgumentException("Angle must be a multiple of 90!");

            this.angle = angle;
            return this;
        }

        public ImageRotateRequestData build() {
            return new ImageRotateRequestData(url, angle);
        }
    }
}
