package dev.darealturtywurty.superturtybot.commands.fun;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EightBallCommand extends CoreCommand {
    public EightBallCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.STRING, "question", "The question that you want to ask the 8 Ball", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "This will fortune tell with an answer for your question.";
    }

    @Override
    public String getHowToUse() {
        return "/eightball [question]";
    }

    @Override
    public String getName() {
        return "eightball";
    }

    @Override
    public String getRichName() {
        return "8 Ball";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        final String question = event.getOption("question", "No question provided!", OptionMapping::getAsString);
        try {
            final URLConnection connection = new URI(
                "https://www.eightballapi.com/api?question=%s&biased=true".formatted(URLEncoder.encode(question, StandardCharsets.UTF_8)))
                    .toURL().openConnection();
            final JsonObject result = Constants.GSON
                .fromJson(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8), JsonObject.class);

            final var embed = new EmbedBuilder();
            embed.setColor(Color.PINK);
            embed.setTimestamp(Instant.now());
            embed.setTitle("8 Ball 🎱");
            embed.setDescription("You asked: " + question + "\n My answer: "
                + URLDecoder.decode(result.get("reading").getAsString(), StandardCharsets.UTF_8).replace("€“", ""));
            embed.setFooter(event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (final IOException | URISyntaxException exception) {
            event.getHook().sendMessage("❌ There has been an issue with the 8 Ball command!").queue();
            Constants.LOGGER.error("There has been an issue with the 8 Ball command!", exception);
        }
    }
}
