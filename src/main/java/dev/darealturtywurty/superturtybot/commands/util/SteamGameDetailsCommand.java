package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SteamGameDetailsCommand extends CoreCommand {
    public SteamGameDetailsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "app-id", "The steam app id.", true)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the app/game details of the specified app id";
    }

    @Override
    public String getName() {
        return "steam-game-details";
    }

    @Override
    public String getRichName() {
        return "Steam Game Details";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (Environment.INSTANCE.steamKey().isEmpty()) {
            reply(event, "❌ This command has been disabled by the bot owner!", false, true);
            Constants.LOGGER.warn("Steam API key is not set!");
            return;
        }

        String appID = event.getOption("app-id", null, OptionMapping::getAsString);
        if (appID == null) {
            reply(event, "❌ You must provide a Steam App ID!", false, true);
            return;
        }

        event.deferReply().queue();

        String gameDetailsUrl = "https://store.steampowered.com/api/appdetails?appids=%s&l=english&cc=us"
                .formatted(appID);
        String playerCountUrl = "https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?key=%s&appid=%s"
                .formatted(Environment.INSTANCE.steamKey().get(), appID);

        final Request gameDetailsRequest = new Request.Builder().url(gameDetailsUrl).get().build();
        final Request playerCountRequest = new Request.Builder().url(playerCountUrl).get().build();

        try (Response gameDetailsResponse = new OkHttpClient().newCall(gameDetailsRequest).execute();
             Response playerCountResponse = new OkHttpClient().newCall(playerCountRequest).execute()) {
            if (!gameDetailsResponse.isSuccessful()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            if (!playerCountResponse.isSuccessful()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            ResponseBody gameDetailsBody = gameDetailsResponse.body();
            if (gameDetailsBody == null) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            ResponseBody playerCountBody = playerCountResponse.body();
            if (playerCountBody == null) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            String gameDetailsStr = gameDetailsBody.string();
            if (gameDetailsStr.isBlank()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            String playerCountStr = playerCountBody.string();
            if (playerCountStr.isBlank()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            SteamAppDetailsResponse appDetailsDataResponse = SteamAppDetailsResponse.fromJsonString(gameDetailsStr, appID);
            AppPlayerCountResponse playerCountDataResponse = AppPlayerCountResponse.fromJsonString(playerCountStr);

            AppDetailsData appDetailsData = appDetailsDataResponse.getData();
            int playerCount = playerCountDataResponse.getPlayer_count();

            if (!appDetailsDataResponse.isSuccess()) {
                event.getHook()
                        .sendMessage("❌ Failed to get steam app-id!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            if (playerCountDataResponse.getResult() == 0) {
                event.getHook()
                        .sendMessage("❌ Failed to get player count!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            StringBuilder categories = new StringBuilder();
            if (!appDetailsData.getCategories().isEmpty()) {
                for (AppDetailsData.Category category : appDetailsData.getCategories()) {
                    categories.append("`").append(category.description).append("`").append(", ");
                }
            }

            StringBuilder genres = new StringBuilder();
            if (!appDetailsData.getGenres().isEmpty()) {
                for (AppDetailsData.Genre genre : appDetailsData.getGenres()) {
                    genres.append("`").append(genre.getDescription()).append("`").append(", ");
                }
            }

            StringBuilder developers = new StringBuilder();
            if (!appDetailsData.getDevelopers().isEmpty()) {
                for (String developer : appDetailsData.getDevelopers()) {
                    developers.append("`").append(developer).append("`").append(", ");
                }
            }

            StringBuilder publishers = new StringBuilder();
            if (!appDetailsData.getPublishers().isEmpty()) {
                for (String publisher : appDetailsData.getPublishers()) {
                    publishers.append("`").append(publisher).append("`").append(", ");
                }
            }

            var embed = new EmbedBuilder()
                    .setTitle(appDetailsData.getName(), appDetailsData.getWebsite())
                    .setThumbnail(appDetailsData.getCapsule_imagev5())
                    .setDescription(appDetailsData.getShort_description())
                    .setImage(appDetailsData.getHeader_image())
                    .setFooter("Requested by %s".formatted(event.getUser().getEffectiveName()),
                            event.getUser().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            if (!categories.isEmpty()) {
                embed.addField("Categories", categories.substring(0, categories.length() - 2), true);
            }

            if (!genres.isEmpty()) {
                embed.addField("Genres", genres.substring(0, genres.length() - 2), true);
            }

            if (!developers.isEmpty()) {
                embed.addField("Developers", developers.substring(0, developers.length() - 2), false);
            }

            if (!publishers.isEmpty()) {
                embed.addField("Publishers", publishers.substring(0, publishers.length() - 2), true);
            }

            embed.addField("Platforms", """
                    Windows: %s
                    Mac: %s
                    Linux: %s""".formatted(
                    StringUtils.trueFalseToYesNo(appDetailsData.getPlatforms().isWindows()),
                    StringUtils.trueFalseToYesNo(appDetailsData.getPlatforms().isMac()),
                    StringUtils.trueFalseToYesNo(appDetailsData.getPlatforms().isLinux())), false);

            if (appDetailsData.getPrice_overview() != null && !appDetailsData.is_free()) {
                embed.addField("Price", appDetailsData.getPrice_overview().getFinal_formatted(), true);
            } else {
                embed.addField("Price", "Free", true);
            }

            embed.addField("Player Count", String.valueOf(playerCount), true);
            embed.addField("Release Date", appDetailsData.getRelease_date().getDate(), true);

            List<EmbedBuilder> screenshotEmbeds = new ArrayList<>();

            List<AppDetailsData.Screenshot> screenshots = appDetailsData.getScreenshots();
            if (!screenshots.isEmpty()) {
                AppDetailsData.Screenshot screenshot0 = screenshots.get(0);
                var screenshot0Embed = new EmbedBuilder()
                        .setTitle("Screenshots")
                        .setUrl(appDetailsData.getWebsite() == null ?
                                "https://store.steampowered.com/app/%s".formatted(appDetailsData.getSteam_appid()) :
                                appDetailsData.getWebsite() + "/")
                        .setImage(screenshot0.getPath_full())
                        .setFooter("Requested by %s".formatted(event.getUser().getEffectiveName()),
                                event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now());
                screenshotEmbeds.add(screenshot0Embed);

                appDetailsData.getScreenshots().subList(1, Math.min(appDetailsData.getScreenshots().size(), 4)).forEach(screenshot -> {
                    var screenshotEmbed = new EmbedBuilder()
                            .setUrl(appDetailsData.getWebsite() == null ?
                                    "https://store.steampowered.com/app/%s".formatted(appDetailsData.getSteam_appid()) :
                                    appDetailsData.getWebsite() + "/")
                            .setImage(screenshot.getPath_full())
                            .setFooter("Requested by %s".formatted(event.getUser().getEffectiveName()),
                                    event.getUser().getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now());
                    screenshotEmbeds.add(screenshotEmbed);
                });
            }

            List<MessageEmbed> embeds = new ArrayList<>();
            embeds.add(embed.build());
            embeds.addAll(screenshotEmbeds.stream().map(EmbedBuilder::build).toList());
            event.getHook().sendMessageEmbeds(embeds).mentionRepliedUser(false).queue();
        } catch (IOException exception) {
            reply(event, "Failed to response!");
            Constants.LOGGER.error("Failed to get response!", exception);
        }
    }

    @Data
    public static class SteamAppDetailsResponse {
        private boolean success;
        private AppDetailsData data;

        private static SteamAppDetailsResponse fromJsonString(String json, String appid) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object.get(appid), SteamAppDetailsResponse.class);
        }
    }

    @Data
    public static class AppDetailsData {
        private String type;
        private String name;
        private int steam_appid;
        private int required_age;
        private boolean is_free;
        private List<Integer> dlc = new ArrayList<>();
        private String detailed_description;
        private String about_the_game;
        private String short_description;
        private String supported_languages;
        private String header_image;
        private String capsule_image;
        private String capsule_imagev5;
        private String website;
        private List<String> developers = new ArrayList<>();
        private List<String> publishers = new ArrayList<>();
        private PriceOverview price_overview;
        private List<Integer> packages = new ArrayList<>();
        private List<PackageGroup> package_groups = new ArrayList<>();
        private Platforms platforms;
        private List<Category> categories = new ArrayList<>();
        private List<Genre> genres = new ArrayList<>();
        private List<Screenshot> screenshots = new ArrayList<>();
        private List<Movie> movies = new ArrayList<>();
        private Recommendations recommendations;
        private ReleaseDate release_date;
        private SupportInfo support_info;
        private String background;
        private String background_raw;
        private ContentDescriptors content_descriptors;

        @Data
        public static class PriceOverview {
            private String currency;
            private int initial;
            private int final_;
            private int discount_percent;
            private String initial_formatted;
            private String final_formatted;
        }

        @Data
        public static class PackageGroup {
            private String name;
            private String title;
            private String description;
            private String selection_text;
            private String save_text;
            private int display_type;
            private String is_recurring_subscription;
            private List<Sub> subs = new ArrayList<>();

            @Data
            public static class Sub {
                private int packageid;
                private String percent_savings_text;
                private int percent_savings;
                private String option_text;
                private String option_description;
                private String can_get_free_license;
                private boolean is_free_license;
                private int price_in_cents_with_discount;
            }
        }

        @Data
        public static class Platforms {
            private boolean windows;
            private boolean mac;
            private boolean linux;
        }

        @Data
        public static class Category {
            private int id;
            private String description;
        }

        @Data
        public static class Genre {
            private String id;
            private String description;
        }

        @Data
        public static class Screenshot {
            private int id;
            private String path_thumbnail;
            private String path_full;
        }

        @Data
        public static class Movie {
            private int id;
            private String name;
            private String thumbnail;
            private Webm webm;
            private Mp4 mp4;
            private boolean highlight;

            @Data
            public static class Webm {
                private String _480;
                private String max;
            }

            @Data
            public static class Mp4 {
                private String _480;
                private String max;
            }
        }

        @Data
        public static class Recommendations {
            private int total;
        }

        @Data
        public static class ReleaseDate {
            private boolean coming_soon;
            private String date;
        }

        @Data
        public static class SupportInfo {
            private String url;
            private String email;
        }

        @Data
        public static class ContentDescriptors {
            private List<Integer> ids = new ArrayList<>();
            private String notes;
        }
    }

    @Data
    public static class AppPlayerCountResponse {
        private int player_count;
        private int result;

        private static AppPlayerCountResponse fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object.get("response"), AppPlayerCountResponse.class);
        }
    }
}
