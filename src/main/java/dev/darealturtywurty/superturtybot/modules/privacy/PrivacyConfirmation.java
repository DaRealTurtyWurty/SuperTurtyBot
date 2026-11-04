package dev.darealturtywurty.superturtybot.modules.privacy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class PrivacyConfirmation {
    private static final Duration LIFETIME = Duration.ofMinutes(5);

    private final Cache<String, Pending> pending = CacheBuilder.newBuilder()
        .maximumSize(1_000).expireAfterWrite(5, TimeUnit.MINUTES).build();
    private final Clock clock;

    public PrivacyConfirmation() {
        this(Clock.systemUTC());
    }

    public PrivacyConfirmation(Clock clock) {
        this.clock = clock;
    }

    public String create(long userId) {
        String token = UUID.randomUUID().toString();
        pending.put(token, new Pending(userId, clock.millis() + LIFETIME.toMillis()));
        return token;
    }

    public boolean consume(String token, long userId) {
        Pending confirmation = pending.getIfPresent(token);
        return confirmation != null && confirmation.userId() == userId
            && confirmation.expiresAt() > clock.millis()
            && pending.asMap().remove(token, confirmation);
    }

    private record Pending(long userId, long expiresAt) {
    }
}
