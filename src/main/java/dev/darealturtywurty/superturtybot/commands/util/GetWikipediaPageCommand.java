package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.fastily.jwiki.core.Wiki;
import io.github.fastily.jwiki.dwrap.ImageInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class GetWikipediaPageCommand extends CoreCommand {
    public GetWikipediaPageCommand() {
        super(new Types(true,false,false,false));
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
        return "Returns the wikipedia page";
    }

    @Override
    public String getName() {
        return "wikipedia-page";
    }

    @Override
    public String getRichName() {
        return "Wikipedia page";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 10L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String pageName = event.getOption("page-name", null, OptionMapping::getAsString).trim();

        if(pageName == null){
            reply(event, "❌ You must provide a valid wiki name!", false, true);
            return;
        }

        Wiki wiki = new Wiki.Builder().build();

        if(wiki == null){
            reply(event, "❌ Failed to get response!", false, true);
            return;
        }

        List<String> imageNames = wiki.getImagesOnPage(pageName);
        String url = null;
        if(!imageNames.isEmpty()) {
            List<ImageInfo> imageInfos = wiki.getImageInfo(imageNames.get(0));
            url = imageInfos.isEmpty() ? null : imageInfos.get(0).url.toString();
        }

        var embed = new EmbedBuilder()
                .setTitle("Wiki Name: %s".formatted(pageName))
                .setDescription(wiki.getTextExtract(pageName).substring(0,300) + " ...")
                .setColor(0xACABAD)
                .setImage(url)
                .setFooter("Author name: " + wiki.getPageCreator(pageName))
                .build();
        event.deferReply().addEmbeds(embed).queue();
    }
}
