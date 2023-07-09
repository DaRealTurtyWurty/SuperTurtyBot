package dev.darealturtywurty.superturtybot.core.api.request;

public class ImageFlagifyRequest {
    private final int colors;
    private final String url;

    private ImageFlagifyRequest(String url, int colors) {
        this.url = url;
        this.colors = colors;
    }

    public int getColors() {
        return colors;
    }

    public String getUrl() {
        return url;
    }

    public static class Builder {
        private int colors = 5;
        private final String url;

        public Builder(String url) {
            if (url == null)
                throw new IllegalArgumentException("URL cannot be null!");

            if (url.isBlank())
                throw new IllegalArgumentException("URL cannot be blank!");

            this.url = url;
        }

        public Builder colors(int colors) {
            if (colors < 1)
                throw new IllegalArgumentException("Colors must be greater than 0!");

            if (colors > 10)
                throw new IllegalArgumentException("Colors must be less than 10!");

            this.colors = colors;
            return this;
        }

        public ImageFlagifyRequest build() {
            return new ImageFlagifyRequest(url, colors);
        }
    }
}
