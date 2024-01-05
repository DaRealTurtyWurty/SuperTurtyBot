package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WeatherCommand extends CoreCommand {
    private static final String GEOCODE_URL = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&hourly=temperature_2m,relativehumidity_2m,apparent_temperature,precipitation_probability,precipitation,rain,showers,snowfall,windspeed_10m,winddirection_10m&daily=temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_sum,rain_sum,showers_sum,snowfall_sum,windspeed_10m_max,windgusts_10m_max&current_weather=true&timezone=GMT&lang=en";

    public WeatherCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "location", "The location to get the weather for.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets the weather for a specified location.";
    }

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getRichName() {
        return "Weather";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 20L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String location = event.getOption("location", "London", OptionMapping::getAsString);
        EmbedBuilder embed = getForecast(location);
        reply(event, embed);
    }

    private static Either<String, GeocodeResult> geocode(String location) {
        String url = String.format(GEOCODE_URL, location);
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful())
                return Either.left("The response was unsuccessful! Maybe our API is down right now? Try again later!");

            ResponseBody body = response.body();
            if (body == null)
                return Either.left("No response was received! Maybe our API is down right now? Try again later!");

            String json = body.string();
            GeocodeResponse geocodeResponse = Constants.GSON.fromJson(json, GeocodeResponse.class);
            List<GeocodeResult> results = geocodeResponse.getResults();
            if (results.isEmpty())
                return Either.left("No results found for the specified location!");

            return Either.right(results.getFirst());
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while getting the weather!", exception);
            return Either.left("An error occurred while getting the weather!");
        }
    }

    private static EmbedBuilder getForecast(String location) {
        Either<String, GeocodeResult> errorOrGeocode = geocode(location);
        if (errorOrGeocode.isLeft()) {
            return new EmbedBuilder()
                    .setTitle("❌ An error occurred!")
                    .setDescription(errorOrGeocode.getLeft())
                    .setColor(Color.RED);
        }

        GeocodeResult result = errorOrGeocode.getRight();

        String url = String.format(FORECAST_URL, result.getLatitude(), result.getLongitude());
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return new EmbedBuilder()
                        .setTitle("❌ An error occurred!")
                        .setDescription("An error occurred while getting the weather!")
                        .setColor(Color.RED);
            }

            ResponseBody body = response.body();
            if (body == null) {
                return new EmbedBuilder()
                        .setTitle("❌ An error occurred!")
                        .setDescription("An error occurred while getting the weather!")
                        .setColor(Color.RED);
            }

            String json = body.string();
            OpenMeteoWeatherForecast forecast = Constants.GSON.fromJson(json, OpenMeteoWeatherForecast.class);
            CurrentWeather currentWeather = forecast.getCurrent_weather();
            DailyUnits dailyUnits = forecast.getDaily_units();
            DailyForecast dailyForecast = forecast.getDaily();

            var embed = new EmbedBuilder();
            embed.setTitle("Current Weather");
            embed.setDescription(String.format("Weather for %s", result.getName()));
            embed.addField("Temperature", String.format("%.2f°C", currentWeather.getTemperature()), true);
            embed.addField("Wind Speed", String.format("%.2f km/h", currentWeather.getWindspeed()), true);
            embed.addField("Wind Direction", String.format("%d°", currentWeather.getWinddirection()), true);

            int days = dailyForecast.getDays();
            for (int dayNumber = 0; dayNumber < days; dayNumber++) {
                ByDay dayForecast = dailyForecast.getByDay(dayNumber);
                String forecastString = """
                        Temperature: %.2f%s - %.2f%s
                        Apparent Temperature: %.2f%s - %.2f%s
                        Precipitation: %.2f %s
                        Rain: %.2f %s
                        Showers: %.2f %s
                        Snowfall: %.2f %s
                        Wind Speed: %.2f %s
                        Wind Gusts: %.2f %s
                        """.formatted(dayForecast.temperature_2m_min(), dailyUnits.getTemperature_2m_min(),
                        dayForecast.temperature_2m_max(), dailyUnits.getTemperature_2m_max(),
                        dayForecast.apparent_temperature_min(), dailyUnits.getApparent_temperature_min(),
                        dayForecast.apparent_temperature_max(), dailyUnits.getApparent_temperature_max(),
                        dayForecast.precipitation_sum(), dailyUnits.getPrecipitation_sum(),
                        dayForecast.rain_sum(), dailyUnits.getRain_sum(),
                        dayForecast.showers_sum(), dailyUnits.getShowers_sum(),
                        dayForecast.snowfall_sum(), dailyUnits.getSnowfall_sum(),
                        dayForecast.windspeed_10m_max(), dailyUnits.getWindspeed_10m_max(),
                        dayForecast.windgusts_10m_max(), dailyUnits.getWindgusts_10m_max());

                // get the day as a week day
                String day = LocalDate.from(
                        Instant.now()
                                .plus(dayNumber, ChronoUnit.DAYS)
                                .atZone(ZoneId.of(forecast.getTimezone())))
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, Locale.ROOT);
                embed.addField(day, forecastString, false);
            }

            return embed;
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while getting the weather!", exception);

            return new EmbedBuilder()
                    .setTitle("❌ An error occurred!")
                    .setDescription("An error occurred while getting the weather!")
                    .setColor(Color.RED);
        }
    }

    @Data
    public static class GeocodeResponse {
        private List<GeocodeResult> results;
        private float generationtime_ms;
    }

    @Data
    public static class GeocodeResult {
        private long id;
        private String name;
        private float latitude;
        private float longitude;
        private float elevation;
        private String feature_code;
        private String country_code;
        private long admin1_id;
        private long admin2_id;
        private long admin3_id;
        private long admin4_id;
        private String timezone;
        private long population;
        private String[] postcodes;
        private long country_id;
        private String country;
        private String admin1;
        private String admin2;
        private String admin3;
        private String admin4;
    }

    @Data
    public static class OpenMeteoWeatherForecast {
        private float latitude;
        private float longitude;
        private float generationtime_ms;
        private int utc_offset_seconds;
        private String timezone;
        private String timezone_abbreviation;
        private float elevation;
        private CurrentWeather current_weather;
        private HourlyUnits hourly_units;
        private HourlyForecast hourly;
        private DailyUnits daily_units;
        private DailyForecast daily;
    }

    @Data
    public static class CurrentWeather {
        private float temperature;
        private float windspeed;
        private int winddirection;
        private int weathercode;
        private int is_day;
        private String time;
    }

    @Data
    public static class HourlyUnits {
        private String time;
        private String temperature_2m;
        private String relativehumidity_2m;
        private String apparent_temperature;
        private String precipitation_probability;
        private String precipitation;
        private String rain;
        private String showers;
        private String snowfall;
        private String windspeed_10m;
        private String winddirection_10m;
    }

    @Data
    public static class HourlyForecast {
        private List<String> time;
        private List<Float> temperature_2m;
        private List<Integer> relativehumidity_2m;
        private List<Float> apparent_temperature;
        private List<Integer> precipitation_probability;
        private List<Float> precipitation;
        private List<Float> rain;
        private List<Float> showers;
        private List<Float> snowfall;
        private List<Float> windspeed_10m;
        private List<Integer> winddirection_10m;

        /**
         * Gets the forecast for a specific hour.
         *
         * @param hour The hour to get the forecast for.
         * @return The forecast for the specified hour.
         */
        public ByHour getByHour(int hour) {
            String time = this.time.get(hour);
            float temperature_2m = this.temperature_2m.get(hour);
            int relativehumidity_2m = this.relativehumidity_2m.get(hour);
            float apparent_temperature = this.apparent_temperature.get(hour);
            int precipitation_probability = this.precipitation_probability.get(hour);
            float precipitation = this.precipitation.get(hour);
            float rain = this.rain.get(hour);
            float showers = this.showers.get(hour);
            float snowfall = this.snowfall.get(hour);
            float windspeed_10m = this.windspeed_10m.get(hour);
            int winddirection_10m = this.winddirection_10m.get(hour);

            return new ByHour(time,
                    temperature_2m,
                    relativehumidity_2m,
                    apparent_temperature,
                    precipitation_probability,
                    precipitation,
                    rain,
                    showers,
                    snowfall,
                    windspeed_10m,
                    winddirection_10m);
        }
    }

    public record ByHour(String time, float temperature_2m, int relativehumidity_2m, float apparent_temperature,
                         int precipitation_probability, float precipitation, float rain, float showers, float snowfall,
                         float windspeed_10m, int winddirection_10m) {
    }

    @Data
    public static class DailyUnits {
        private String time;
        private String temperature_2m_max;
        private String temperature_2m_min;
        private String apparent_temperature_max;
        private String apparent_temperature_min;
        private String precipitation_sum;
        private String rain_sum;
        private String showers_sum;
        private String snowfall_sum;
        private String windspeed_10m_max;
        private String windgusts_10m_max;
    }

    @Data
    public static class DailyForecast {
        private List<String> time;
        private List<Float> temperature_2m_max;
        private List<Float> temperature_2m_min;
        private List<Float> apparent_temperature_max;
        private List<Float> apparent_temperature_min;
        private List<Float> precipitation_sum;
        private List<Float> rain_sum;
        private List<Float> showers_sum;
        private List<Float> snowfall_sum;
        private List<Float> windspeed_10m_max;
        private List<Float> windgusts_10m_max;

        /**
         * Gets the forecast for a specific day.
         *
         * @param day The day to get the forecast for.
         * @return The forecast for the specified day.
         */
        public ByDay getByDay(int day) {
            String time = this.time.get(day);
            float temperature_2m_max = this.temperature_2m_max.get(day);
            float temperature_2m_min = this.temperature_2m_min.get(day);
            float apparent_temperature_max = this.apparent_temperature_max.get(day);
            float apparent_temperature_min = this.apparent_temperature_min.get(day);
            float precipitation_sum = this.precipitation_sum.get(day);
            float rain_sum = this.rain_sum.get(day);
            float showers_sum = this.showers_sum.get(day);
            float snowfall_sum = this.snowfall_sum.get(day);
            float windspeed_10m_max = this.windspeed_10m_max.get(day);
            float windgusts_10m_max = this.windgusts_10m_max.get(day);

            return new ByDay(time,
                    temperature_2m_max,
                    temperature_2m_min,
                    apparent_temperature_max,
                    apparent_temperature_min,
                    precipitation_sum,
                    rain_sum,
                    showers_sum,
                    snowfall_sum,
                    windspeed_10m_max,
                    windgusts_10m_max);
        }

        /**
         * @return The minimum number of days that the forecast is for.
         */
        public int getDays() {
            int days = 100;

            if(days > time.size())
                days = time.size();
            if (days > temperature_2m_max.size())
                days = temperature_2m_max.size();
            if (days > temperature_2m_min.size())
                days = temperature_2m_min.size();
            if (days > apparent_temperature_max.size())
                days = apparent_temperature_max.size();
            if (days > apparent_temperature_min.size())
                days = apparent_temperature_min.size();
            if (days > precipitation_sum.size())
                days = precipitation_sum.size();
            if (days > rain_sum.size())
                days = rain_sum.size();
            if (days > showers_sum.size())
                days = showers_sum.size();
            if (days > snowfall_sum.size())
                days = snowfall_sum.size();
            if (days > windspeed_10m_max.size())
                days = windspeed_10m_max.size();
            if (days > windgusts_10m_max.size())
                days = windgusts_10m_max.size();

            return days;
        }
    }

    public record ByDay(String time, float temperature_2m_max, float temperature_2m_min, float apparent_temperature_max,
                        float apparent_temperature_min, float precipitation_sum, float rain_sum, float showers_sum,
                        float snowfall_sum, float windspeed_10m_max, float windgusts_10m_max) {
    }
}
