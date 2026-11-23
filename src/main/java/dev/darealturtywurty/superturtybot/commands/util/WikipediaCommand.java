package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WikipediaCommand extends CoreCommand {
    private static final HttpUrl API_URL = HttpUrl.get("https://en.wikipedia.org/w/api.php");
    private static final String USER_AGENT = "TurtyBot/1.0 (https://turtywurty.dev/projects/turtybot; turtybot@gmail.com)";
    private static final String BUTTON_PREFIX = "wikipedia-";
    private static final String RESULT_TITLE_PREFIX = "Results for: ";
    private static final int RESULT_LIMIT = 5;
    private static final int SUMMARY_LIMIT = 350;

    public WikipediaCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "page-name", "The wikipedia page name.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the Wikipedia page for the given page name.";
    }

    @Override
    public String getName() {
        return "wikipedia";
    }

    @Override
    public String getRichName() {
        return "Wikipedia";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String pageName = event.getOption("page-name", OptionMapping::getAsString);
        if (pageName == null || pageName.isBlank()) {
            reply(event, "❌ You must supply a page to view", false, true);
            return;
        }

        event.deferReply().queue(hook -> Thread.startVirtualThread(() -> showResults(event, hook, pageName.strip())));
    }

    private static void showResults(SlashCommandInteractionEvent event, InteractionHook hook, String pageName) {
        List<Article> articles;
        try {
            articles = search(pageName);
        } catch (IOException | JsonParseException exception) {
            Constants.LOGGER.warn("Wikipedia lookup failed ({})", exception.getClass().getSimpleName());
            hook.editOriginal("❌ Wikipedia is temporarily unavailable. Please try again later.").queue();
            return;
        }

        var embed = new EmbedBuilder();
        embed.setTitle(StringUtils.truncateString(RESULT_TITLE_PREFIX + pageName, MessageEmbed.TITLE_MAX_LENGTH));
        embed.setTimestamp(Instant.now());
        embed.setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());
        if (articles.isEmpty()) {
            embed.setDescription("❌ No matching Wikipedia articles found.");
            hook.editOriginalEmbeds(embed.build()).queue();
            return;
        }

        List<ActionRowChildComponent> buttons = new ArrayList<>();
        for (int index = 0; index < articles.size(); index++) {
            Article article = articles.get(index);
            String label = String.valueOf(index + 1);
            embed.addField(StringUtils.truncateString(label + ". " + article.title(), MessageEmbed.TITLE_MAX_LENGTH),
                article.fullUrl(), false);
            buttons.add(Button.primary(BUTTON_PREFIX + index, label));
        }

        hook.editOriginalEmbeds(embed.build()).queue(message -> {
            TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(interaction -> interaction.getMessageIdLong() == message.getIdLong()
                    && interaction.getUser().getIdLong() == event.getUser().getIdLong()
                    && interaction.getComponentId().startsWith(BUTTON_PREFIX))
                .timeout(5, TimeUnit.MINUTES)
                .timeoutAction(() -> message.editMessageComponents().queue())
                .success(interaction -> {
                    int index = Integer.parseInt(interaction.getComponentId().substring(BUTTON_PREFIX.length()));
                    showArticle(event, interaction, articles.get(index));
                })
                .build();
            message.editMessageComponents(ActionRow.of(buttons)).queue();
        });
    }

    private static void showArticle(
        SlashCommandInteractionEvent event,
        ButtonInteractionEvent interaction,
        Article article
    ) {
        var embed = new EmbedBuilder();
        embed.setTitle(StringUtils.truncateString(article.title(), MessageEmbed.TITLE_MAX_LENGTH), article.fullUrl());
        embed.setTimestamp(Instant.now());
        embed.setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());
        if (article.extract() != null && !article.extract().isBlank()) {
            embed.setDescription(StringUtils.truncateString(article.extract(), SUMMARY_LIMIT));
        }

        if (article.thumbnail() != null) {
            embed.setImage(article.thumbnail().source());
        }

        interaction.editMessageEmbeds(embed.build()).setComponents().queue();
    }

    private static List<Article> search(String pageName) throws IOException {
        HttpUrl url = API_URL.newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("format", "json")
            .addQueryParameter("formatversion", "2")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", pageName)
            .addQueryParameter("gsrnamespace", "0")
            .addQueryParameter("gsrlimit", String.valueOf(RESULT_LIMIT))
            .addQueryParameter("prop", "info|extracts|pageimages")
            .addQueryParameter("inprop", "url")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("exchars", String.valueOf(SUMMARY_LIMIT))
            .addQueryParameter("piprop", "thumbnail")
            .addQueryParameter("pithumbsize", "500")
            .addQueryParameter("pilimit", String.valueOf(RESULT_LIMIT))
            .build();

        var request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
        var call = Constants.HTTP_CLIENT.newCall(request);
        call.timeout().timeout(10, TimeUnit.SECONDS);
        try (var response = call.execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                Constants.LOGGER.warn("Wikipedia returned HTTP {}", response.code());
                throw new IOException("Unsuccessful Wikipedia response");
            }

            return parseSearchResponse(body.string());
        }
    }

    private static List<Article> parseSearchResponse(String body) throws IOException {
        SearchResponse result = Constants.GSON.fromJson(body, SearchResponse.class);
        if (result == null || result.error() != null)
            throw new IOException("Invalid Wikipedia response");

        if (result.query() == null && result.batchComplete())
            return List.of();

        if (result.query() == null || result.query().pages() == null)
            throw new IOException("Missing Wikipedia search result");

        List<Article> pages = result.query().pages();
        for (Article article : pages) {
            if (article == null || article.index() < 1 || article.title() == null || article.title().isBlank()
                || !isHttpsUrl(article.fullUrl(), MessageEmbed.VALUE_MAX_LENGTH)
                || (article.thumbnail() != null
                    && !isHttpsUrl(article.thumbnail().source(), MessageEmbed.URL_MAX_LENGTH)))
                throw new IOException("Invalid Wikipedia article");
        }

        return pages.stream().sorted(Comparator.comparingInt(Article::index)).limit(RESULT_LIMIT).toList();
    }

    private static boolean isHttpsUrl(String value, int maxLength) {
        HttpUrl url = value == null || value.length() > maxLength ? null : HttpUrl.parse(value);
        return url != null && url.isHttps();
    }

    private record SearchResponse(
        @SerializedName("batchcomplete") boolean batchComplete,
        SearchQuery query,
        JsonElement error
    ) {
    }

    private record SearchQuery(List<Article> pages) {
    }

    private record Article(
        int index,
        String title,
        @SerializedName("fullurl") String fullUrl,
        String extract,
        Thumbnail thumbnail
    ) {
    }

    private record Thumbnail(String source) {
    }
}
