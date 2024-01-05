package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CatSaysCommand extends CoreCommand {
    public CatSaysCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "text", "The text to make the cat say", true)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }

    @Override
    public String getDescription() {
        return "Makes a cat say something!";
    }

    @Override
    public String getName() {
        return "catsays";
    }

    @Override
    public String getRichName() {
        return "Cat Says";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", "how tf did you manage this?", OptionMapping::getAsString);
        String url = "https://cataas.com/cat/says/" + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");

        event.deferReply().queue();
        try {
            InputStream stream = new URI(url).toURL().openStream();
            var upload = FileUpload.fromData(stream, "cat.png");
            event.getHook().sendFiles(upload).queue(ignored -> { try { stream.close(); } catch (IOException ignored2) {}});
        } catch (IOException | URISyntaxException exception) {
            event.getHook().editOriginal("❌ Something went wrong!").queue();
        }
    }
}
