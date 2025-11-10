package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
    public String getHowToUse() {
        return "/github [repositoryName]";
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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String rawOption = event.getOption("repository", OptionMapping::getAsString);

        if (rawOption == null) {
            reply(event, "❌ You must provide a repository name!", false, true);
            return;
        }

        event.deferReply().queue();

        // https://github.com/owner/repository.git?...
        if(rawOption.matches("https://github\\.com/[^/]+/[^/]+")) {
            String[] split = rawOption.split("/");
            rawOption = split[split.length - 1];

            // remove ?... from the end
            if(rawOption.contains("?")) {
                rawOption = rawOption.substring(0, rawOption.indexOf("?"));
            }

            // remove .git from the end
            if(rawOption.endsWith(".git")) {
                rawOption = rawOption.substring(0, rawOption.indexOf(".git"));
            }

            String owner = split[split.length - 2];
            String name = split[split.length - 1];

            @NotNull Repository repo;
            try {
                repo = findRepo(owner, name);
            } catch (final IOException | URISyntaxException exception) {
                event.getHook().editOriginal("❌ I could not find any repositories matching the name: `" + rawOption + "`!")
                        .mentionRepliedUser(false).queue();
                return;
            }

            final EmbedBuilder embed = createEmbed(repo);
            event.getHook().editOriginalEmbeds(embed.build()).queue();
            return;
        }

        final String repositoryName = URLEncoder.encode(rawOption, StandardCharsets.UTF_8);

        List<Repository> repositories;
        try {
            repositories = searchGithubRepo(repositoryName);
        } catch (final IOException | URISyntaxException exception) {
            event.getHook().editOriginal("❌ I could not find any repositories matching the name: `" + repositoryName + "`!")
                .mentionRepliedUser(false).queue();
            Constants.LOGGER.error("Failed to search for repositories!", exception);
            return;
        }

        if (repositories.isEmpty()) {
            event.getHook().editOriginal("❌ I could not find any repositories matching the name: `" + repositoryName + "`!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        if (repositories.size() == 1) {
            final EmbedBuilder embed = createEmbed(repositories.getFirst());
            event.getHook().editOriginalEmbeds(embed.build()).mentionRepliedUser(false).queue();
            return;
        }

        var contentsBuilder = new PaginatedEmbed.ContentsBuilder();
        for (final Repository repo : repositories) {
            String description = repo.description().substring(0, Math.min(repo.description().length(), MessageEmbed.VALUE_MAX_LENGTH));
            contentsBuilder.field(repo.name() + " - " + repo.url(), description, false);
        }

        PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contentsBuilder)
                .title("GitHub Repositories")
                .description("Here are the repositories I found matching the search term: `" + repositoryName + "`")
                .color(Color.BLUE)
                .timestamp(Instant.now())
                .footer("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                .authorOnly(event.getUser().getIdLong())
                .build(event.getJDA());

        embed.send(event.getHook(),
                () -> event.getHook().editOriginal("I found " + repositories.size() + " repositories matching the search term: `" + repositoryName + "`!")
                        .mentionRepliedUser(false)
                        .queue()
        );

        embed.setOnMessageUpdate(message -> {
            List<MessageTopLevelComponent> components = new ArrayList<>(message.getComponents());

            // get a list of the current page's fields
            List<Repository> currentRepos = repositories.subList(embed.getPage() * 10, Math.min(repositories.size(), (embed.getPage() + 1) * 10));

            //noinspection DataFlowIssue
            var menu = StringSelectMenu.create("github-%d-%d-%d-%d".formatted(
                            event.isFromGuild() ? event.getGuild().getIdLong() : 0,
                            event.getChannel().getIdLong(),
                            message.getIdLong(),
                            event.getUser().getIdLong()))
                    .setPlaceholder("Select a Repository")
                    .addOptions(currentRepos.stream().map(repo -> SelectOption.of(repo.name(), repo.authorName() + "::" + repo.name())).toList())
                    .setRequiredRange(1, 1)
                    .build();

            components.add(ActionRow.of(menu));
            message.editMessageComponents(components).queue();
        });
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        String[] split = id.split("-");

        String type = split[0];
        if (!type.equals("github-")) return;

        long guildId = Long.parseLong(split[1]);
        long channelId = Long.parseLong(split[2]);
        long messageId = Long.parseLong(split[3]);
        long userId = Long.parseLong(split[4]);

        Guild guild = event.getGuild();
        if (guildId == 0 && guild != null) return;
        else if (guildId != 0 && guild != null && guild.getIdLong() != guildId) return;
        else if(event.getChannel().getIdLong() != channelId) return;
        else if(event.getMessageIdLong() != messageId) return;
        else if(event.getUser().getIdLong() != userId) {
            event.deferEdit().queue();
            return;
        }

        String value = event.getSelectedOptions().getFirst().getValue();
        String[] repoInfo = value.split("::");
        String author = repoInfo[0];
        String repoName = repoInfo[1];

        Repository repo;
        try {
            repo = findRepo(author, repoName);
        } catch (final IOException | URISyntaxException exception) {
            event.deferEdit().queue();
            return;
        }

        EmbedBuilder embed = createEmbed(repo);
        event.editComponents().setEmbeds(embed.build()).queue();
    }

    private static EmbedBuilder createEmbed(Repository repo) {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(languageToColor(repo.language));
        embed.setTitle("GitHub Repository: " + repo.name);
        embed.setDescription(repo.description);
        embed.addField("Owned By:", repo.authorName, false);
        embed.addField("Link:", repo.url, false);

        embed.addField("Language:", repo.language, true);
        embed.addField("Default Branch:", repo.defaultBranch, true);

        String license = "%s (%s)".formatted(repo.license.name, repo.license.key.toUpperCase(Locale.ROOT));
        embed.addField("License:", license, true);

        embed.addField("Size:", repo.estimateSize + "MB", true);
        embed.addField("Stars:", String.valueOf(repo.stars), true);
        embed.addField("Forks:", String.valueOf(repo.forks), true);

        embed.addField("Watches:", String.valueOf(repo.watchers), true);
        embed.addField("Subscribers:", String.valueOf(repo.subscribers), true);
        embed.addField("Issues:", String.valueOf(repo.openIssueCount), true);

        embed.addField("Archived:", StringUtils.trueFalseToYesNo(repo.archived), true);
        embed.addField("Disabled:", StringUtils.trueFalseToYesNo(repo.disabled), true);
        embed.addField("Fork:", StringUtils.trueFalseToYesNo(repo.isFork), true);

        long creationTime = parseDateTime(repo.creationDate).getTime();
        long lastUpdateTime = parseDateTime(repo.lastUpdated).getTime();
        embed.addField("Created At:", TimeFormat.DATE_TIME_SHORT.format(creationTime), true);
        embed.addField("Last Updated At:", TimeFormat.DATE_TIME_SHORT.format(lastUpdateTime), true);

        return embed;
    }

    private static Repository getDetails(final JsonObject from) {
        Constants.LOGGER.info("Parsing JSON object: {}", from);
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
        final int subscribers = from.has("subscribers_count") ? from.get("subscribers_count").getAsInt() : 0;
        final int openIssueCount = from.get("open_issues_count").getAsInt();
        final int estimateSize = from.get("size").getAsInt();
        final boolean isFork = from.get("fork").getAsBoolean();
        final boolean isArchived = from.get("archived").getAsBoolean();
        final boolean isDisabled = from.get("disabled").getAsBoolean();
        return new Repository(name, authorName, url, description, language, defaultBranch, createdAt, updatedAt,
            license, stars, forks, watchers, subscribers, openIssueCount, estimateSize, isFork, isArchived, isDisabled);
    }

    private static Color languageToColor(final String language) {
        try {
            final URLConnection urlc = new URI("https://raw.githubusercontent.com/ozh/github-colors/master/colors.json").toURL()
                .openConnection();
            urlc.addRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(urlc.getInputStream())));
            final JsonObject master = Constants.GSON.fromJson(result, JsonObject.class);

            return master.has(language)
                ? Color.decode(master.get(language).getAsJsonObject().get("color").getAsString())
                : Color.WHITE;
        } catch (final IOException | URISyntaxException exception) {
            return Color.WHITE;
        }
    }

    @SuppressWarnings({"deprecation", "MagicConstant"})
    private static Date parseDateTime(final String dateTime) {
        final int year = Integer.parseInt(dateTime.split("-")[0]);
        final int month = Integer.parseInt(dateTime.split("-")[1]);
        final int day = Integer.parseInt(dateTime.split("-")[2].split("T")[0]);
        final int hour = Integer.parseInt(dateTime.split("T")[1].split(":")[0]);
        final int minute = Integer.parseInt(dateTime.split("T")[1].split(":")[1]);
        final int second = Integer.parseInt(dateTime.split("T")[1].split(":")[2].split("Z")[0]);
        return new Date(year - 1900, month, day, hour, minute, second);
    }

    @NotNull
    private static List<Repository> searchGithubRepo(final String repo) throws IOException, URISyntaxException {
        final var url = new URI("https://api.github.com/search/repositories?q=%s".formatted(repo)).toURL();
        final URLConnection connection = url.openConnection();
        connection.addRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(connection.getInputStream())));
        final JsonArray items = Constants.GSON.fromJson(result, JsonObject.class).get("items").getAsJsonArray();
        final List<Repository> repositories = new ArrayList<>();
        for (final JsonElement item : items) {
            repositories.add(getDetails(item.getAsJsonObject()));
        }

        return repositories;
    }

    private static Repository findRepo(String owner, String name) throws IOException, URISyntaxException {
        final var url = new URI("https://api.github.com/repos/%s/%s".formatted(owner, name)).toURL();
        final URLConnection connection = url.openConnection();
        connection.addRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(connection.getInputStream())));
        return getDetails(Constants.GSON.fromJson(result, JsonObject.class));
    }

    private record Repository(String name, String authorName, String url, String description, String language,
        String defaultBranch, String creationDate, String lastUpdated, License license, int stars, int forks,
        int watchers, int subscribers, int openIssueCount, int estimateSize, boolean isFork, boolean archived, boolean disabled) {
    }

    private record License(String key, String name) {
    }
}
