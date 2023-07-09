package dev.darealturtywurty.superturtybot.commands.util;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.io.FileUtils;
import org.json.XML;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LatestCommand extends CoreCommand {
    private static final String MINECRAFT_PISTON_META = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String FORGE_PROMOS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String FABRIC_LOADER_META = "https://meta.fabricmc.net/v2/versions/loader";
    private static final String PARCHMENT_MAVEN_META = "https://ldtteam.jfrog.io/artifactory/parchmentmc-public/org/parchmentmc/data/parchment-%s/maven-metadata.xml";

    public LatestCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("minecraft", "Get the latest Minecraft version"),
                new SubcommandData("forge", "Get the latest Forge version"),
                new SubcommandData("fabric", "Get the latest Fabric version"),
                new SubcommandData("parchment", "Get the latest Parchment version"),
                new SubcommandData("all", "Get the latest versions of all"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Get the latest version of Forge, Fabric, Parchment, or Minecraft";
    }

    @Override
    public String getName() {
        return "latest";
    }

    @Override
    public String getRichName() {
        return "Latest";
    }

    @Override
    public org.apache.commons.lang3.tuple.Pair<TimeUnit, Long> getRatelimit() {
        return org.apache.commons.lang3.tuple.Pair.of(TimeUnit.SECONDS, 5L);
    }

    private static Pair<String, String> getMinecraftVersions() {
        try {
            JsonObject json = Constants.GSON.fromJson(
                    new InputStreamReader(new URL(MINECRAFT_PISTON_META).openStream()), JsonObject.class);

            JsonObject latest = json.getAsJsonObject("latest");
            String latestVersion = latest.get("release").getAsString();
            String latestSnapshot = latest.get("snapshot").getAsString();

            return new Pair<>(latestVersion, latestSnapshot);
        } catch (IOException exception) {
            exception.printStackTrace();
            return new Pair<>("Unknown", "Unknown");
        }
    }

    private static LinkedHashMap<String, Boolean> getAllMinecraftVersions() {
        try {
            JsonObject json = Constants.GSON.fromJson(
                    new InputStreamReader(new URL(MINECRAFT_PISTON_META).openStream()), JsonObject.class);

            JsonArray versions = json.getAsJsonArray("versions");
            LinkedHashMap<String, Boolean> versionsMap = Maps.newLinkedHashMap();
            for (JsonElement version : versions) {
                JsonObject versionObject = version.getAsJsonObject();
                versionsMap.put(versionObject.get("id").getAsString(),
                        versionObject.get("type").getAsString().equals("release"));
            }

            return versionsMap;
        } catch (IOException exception) {
            exception.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    private static Pair<String, String> getForgeVersions() {
        try {
            JsonObject promos = Constants.GSON.fromJson(new InputStreamReader(new URL(FORGE_PROMOS).openStream()),
                    JsonObject.class).getAsJsonObject("promos");

            List<Map.Entry<String, JsonElement>> elements = new ArrayList<>(promos.entrySet().stream()
                    .sorted(Comparator.comparingInt(
                            entry -> Integer.parseInt(entry.getKey().split("\\.")[1].split("-")[0]))).toList());
            Collections.reverse(elements);

            Map.Entry<String, JsonElement> latestRecommended = elements.stream()
                    .filter(entry -> entry.getKey().contains("-recommended")).findFirst().orElse(elements.get(0));
            Map.Entry<String, JsonElement> latestLatest = elements.stream()
                    .filter(entry -> entry.getKey().contains("-latest")).findFirst().orElse(elements.get(0));

            String latestRecommendedVersion = latestRecommended.getKey() + "-" + latestRecommended.getValue()
                    .getAsString();
            if (latestRecommendedVersion.contains("-recommended")) {
                latestRecommendedVersion = latestRecommendedVersion.replace("-recommended", "");
            } else {
                latestRecommendedVersion = "None";
            }

            String latestLatestVersion = latestLatest.getKey() + "-" + latestLatest.getValue().getAsString();
            if (latestLatestVersion.contains("-latest")) {
                latestLatestVersion = latestLatestVersion.replace("-latest", "");
            } else {
                latestLatestVersion = "None";
            }

            return new Pair<>(latestRecommendedVersion, latestLatestVersion);
        } catch (IOException exception) {
            exception.printStackTrace();
            return new Pair<>("Unknown", "Unknown");
        }
    }

    private static Pair<String, String> getFabricVersions() {
        try {
            JsonArray json = Constants.GSON.fromJson(new InputStreamReader(new URL(FABRIC_LOADER_META).openStream()),
                    JsonArray.class);

            String latestLatest = json.get(0).getAsJsonObject().get("version").getAsString();
            String latestStable;
            for (JsonElement jsonElement : json) {
                if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("stable")) {
                    boolean stable = jsonElement.getAsJsonObject().get("stable").getAsBoolean();
                    if (stable) {
                        latestStable = jsonElement.getAsJsonObject().get("version").getAsString();
                        return new Pair<>(latestStable, latestLatest);
                    }
                }
            }

            return new Pair<>("None", latestLatest);
        } catch (IOException exception) {
            exception.printStackTrace();
            return new Pair<>("Unknown", "Unknown");
        }
    }

    private static String getParchmentVersion(String mcVersion) {
        try {
            String parchmentUrl = String.format(PARCHMENT_MAVEN_META, mcVersion);
            var file = new File(mcVersion + "-parchment.xml");
            FileUtils.copyURLToFile(new URL(parchmentUrl), file);

            final String xmlJsonStr = XML.toJSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8))
                    .toString(1);
            Files.deleteIfExists(file.toPath());

            final JsonObject xmlJson = Constants.GSON.fromJson(xmlJsonStr, JsonObject.class);
            final JsonObject versioning = xmlJson.getAsJsonObject("metadata").getAsJsonObject("versioning");
            final JsonArray versionsArray = versioning.getAsJsonObject("versions").getAsJsonArray("version");

            List<String> results = new ArrayList<>();
            for (final JsonElement element : versionsArray) {
                final String version = element.getAsString();
                if (!Pattern.matches("\\d+\\.\\d+(\\.\\d+)?", version)) continue;

                results.add(version);
            }

            results.sort(Comparator.comparingInt(entry -> {
                String[] split = entry.split("\\.");
                return Integer.parseInt(split[0]) * 10000 + Integer.parseInt(
                        split[1]) * 100 + (split.length > 2 ? Integer.parseInt(split[2]) : 0);
            }));

            return results.get(results.size() - 1);
        } catch (IOException exception) {
            return "Unknown";
        }
    }

    private static String findLatestParchment(String latestMinecraftVersion) {
        String latestVersion = getParchmentVersion(latestMinecraftVersion);

        int increment = 0;
        LinkedHashMap<String, Boolean> versions = null;
        while (latestVersion.equals("Unknown")) {
            if (versions == null) {
                versions = getAllMinecraftVersions();
            }

            latestMinecraftVersion = versions.keySet().stream().skip(increment++).findFirst().orElse(null);
            if (latestMinecraftVersion == null) {
                break;
            }

            latestVersion = getParchmentVersion(latestMinecraftVersion);
        }

        return latestMinecraftVersion + "-" + latestVersion;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();

        switch (event.getSubcommandName()) {
            case "minecraft" -> {
                Pair<String, String> versions = getMinecraftVersions();

                event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("Latest Minecraft Versions")
                        .addField("Release", versions.getFirst(), true).addField("Snapshot", versions.getSecond(), true)
                        .build()).queue();
            }
            case "forge" -> {
                Pair<String, String> versions = getForgeVersions();

                event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("Latest Forge Versions")
                        .addField("Recommended", versions.getFirst(), true)
                        .addField("Latest", versions.getSecond(), true).build()).queue();
            }
            case "fabric" -> {
                Pair<String, String> versions = getFabricVersions();

                event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("Latest Fabric Versions")
                        .addField("Stable", versions.getFirst(), true).addField("Latest", versions.getSecond(), true)
                        .build()).queue();
            }
            case "parchment" -> {
                String latestMinecraftVersion = getMinecraftVersions().getFirst();
                String latestVersion = findLatestParchment(latestMinecraftVersion);

                event.getHook().editOriginalEmbeds(
                                new EmbedBuilder().setTitle("Latest Parchment Version").setDescription(latestVersion).build())
                        .queue();
            }
            case "all" -> {
                Pair<String, String> minecraftVersions = getMinecraftVersions();
                Pair<String, String> forgeVersions = getForgeVersions();
                Pair<String, String> fabricVersions = getFabricVersions();
                String latestMinecraftVersion = minecraftVersions.getFirst();
                String latestParchmentVersion = findLatestParchment(latestMinecraftVersion);

                event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("Latest Versions").addField("Minecraft",
                                "Release: " + minecraftVersions.getFirst() + "\nSnapshot: " + minecraftVersions.getSecond(),
                                false).addField("Forge",
                                "Recommended: " + forgeVersions.getFirst() + "\nLatest: " + forgeVersions.getSecond(), false)
                        .addField("Fabric",
                                "Stable: " + fabricVersions.getFirst() + "\nLatest: " + fabricVersions.getSecond(),
                                false).addField("Parchment", latestParchmentVersion, false).build()).queue();
            }
        }
    }
}
