package io.github.darealturtywurty.superturtybot;

import org.jetbrains.annotations.NotNull;

import io.github.cdimascio.dotenv.Dotenv;
import kotlin.jvm.Throws;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;

public final class Environment {
    public static final Environment INSTANCE = new Environment();
    private final Dotenv env = Dotenv.load();
    
    private Environment() {
    }

    public String activity() {
        return getString("ACTIVITY");
    }

    public Activity.ActivityType activityType() {
        final String strType = getString("ACTIVITY_TYPE");
        final Activity.ActivityType type = ActivityType.valueOf(strType.toUpperCase());
        return type == null ? Activity.ActivityType.PLAYING : type;
    }

    public String botToken() {
        return getString("BOT_TOKEN");
    }

    public String curseforgeKey() {
        return getString("CURSEFORGE_KEY");
    }
    
    public String defaultPrefix() {
        return getString("DEFAULT_PREFIX");
    }

    @NotNull
    @Throws(exceptionClasses = IllegalStateException.class)
    public double getDouble(String key) {
        try {
            return Double.parseDouble(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an double!", exception);
        }
    }

    @NotNull
    @Throws(exceptionClasses = IllegalStateException.class)
    public float getFloat(String key) {
        try {
            return Float.parseFloat(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an float!", exception);
        }
    }
    
    @NotNull
    @Throws(exceptionClasses = IllegalStateException.class)
    public int getInteger(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an integer!", exception);
        }
    }

    @NotNull
    @Throws(exceptionClasses = IllegalStateException.class)
    public long getLong(String key) {
        try {
            return Long.parseLong(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an long!", exception);
        }
    }
    
    @NotNull
    @Throws(exceptionClasses = IllegalStateException.class)
    public String getString(String key) {
        try {
            return this.env.get(key);
        } catch (final NullPointerException exception) {
            throw new IllegalStateException("'" + key + "' does not exist in this .env!", exception);
        }
    }
    
    public String mongoPassword() {
        return getString("MONGO_PASSWORD");
    }

    public String mongoUsername() {
        return getString("MONGO_USERNAME");
    }

    public long ownerId() {
        return getLong("OWNER_ID");
    }

    public String pexelsKey() {
        return getString("PEXELS_KEY");
    }

    public void print() {
        this.env.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
            .forEach(entry -> System.out.println(entry.getKey() + "=" + entry.getValue()));
    }

    public String r6StatsKey() {
        return getString("R6_STATS_KEY");
    }
    
    public String redditClientId() {
        return getString("REDDIT_CLIENT_ID");
    }

    public String redditClientSecret() {
        return getString("REDDIT_CLIENT_SECRET");
    }

    public String spotifyID() {
        return getString("SPOTIFY_CLIENT_ID");
    }

    public String spotifySecret() {
        return getString("SPOTIFY_CLIENT_SECRET");
    }

    public String urbanDictionaryKey() {
        return getString("URBAN_DICTIONARY_KEY");
    }
}
