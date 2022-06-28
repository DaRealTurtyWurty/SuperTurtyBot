package io.github.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class GithubRepositoryCommand extends CoreCommand {
    public GithubRepositoryCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "repository", "The name of the repository", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Get stats about a GitHub Repository.";
    }

    @Override
    public String getName() {
        return "github";
    }

    @Override
    public String getRichName() {
        return "Github Repository";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String repositoryName = URLEncoder.encode(event.getOption("repository").getAsString(),
            StandardCharsets.UTF_8);
        
        Repository repo;
        try {
            repo = searchGithubRepo(repositoryName);
        } catch (final IOException exception) {
            event.deferReply(true)
                .setContent("I could not find any repositories matching the name: `" + repositoryName + "`!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        if (repo != null) {
            final EmbedBuilder embed = createEmbed(repo);
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
        } else {
            event.deferReply(true)
                .setContent("I could not find any repositories matching the name: `" + repositoryName + "`!")
                .mentionRepliedUser(false).queue();
        }
    }
    
    private static EmbedBuilder createEmbed(Repository repo) {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(languageToColor(repo.language));
        embed.setTitle("GitHub Repository: " + repo.name);
        embed.setDescription(repo.description);
        embed.addField("Owned By:", repo.authorName, false);
        embed.addField("Link:", repo.url, false);
        embed.addField("Main Language:", repo.language, false);
        embed.addField("Default Branch:", repo.defaultBranch, false);
        embed.addField("Estimate File Size:", repo.estimateSize + "MB", false);
        embed.addField("⭐:", repo.stars + "", false);
        embed.addField("🍴", repo.forks + "", false);
        embed.addField("👁", repo.watchers + "", false);
        embed.addField("‼", repo.openIssueCount + "", false);
        embed.addField("Is Archived:", StringUtils.trueFalseToYesNo(repo.archived), false);
        embed.addField("Is Disabled:", StringUtils.trueFalseToYesNo(repo.disabled), false);
        embed.addField("Is Fork:", StringUtils.trueFalseToYesNo(repo.isFork), false);
        embed.addField("Created At:", DateFormat.getDateTimeInstance().format(parseDateTime(repo.creationDate)), false);
        embed.addField("Last Updated:", DateFormat.getDateTimeInstance().format(parseDateTime(repo.lastUpdated)),
            false);
        embed.addField("License:", "Name: " + repo.license.name + " (" + repo.license.key + ")", false);
        return embed;
    }

    private static Repository getDetails(final JsonObject from) {
        final String name = from.get("name").getAsString();
        final String authorName = from.get("owner").getAsJsonObject().get("login").getAsString();
        final String url = from.get("html_url").getAsString();
        
        String description = "No description provided.";
        if (!(from.get("description") instanceof JsonNull)) {
            description = from.get("description").getAsString();
        }
        
        String language = "No language";
        if (!(from.get("language") instanceof JsonNull)) {
            language = from.get("language").getAsString();
        }
        
        final String defaultBranch = from.get("default_branch").getAsString();
        final String createdAt = from.get("created_at").getAsString();
        final String updatedAt = from.get("updated_at").getAsString();
        
        License license;
        if (!(from.get("license") instanceof JsonNull)) {
            final JsonObject licenseObj = from.get("license").getAsJsonObject();
            license = new License(licenseObj.get("key").getAsString(), licenseObj.get("name").getAsString());
        } else {
            license = new License("arr", "All Rights Reserved");
        }
        
        final int stars = from.get("stargazers_count").getAsInt();
        final int forks = from.get("forks_count").getAsInt();
        final int watchers = from.get("watchers_count").getAsInt();
        final int openIssueCount = from.get("open_issues_count").getAsInt();
        final int estimateSize = from.get("size").getAsInt();
        final boolean isFork = from.get("fork").getAsBoolean();
        final boolean isArchived = from.get("archived").getAsBoolean();
        final boolean isDisabled = from.get("disabled").getAsBoolean();
        return new Repository(name, authorName, url, description, language, defaultBranch, createdAt, updatedAt,
            license, stars, forks, watchers, openIssueCount, estimateSize, isFork, isArchived, isDisabled);
    }
    
    private static Color languageToColor(final String language) {
        try {
            final URLConnection urlc = new URL("https://raw.githubusercontent.com/ozh/github-colors/master/colors.json")
                .openConnection();
            urlc.addRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(urlc.getInputStream())));
            final JsonObject master = Constants.GSON.fromJson(result, JsonObject.class);
            
            return master.has(language)
                ? Color.decode(master.get(language).getAsJsonObject().get("color").getAsString())
                : Color.WHITE;
        } catch (final IOException exception) {
            return Color.WHITE;
        }
    }
    
    @SuppressWarnings("deprecation")
    private static Date parseDateTime(final String dateTime) {
        final int year = Integer.parseInt(dateTime.split("-")[0]);
        final int month = Integer.parseInt(dateTime.split("-")[1]);
        final int day = Integer.parseInt(dateTime.split("-")[2].split("T")[0]);
        final int hour = Integer.parseInt(dateTime.split("T")[1].split(":")[0]);
        final int minute = Integer.parseInt(dateTime.split("T")[1].split(":")[1]);
        final int second = Integer.parseInt(dateTime.split("T")[1].split(":")[2].split("Z")[0]);
        return new Date(year - 1900, month, day, hour, minute, second);
    }
    
    private static Repository searchGithubRepo(final String repo) throws IOException {
        final var url = new URL("https://api.github.com/search/repositories?q=" + repo + "&sort=stars&order=desc");
        final URLConnection urlc = url.openConnection();
        urlc.addRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(urlc.getInputStream())));
        final JsonArray items = Constants.GSON.fromJson(result, JsonObject.class).get("items").getAsJsonArray();
        if (items.size() >= 1) {
            final JsonObject first = items.get(0).getAsJsonObject();
            return getDetails(first);
        }
        return null;
    }
    
    private static record Repository(String name, String authorName, String url, String description, String language,
        String defaultBranch, String creationDate, String lastUpdated, License license, int stars, int forks,
        int watchers, int openIssueCount, int estimateSize, boolean isFork, boolean archived, boolean disabled) {
    }
    
    private static record License(String key, String name) {
    }
}
