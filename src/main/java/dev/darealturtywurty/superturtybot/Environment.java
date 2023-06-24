package dev.darealturtywurty.superturtybot;

import org.jetbrains.annotations.NotNull;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;

import java.nio.file.Path;

public final class Environment {
    public static final Environment INSTANCE = new Environment();
    private Dotenv env;

    private Environment() {}

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
    public double getDouble(String key) throws IllegalStateException {
        try {
            return Double.parseDouble(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an double!", exception);
        }
    }

    @NotNull
    public float getFloat(String key) throws IllegalStateException {
        try {
            return Float.parseFloat(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an float!", exception);
        }
    }

    @NotNull
    public int getInteger(String key) throws IllegalStateException {
        try {
            return Integer.parseInt(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an integer!", exception);
        }
    }

    @NotNull
    public long getLong(String key) throws IllegalStateException {
        try {
            return Long.parseLong(getString(key));
        } catch (final NumberFormatException exception) {
            throw new IllegalStateException("'" + key + "' is not an long!", exception);
        }
    }

    @NotNull
    public String getString(String key) throws IllegalStateException {
        try {
            return this.env.get(key);
        } catch (final NullPointerException exception) {
            throw new IllegalStateException("'" + key + "' does not exist in this .env!", exception);
        }
    }

    public String githubOAuthToken() {
        return getString("GITHUB_OAUTH_TOKEN");
    }

    public String loggingWebhookId() {
        return getString("LOGGING_WEBHOOK_ID");
    }

    public String loggingWebhookToken() {
        return getString("LOGGING_WEBHOOK_TOKEN");
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
            .forEach(entry -> Constants.LOGGER.debug("{}={}", entry.getKey(), entry.getValue()));
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
    
    public String steamKey() {
        return getString("STEAM_KEY");
    }

    public String twitchOAuthToken() {
        return getString("TWITCH_OAUTH_TOKEN");
    }
    
    public String twitterApiKey() {
        return getString("TWITTER_API_KEY");
    }

    public String twitterAPIKeySecret() {
        return getString("TWITTER_API_KEY_SECRET");
    }
    
    public String twitterAppId() {
        return getString("TWITTER_APP_ID");
    }

    public String twitterBearerToken() {
        return getString("TWITTER_BEARER_TOKEN");
    }

    public String urbanDictionaryKey() {
        return getString("URBAN_DICTIONARY_KEY");
    }

    public String youtubeApiKey() {
        return getString("YOUTUBE_API_KEY");
    }

    public String geniusClientSecret() {
        return getString("GENIUS_CLIENT_SECRET");
    }

    public String geniusClientID() {
        return getString("GENIUS_CLIENT_ID");
    }

    public String geniusAccessToken() {
        return getString("GENIUS_ACCESS_TOKEN");
    }

    public void load(Path environment) {
        if(this.env != null)
            throw new IllegalStateException("Environment already loaded!");

        this.env = Dotenv.configure().directory(environment.toString()).load();
    }
}
