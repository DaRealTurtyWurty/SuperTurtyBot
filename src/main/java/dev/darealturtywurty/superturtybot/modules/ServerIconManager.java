package dev.darealturtywurty.superturtybot.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ServerIconManager {
    private static Path ICONS_PATH = null;
    private static boolean RUNNING = false;

    private static final Map<Long, IconCalendar> CALENDARS = new HashMap<>();
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    public static void start(JDA jda) {
        if (RUNNING)
            return;

        RUNNING = true;
        readIcons();

        Map<Long, Path> yesterdayIcons = new HashMap<>();
        EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            Calendar now = Calendar.getInstance();
            for (Map.Entry<Long, IconCalendar> entry : CALENDARS.entrySet()) {
                Guild guild = jda.getGuildById(entry.getKey());
                if (guild == null)
                    continue;

                IconCalendar calendar = entry.getValue();
                if (calendar.icons.isEmpty())
                    continue;

                Date nowDate = now.getTime();
                Path iconPath = calendar.getIcon(nowDate);

                try {
                    Icon icon;
                    if (yesterdayIcons.containsKey(entry.getKey())) {
                        Path yesterdayIcon = yesterdayIcons.get(entry.getKey());
                        if (iconPath.equals(yesterdayIcon))
                            continue;

                        icon = Icon.from(Files.readAllBytes(iconPath));
                    } else {
                        icon = Icon.from(Files.readAllBytes(calendar.defaultIcon));
                    }

                    guild.getManager().setIcon(icon).queue();
                } catch (IOException exception) {
                    Constants.LOGGER.error("An error occurred while setting the server icon for guild: {}", guild.getId(), exception);
                }
                
                yesterdayIcons.put(entry.getKey(), iconPath);
            }
        }, 0, 1, TimeUnit.DAYS);
    }

    public static void setIconsPath(@NotNull Path path) {
        if (path == null)
            throw new IllegalArgumentException("The server icons path: \"" + path + "\" cannot be null!");
        if (Files.notExists(path))
            throw new IllegalArgumentException("The server icons path: \"" + path + "\" does not exist!");
        if (!path.toString().endsWith(".json"))
            throw new IllegalArgumentException("The server icons path: \"" + path + "\" must be a JSON file!");

        ICONS_PATH = path;
    }

    private static void readIcons() {
        if (ICONS_PATH == null)
            throw new IllegalStateException("The server icons path has not been set!");

        if (Files.notExists(ICONS_PATH))
            throw new IllegalStateException("The server icons path: \"" + ICONS_PATH + "\" does not exist!");

        try {
            JsonObject obj = Constants.GSON.fromJson(Files.readString(ICONS_PATH), JsonObject.class);
            for (String key : obj.keySet()) {
                long guildId = Long.parseLong(key);
                CALENDARS.put(guildId, IconCalendar.fromJson(obj.getAsJsonObject(key)));
            }
        } catch (IOException exception) {
            Constants.LOGGER.error("An error occurred while reading the server icons file!", exception);
        }
    }

    public record IconCalendar(Path defaultIcon, Map<DateRange, List<Path>> icons) {
        public Path getIcon(Date date) {
            for (DateRange range : this.icons.keySet()) {
                if (range.contains(date)) {
                    List<Path> paths = this.icons.get(range);
                    return paths.get(ThreadLocalRandom.current().nextInt(paths.size()));
                }
            }

            return this.defaultIcon;
        }

        public static JsonObject toJson(IconCalendar calendar) {
            var obj = new JsonObject();
            obj.addProperty("defaultIcon", calendar.defaultIcon.toString());
            var icons = new JsonArray();
            calendar.icons.forEach((range, paths) -> {
                var icon = new JsonObject();
                icon.add("range", DateRange.toJson(range));
                if (paths.size() == 1) {
                    icon.addProperty("path", paths.getFirst().toString());
                } else {
                    var pathsArray = new JsonArray();
                    paths.forEach(path -> pathsArray.add(path.toString()));
                    icon.add("paths", pathsArray);
                }

                icons.add(icon);
            });
            obj.add("icons", icons);
            return obj;
        }

        public static IconCalendar fromJson(JsonObject obj) {
            Path defaultIcon = Path.of(obj.get("defaultIcon").getAsString());

            Map<DateRange, List<Path>> icons = new HashMap<>();
            JsonArray iconsArray = obj.getAsJsonArray("icons");
            for (JsonElement jsonElement : iconsArray) {
                JsonObject icon = jsonElement.getAsJsonObject();
                DateRange range = DateRange.fromJson(icon.getAsJsonObject("range"));

                List<Path> paths = new ArrayList<>();
                if (icon.has("path")) {
                    paths.add(Path.of(icon.get("path").getAsString()));
                } else {
                    JsonArray pathsArray = icon.getAsJsonArray("paths");
                    for (JsonElement path : pathsArray) {
                        paths.add(Path.of(path.getAsString()));
                    }
                }

                icons.put(range, paths);
            }

            return new IconCalendar(defaultIcon, icons);
        }

        public record DateRange(DayOfYear start, int length) {
            public boolean contains(int month, int day) {
                return this.start.isBefore(new DayOfYear(month, day)) && this.start.isAfter(new DayOfYear(month, day));
            }

            public boolean contains(Date date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return this.contains(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            }

            public Date getStartAsDate() {
                return this.start.getNextAsDate();
            }

            public Date getEndAsDate() {
                Calendar cal = Calendar.getInstance();
                cal.setTime(this.start.getNextAsDate());
                cal.add(Calendar.DAY_OF_MONTH, this.length);
                return cal.getTime();
            }

            public boolean isBetween(Date start, Date end) {
                return contains(start) && contains(end);
            }

            public static JsonObject toJson(DateRange range) {
                var obj = new JsonObject();
                obj.add("start", DayOfYear.toJson(range.start));
                obj.addProperty("length", range.length);
                return obj;
            }

            public static DateRange fromJson(JsonObject obj) {
                return new DateRange(DayOfYear.fromJson(obj.getAsJsonObject("start")), obj.get("length").getAsInt());
            }

            public static String toString(DateRange range) {
                return range.start.month + "/" + range.start.day + " - " + range.length;
            }

            public static DateRange fromString(String str) {
                String[] parts = str.split(" - ");
                String[] date = parts[0].split("/");
                return new DateRange(new DayOfYear(Integer.parseInt(date[0]), Integer.parseInt(date[1])), Integer.parseInt(parts[1]));
            }

            public record DayOfYear(int month, int day) {
                public boolean isBefore(DayOfYear other) {
                    return this.month < other.month || (this.month == other.month && this.day < other.day);
                }

                public boolean isAfter(DayOfYear other) {
                    return this.month > other.month || (this.month == other.month && this.day > other.day);
                }

                public boolean isBetween(DayOfYear start, DayOfYear end) {
                    return this.isAfter(start) && this.isBefore(end);
                }

                public Date getNextAsDate() {
                    Calendar now = Calendar.getInstance();

                    Calendar next = Calendar.getInstance();
                    next.set(Calendar.MONTH, this.month - 1);
                    next.set(Calendar.DAY_OF_MONTH, this.day);

                    if (next.before(now)) {
                        next.add(Calendar.YEAR, 1);
                    }

                    return next.getTime();
                }

                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    DayOfYear dayOfYear = (DayOfYear) obj;
                    return month == dayOfYear.month && day == dayOfYear.day;
                }

                public static JsonObject toJson(DayOfYear day) {
                    var obj = new JsonObject();
                    obj.addProperty("month", day.month);
                    obj.addProperty("day", day.day);
                    return obj;
                }

                public static DayOfYear fromJson(JsonObject obj) {
                    return new DayOfYear(obj.get("month").getAsInt(), obj.get("day").getAsInt());
                }
            }
        }
    }
}
