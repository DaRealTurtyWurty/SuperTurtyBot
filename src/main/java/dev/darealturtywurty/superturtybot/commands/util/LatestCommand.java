package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class LatestCommand extends CoreCommand {
    private static final String MINECRAFT_PISTON_META =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2" + ".json";
    private static final String FORGE_PROMOS = "https://files.minecraftforge" + ".net/net/minecraftforge/forge" +
            "/promotions_slim.json";

    public LatestCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("minecraft", "Get the latest Minecraft version"),
                new SubcommandData("forge", "Get the latest Forge version"),
                new SubcommandData("fabric", "Get the latest Fabric version"),
                new SubcommandData("parchment", "Get the latest Parchment version"));
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();

        try {
            switch (event.getSubcommandName()) {
                case "minecraft" -> {
                    JsonObject json = Constants.GSON.fromJson(
                            new InputStreamReader(new URL(MINECRAFT_PISTON_META).openStream()), JsonObject.class);

                    JsonObject latest = json.getAsJsonObject("latest");
                    String latestVersion = latest.get("release").getAsString();
                    String latestSnapshot = latest.get("snapshot").getAsString();

                    event.getHook().editOriginalEmbeds(new EmbedBuilder().setTitle("Latest Minecraft Versions")
                                                                         .addField("Release", latestVersion, true)
                                                                         .addField("Snapshot", latestSnapshot, true)
                                                                         .build()).queue();
                }
                case "forge" -> {
                    JsonObject promos = Constants.GSON.fromJson(
                                                         new InputStreamReader(new URL(FORGE_PROMOS).openStream()),
                                                         JsonObject.class)
                                                      .getAsJsonObject("promos");

                    // get the last element in the array
                    List<Map.Entry<String, String>> versions = promos.entrySet().stream()
                                                                     .map(entry -> Map.entry(entry.getKey(),
                                                                             entry.getValue().getAsString()))
                                                                     .sorted(Map.Entry.comparingByKey()).toList();

                    Map.Entry<String, String> latestRecommended = versions.get(versions.size() - 1);
                    String latestRecommendedVersion = latestRecommended.getKey().endsWith(
                            "-recommended") ? (latestRecommended.getKey() + "-" + latestRecommended.getValue()) : null;

                    Map.Entry<String, String> latestLatest = versions.get(versions.size() - 2);
                    String latestLatestVersion = latestLatest.getKey().endsWith(
                            "-latest") ? (latestLatest.getKey() + "-" + latestLatest.getValue()) : null;

                    var embed = new EmbedBuilder().setTitle("Latest Forge Versions");
                    if (latestRecommendedVersion != null) {
                        embed.addField("Recommended", latestRecommendedVersion.replace("-recommended", ""), true);
                    }

                    if (latestLatestVersion != null) {
                        embed.addField("Latest", latestLatestVersion.replace("-latest", ""), true);
                    }

                    event.getHook().editOriginalEmbeds(embed.build()).queue();
                }
            }
        } catch (IOException exception) {
            event.getHook().editOriginal("An error occurred while getting the latest version!").queue();
        }
    }
}
