package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InspiroBotCommand extends AbstractImageCommand {
    public InspiroBotCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public String getDescription() {
        return "Gets an artificially generated inspirational quote";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.FUN;
    }
    
    @Override
    public String getName() {
        return "inspirobot";
    }

    @Override
    public String getRichName() {
        return "InspiroBot";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final CompletableFuture<String> quote = getInspiroQuote();
        event.deferReply().setContent("Loading InspiroBot quote...").mentionRepliedUser(false).queue();
        quote.thenAccept(result -> event.getHook().editOriginal(result).queue());
    }
    
    private static CompletableFuture<String> getInspiroQuote() {
        final var future = new CompletableFuture<String>();
        try {
            final URLConnection connection = new URI("https://inspirobot.me/api/?generate=true").toURL().openConnection();
            final String body = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            future.complete(body);
        } catch (final IOException | URISyntaxException exception) {
            future.complete("There has been an issue processing this command. Please try again later!");
            Constants.LOGGER.error("Error getting InspiroBot quote!", exception);
        }
        
        return future;
    }
}
