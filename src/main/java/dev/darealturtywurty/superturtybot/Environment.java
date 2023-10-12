package dev.darealturtywurty.superturtybot;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class Environment {
    public static final Environment INSTANCE = new Environment();
    private Dotenv env;

    private Environment() {}

    public Optional<String> activity() {
        return getString("ACTIVITY");
    }

    public Optional<String> botToken() {
        return getString("BOT_TOKEN");
    }

    public Optional<String> curseforgeKey() {
        return getString("CURSEFORGE_KEY");
    }

    public Optional<String> defaultPrefix() {
        return getString("DEFAULT_PREFIX");
    }

    public Optional<String> githubOAuthToken() {
        return getString("GITHUB_OAUTH_TOKEN");
    }

    public Optional<String> loggingWebhookId() {
        return getString("LOGGING_WEBHOOK_ID");
    }

    public Optional<String> loggingWebhookToken() {
        return getString("LOGGING_WEBHOOK_TOKEN");
    }

    public Optional<String> mongoPassword() {
        return getString("MONGO_PASSWORD");
    }

    public Optional<String> mongoUsername() {
        return getString("MONGO_USERNAME");
    }

    public Optional<Long> ownerId() {
        return getLong("OWNER_ID");
    }

    public Optional<String> pexelsKey() {
        return getString("PEXELS_KEY");
    }

    public Optional<String> r6StatsKey() {
        return getString("R6_STATS_KEY");
    }

    public Optional<String> redditClientId() {
        return getString("REDDIT_CLIENT_ID");
    }

    public Optional<String> redditClientSecret() {
        return getString("REDDIT_CLIENT_SECRET");
    }
    
    public Optional<String> spotifyID() {
        return getString("SPOTIFY_CLIENT_ID");
    }

    public Optional<String> spotifySecret() {
        return getString("SPOTIFY_CLIENT_SECRET");
    }
    
    public Optional<String> steamKey() {
        return getString("STEAM_KEY");
    }

    public Optional<String> twitchOAuthToken() {
        return getString("TWITCH_OAUTH_TOKEN");
    }
    
    public Optional<String> twitterApiKey() {
        return getString("TWITTER_API_KEY");
    }

    public Optional<String> twitterAPIKeySecret() {
        return getString("TWITTER_API_KEY_SECRET");
    }
    
    public Optional<String> twitterAppId() {
        return getString("TWITTER_APP_ID");
    }

    public Optional<String> twitterBearerToken() {
        return getString("TWITTER_BEARER_TOKEN");
    }

    public Optional<String> urbanDictionaryKey() {
        return getString("URBAN_DICTIONARY_KEY");
    }

    public Optional<String> youtubeApiKey() {
        return getString("YOUTUBE_API_KEY");
    }

    public Optional<String> geniusClientSecret() {
        return getString("GENIUS_CLIENT_SECRET");
    }

    public Optional<String> geniusClientID() {
        return getString("GENIUS_CLIENT_ID");
    }

    public Optional<String> geniusAccessToken() {
        return getString("GENIUS_ACCESS_TOKEN");
    }

    public Optional<String> nasaApiKey(){return getString("NASA_KEY");}

    public Optional<Double> getDouble(String key){
        try {
            return getString(key).map(Double::parseDouble);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Float> getFloat(String key) {
        try {
            return getString(key).map(Float::parseFloat);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getInteger(String key) {
        try {
            return getString(key).map(Integer::parseInt);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Long> getLong(String key) {
        try {
            return getString(key).map(Long::parseLong);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> getString(String key) {
        try {
            return Optional.ofNullable(this.env.get(key));
        } catch (final NullPointerException exception) {
            return Optional.empty();
        }
    }

    public void print() {
        this.env.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
                .forEach(entry -> Constants.LOGGER.debug("{}={}", entry.getKey(), entry.getValue()));
    }

    public Activity.ActivityType activityType() {
        Optional<String> activityType = getString("ACTIVITY_TYPE");
        return activityType.map(s -> ActivityType.valueOf(s.toUpperCase(Locale.ROOT))).orElse(ActivityType.PLAYING);
    }

    public void load(Path environment) {
        if(this.env != null)
            throw new IllegalStateException("Environment already loaded!");

        this.env = Dotenv.configure().directory(environment.toString()).load();
    }
}
