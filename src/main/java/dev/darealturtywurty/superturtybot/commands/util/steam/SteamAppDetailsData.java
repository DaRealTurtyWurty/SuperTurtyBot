package dev.darealturtywurty.superturtybot.commands.util.steam;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SteamAppDetailsData {
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