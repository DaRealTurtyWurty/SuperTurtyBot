package io.github.darealturtywurty.superturtybot.commands.nsfw;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Rule34Command extends NSFWCommand {
    public Rule34Command() {
        super(NSFWCategory.MISC);
    }
    
    @Override
    public String getDescription() {
        return "Searches rule34 for the inputted search phrase";
    }
    
    @Override
    public String getName() {
        return "rule34";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        // TODO: Check user config to see if they have denied NSFW usage
        // TODO: Check server config to see if the server has disabled this command
        if (event.isFromGuild() && !event.getTextChannel().isNSFW())
            return;
        
        // Essential
        super.runNormalMessage(event);
        
        final String content = event.getMessage().getContentRaw();
        final String[] args = content.split(" ");
        if (args.length < 2)
            return;
        final String input = content.replace(args[0], "");
        try {
            final String encodedInput = URLEncoder.encode(input.trim().replace(" ", "_"), StandardCharsets.UTF_8);
            final Document document = Jsoup
                .connect("https://rule34.xxx/index.php?page=post&s=list&tags=" + encodedInput).get();
            final Element pagination = document.getElementsByClass("pagination").first();
            
            int pages;
            if (pagination == null) {
                pages = 1;
            } else {
                final List<Element> pageButtons = pagination.childNodes().stream().filter(node -> node.hasAttr("href"))
                    .map(Element.class::cast).toList();
                pages = pageButtons.size() - 1;
                if (pages < 1) {
                    pages = 1;
                }
                
                for (final Element pageBtn : pageButtons) {
                    if ("last page".equalsIgnoreCase(pageBtn.attr("alt"))) {
                        pages = Integer.parseInt(pageBtn.attr("href").split("&pid=")[1]) / 42;
                        break;
                    }
                }
            }
            
            final int randomPage = ThreadLocalRandom.current().nextInt(pages);
            final String newURL = "https://rule34.xxx/index.php?page=post&s=list&tags=" + encodedInput + "&pid="
                + randomPage * 42;
            final Document newPage = Jsoup.connect(newURL).get();
            
            final Element imageList = newPage.getElementsByClass("image-list").first();
            final List<String> images = imageList.getElementsByClass("thumb").stream()
                .map(element -> element.select("a")).map(element -> element.attr("href")).collect(Collectors.toList());
            Collections.shuffle(images);
            final Document imagePage = Jsoup.connect("https://rule34.xxx/" + images.get(0)).get();
            final Element image = imagePage.selectFirst("img#image");
            final String imageURL = image.attr("src");
            event.getChannel().sendMessage(imageURL).queue();
        } catch (final IOException exception) {
            event.getChannel().sendMessage("There was an issue accessing the rule34 database! Please try again later.")
                .queue();
        } catch (final NullPointerException exception) {
            event.getChannel().sendMessage("I have not found any rule34 for '" + input.trim()
                + "'! This could however be a bug, feel free to try again.").queue();
        }
    }
}
