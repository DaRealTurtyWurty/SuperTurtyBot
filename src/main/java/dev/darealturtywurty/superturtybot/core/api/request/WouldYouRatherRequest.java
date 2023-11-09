package dev.darealturtywurty.superturtybot.core.api.request;

import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public class WouldYouRatherRequest {
    private final boolean includeNsfw;
    private final boolean nsfw;

    private WouldYouRatherRequest(final boolean includeNsfw, final boolean nsfw) {
        this.includeNsfw = includeNsfw;
        this.nsfw = nsfw;
    }

    public static WouldYouRatherRequest nsfw() {
        return new WouldYouRatherRequest(true, true);
    }

    public static WouldYouRatherRequest sfw() {
        return new WouldYouRatherRequest(false, false);
    }

    public static WouldYouRatherRequest randomlyNsfw() {
        return new WouldYouRatherRequest(
                ThreadLocalRandom.current().nextDouble() < 0.2D,
                false);
    }

    public static class Builder {
        private boolean includeNsfw = false;
        private boolean nsfw = false;

        public Builder includeNsfw(final boolean includeNsfw) {
            this.includeNsfw = includeNsfw;
            return this;
        }

        public Builder nsfw(final boolean nsfw) {
            this.nsfw = nsfw;
            return this;
        }

        public WouldYouRatherRequest build() {
            return new WouldYouRatherRequest(this.includeNsfw, this.nsfw);
        }
    }
}
