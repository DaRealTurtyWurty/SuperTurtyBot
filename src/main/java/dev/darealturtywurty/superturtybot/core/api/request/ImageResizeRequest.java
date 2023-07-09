package dev.darealturtywurty.superturtybot.core.api.request;

import java.util.Optional;

public class ImageResizeRequest {
    private final Optional<Integer> width;
    private final Optional<Integer> height;
    private final String url;

    private ImageResizeRequest(String url, Integer width, Integer height) {
        this.url = url;
        this.width = Optional.ofNullable(width);
        this.height = Optional.ofNullable(height);
    }

    public Optional<Integer> getWidth() {
        return width;
    }

    public Optional<Integer> getHeight() {
        return height;
    }

    public String getUrl() {
        return url;
    }

    public static class Builder {
        private Integer width;
        private Integer height;
        private final String url;

        public Builder(String url) {
            if (url == null)
                throw new IllegalArgumentException("URL cannot be null!");

            if (url.isBlank())
                throw new IllegalArgumentException("URL cannot be blank!");

            this.url = url;
        }

        public Builder width(int width) {
            if (width < 1)
                throw new IllegalArgumentException("Width must be greater than 0!");

            this.width = width;
            return this;
        }

        public Builder height(int height) {
            if (height < 1)
                throw new IllegalArgumentException("Height must be greater than 0!");

            this.height = height;
            return this;
        }

        public Builder size(int width, int height) {
            if (width < 1)
                throw new IllegalArgumentException("Width must be greater than 0!");

            if (height < 1)
                throw new IllegalArgumentException("Height must be greater than 0!");

            this.width = width;
            this.height = height;
            return this;
        }

        public ImageResizeRequest build() {
            return new ImageResizeRequest(url, width, height);
        }
    }
}
