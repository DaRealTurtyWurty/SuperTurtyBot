package dev.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

// TODO: Take a look at the searching for this command. If it can't be fixed,
// add pagination
public class CurseforgeCommand extends CoreCommand {
    public CurseforgeCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "project", "The name of the curseforge project", true));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Get stats about a curseforge project";
    }
    
    @Override
    public String getHowToUse() {
        return "/curseforge [projectName]";
    }
    
    @Override
    public String getName() {
        return "curseforge";
    }
    
    @Override
    public String getRichName() {
        return "Curseforge Project";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String search = URLEncoder.encode(event.getOption("project").getAsString(), StandardCharsets.UTF_8);
        final JsonObject mod = getMod(listMods(search));
        event.deferReply().addEmbeds(createModEmbed(mod, search).build()).mentionRepliedUser(false).queue();
    }
    
    private EmbedBuilder createModEmbed(JsonObject modObj, String search) {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.YELLOW);
        if (modObj == null) {
            embed.setTitle("No results found for: `" + search + "`");
            embed.setColor(Color.RED);
            return embed;
        }
        
        embed.setTitle(modObj.get("name").getAsString());
        embed.setDescription(modObj.get("summary").getAsString());
        embed.addField("Downloads", modObj.get("downloadCount").getAsInt() + "", false);
        
        var builder = new StringBuilder();
        final JsonArray categories = modObj.getAsJsonArray("categories");
        for (final JsonElement element : categories) {
            final JsonObject category = element.getAsJsonObject();
            builder.append("\u2022 ").append(category.get("name").getAsString()).append("\n");
        }
        
        String categoriesStr = builder.toString();
        final String[] categoriesArr = categoriesStr.split("\n");
        if (categoriesArr.length < 2) {
            categoriesStr = categoriesStr.replace("\u2022 ", "");
        }
        
        embed.addField("Categories", categoriesStr, false);
        
        builder = new StringBuilder();
        final JsonArray authors = modObj.getAsJsonArray("authors");
        for (final JsonElement element : authors) {
            final JsonObject author = element.getAsJsonObject();
            builder.append("\u2022 ").append(author.get("name").getAsString()).append("\n");
        }
        
        String authorsStr = builder.toString();
        final String[] authorArr = authorsStr.split("\n");
        if (authorArr.length < 2) {
            authorsStr = authorsStr.replace("\u2022 ", "");
        }
        
        embed.addField("Authors", authorsStr, false);
        
        if (!modObj.get("logo").isJsonNull()) {
            embed.setThumbnail(modObj.get("logo").getAsJsonObject().get("url").getAsString());
        }
        
        embed.addField("Updated At",
            DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.parse(modObj.get("dateModified").getAsString())),
            false);
        embed.addField("Created At",
            DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.parse(modObj.get("dateReleased").getAsString())),
            false);
        
        final JsonArray latestFiles = modObj.getAsJsonArray("latestFiles");
        if (!latestFiles.isEmpty()) {
            final List<String> foundVersions = new ArrayList<>();
            builder = new StringBuilder();
            for (final JsonElement element : latestFiles) {
                final JsonObject file = element.getAsJsonObject();
                final JsonArray gameVersions = file.getAsJsonArray("gameVersions");
                for (final JsonElement elem : gameVersions) {
                    final String version = elem.getAsString();
                    if (!foundVersions.contains(version)) {
                        foundVersions.add(version);
                        builder.append("\u2022 ").append(version).append("\n");
                    }
                }
            }
            
            String versionsStr = builder.toString();
            final String[] versionsArr = versionsStr.split("\n");
            if (versionsArr.length < 2) {
                versionsStr = versionsStr.replace("\u2022 ", "");
            }
            
            embed.addField("Versions", versionsStr, false);
        }
        
        return embed;
    }
    
    private JsonObject getMod(String modsStr) {
        final var mods = Constants.GSON.fromJson(modsStr, JsonObject.class);
        final JsonArray modArray = mods.getAsJsonArray("data");
        
        final List<JsonObject> objs = new ArrayList<>();
        StreamSupport.stream(modArray.spliterator(), false).map(JsonElement::getAsJsonObject)
            .sorted(Comparator.comparingInt(obj -> -obj.get("downloadCount").getAsInt())).forEach(objs::add);
        return objs.isEmpty() ? null : objs.get(0);
    }
    
    private String listCategories() {
        try {
            final URLConnection urlc = new URL("https://api.curseforge.com/v1/categories?gameId=432").openConnection();
            urlc.addRequestProperty("x-api-key", Environment.INSTANCE.curseforgeKey());
            return IOUtils.toString(urlc.getInputStream(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
    
    private String listGames() {
        try {
            final URLConnection urlc = new URL("https://api.curseforge.com/v1/games").openConnection();
            urlc.addRequestProperty("x-api-key", Environment.INSTANCE.curseforgeKey());
            return IOUtils.toString(urlc.getInputStream(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
    
    private String listMods(String search) {
        try {
            final URLConnection urlc = new URL(
                "https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&searchFilter=" + search)
                    .openConnection();
            urlc.addRequestProperty("x-api-key", Environment.INSTANCE.curseforgeKey());
            return IOUtils.toString(urlc.getInputStream(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
    
    private void printMods(String search) {
        final String result = listMods(search);
        final Iterator<JsonElement> mods = Constants.GSON.fromJson(result, JsonObject.class).getAsJsonArray("data")
            .iterator();
        final var builder = new StringBuilder();
        Stream.generate(() -> null).takeWhile(x -> mods.hasNext()).map(n -> mods.next())
            .map(JsonElement::getAsJsonObject).map(obj -> obj.get("name").getAsString())
            .forEach(n -> builder.append(n).append("\n"));
        Constants.LOGGER.debug(builder.toString());
    }
}
