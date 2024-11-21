package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.fastily.jwiki.core.NS;
import io.github.fastily.jwiki.core.Wiki;
import io.github.fastily.jwiki.dwrap.ImageInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WikipediaCommand extends CoreCommand {
    private static Wiki WIKI;

    static {
        new Thread(() -> WIKI = new Wiki.Builder().build()).start();
    }

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
        String pageName = event.getOption("page-name", null, OptionMapping::getAsString);
        if (pageName == null) {
            reply(event, "❌ You must supply a page to view", false, true);
            return;
        }

        event.deferReply().queue();

        var embed = new EmbedBuilder();
        embed.setTitle("Results for: " + pageName);
        embed.setTimestamp(Instant.now());
        embed.setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

        List<String> pages = WIKI.search(pageName, 5, NS.MAIN)
                .stream()
                .filter(page -> page != null && !page.isBlank())
                .toList();

        if (pages.isEmpty()) {
            embed.setDescription("❌ No results found for: " + pageName);
            event.getHook().editOriginalEmbeds(embed.build()).queue();
            return;
        }

        for (int index = 0; index < pages.size(); index++) {
            String page = pages.get(index);
            String url = "https://en.wikipedia.org/wiki/" + encode(page);
            String title = decode(page);

            embed.addField((index + 1) + ". " + title, url, false);
        }

        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue(message -> {
            List<ItemComponent> components = new ArrayList<>();
            for (int index = 0; index < pages.size(); index++) {
                components.add(Button.primary(
                        "wikipedia-" + index, String.valueOf(index + 1)));
            }

            message.editMessageComponents(ActionRow.of(components)).queue(ignored -> TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                    .condition(interaction ->
                            !interaction.getComponentId().startsWith("wikipedia-") ||
                                    interaction.isFromGuild() != event.isFromGuild() ||
                                    interaction.getChannel().getIdLong() != event.getChannel().getIdLong() ||
                                    interaction.getMessageIdLong() != message.getIdLong() ||
                                    interaction.getGuild() == null ||
                                    event.getGuild() == null ||
                                    interaction.getGuild().getIdLong() == event.getGuild().getIdLong())
                    .timeout(5, TimeUnit.MINUTES)
                    .success(buttonInteractionEvent -> {
                        buttonInteractionEvent.deferEdit().queue();
                        if (buttonInteractionEvent.getUser().getIdLong() != event.getUser().getIdLong())
                            return;

                        int index = Integer.parseInt(buttonInteractionEvent.getComponentId().split("-")[1]);
                        String page = pages.get(index);
                        String url = "https://en.wikipedia.org/wiki/" + encode(page);
                        String title = decode(page);

                        embed.clearFields();
                        embed.setTitle(title, url);
                        embed.setTimestamp(Instant.now());
                        embed.setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

                        String summary = WIKI.getTextExtract(page);
                        if(summary.length() > 350) {
                            summary = summary.substring(0, 350) + "...";
                        }

                        embed.setDescription(summary);
                        embed.setImage(getImageUrl(page));

                        buttonInteractionEvent.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
                    }).build());
        });
    }

    public static String decode(String page) {
        return page.replace("_", " ")
                .replace("%27", "'")
                .replace("%26", "&")
                .replace("%2C", ",")
                .replace("%3A", ":")
                .replace("%3F", "?")
                .replace("%21", "!")
                .replace("%22", "\"")
                .replace("%23", "#")
                .replace("%24", "$")
                .replace("%25", "%")
                .replace("%28", "(")
                .replace("%29", ")");
    }

    public static String encode(String page) {
        return page.replace(" ", "_")
                .replace("'", "%27")
                .replace("&", "%26")
                .replace(",", "%2C")
                .replace(":", "%3A")
                .replace("?", "%3F")
                .replace("!", "%21")
                .replace("\"", "%22")
                .replace("#", "%23")
                .replace("$", "%24")
                .replace("%", "%25")
                .replace("(", "%28")
                .replace(")", "%29");
    }

    public static String getImageUrl(String pageName) {
        List<String> imageNames = new ArrayList<>(
                WIKI.getImagesOnPage(pageName)
                        .stream()
                        .filter(image -> image.endsWith(".png") || image.endsWith(".jpg"))
                        .toList());
        Collections.shuffle(imageNames);

        String url = null;
        while(url == null && !imageNames.isEmpty()) {
            List<ImageInfo> imageInfos = WIKI.getImageInfo(imageNames.getFirst());
            url = imageInfos.isEmpty() ? null : imageInfos.getFirst().url.toString();

            if(url == null) {
                imageNames.removeFirst();
            }
        }

        return url;
    }
}
