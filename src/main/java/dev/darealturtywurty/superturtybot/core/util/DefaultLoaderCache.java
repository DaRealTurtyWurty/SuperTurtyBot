package dev.darealturtywurty.superturtybot.core.util;

public class DefaultLoaderCache<T> extends SimpleInvalidatingCache<T> {
    private final Loader<T> loader;

    public DefaultLoaderCache(Loader<T> loader) {
        super();
        this.loader = loader;
    }

    public DefaultLoaderCache(long expirationTime, boolean checkExpired, Loader<T> loader) {
        super(expirationTime, checkExpired);
        this.loader = loader;
    }

    public DefaultLoaderCache(long expirationTime, Loader<T> loader) {
        super(expirationTime, true);
        this.loader = loader;
    }

    public DefaultLoaderCache(Loader<T> loader, boolean checkExpired) {
        super(checkExpired);
        this.loader = loader;
    }

    public T get() {
        return super.getOrLoad(this.loader);
    }
}
